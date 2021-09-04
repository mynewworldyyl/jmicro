package cn.jmicro.api.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Utils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UploadFileManager {

	//当前资源存放目录
	private File dir = null;
	
	private File tempDir = null;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	//分块上传文件中每块大小
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 32767;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	private Map<Integer,DataBlockJRso> fileInfos = new ConcurrentHashMap<>();
	
	@FunctionalInterface
	public interface FinishCallback {
		RespJRso<DataBlockJRso> doFinish(DataBlockJRso db);
	}
	
	/**
	 * 实现文件上传功能：
	 * 0~3字节为ID
	 * 
	 * 第一个请求文件上传 data格式如下：
	 * 		ID=0，表示文件上传初始化，4~12字节为文件长度，long型，后面跟文件名
	 * 		DataBlockJRso返回文件块大小，总块数，文件ID等
	 * 
	 * 之后全部请求 ID！=0，
	 * 		4~8字节为数据块编号，后面紧跟数据
	 *      返回code=0,data=null
	 *      
	 * 最后一个请求时，返回code=0,data=DataBlockJRso实例，filePath即为上传文件全路径，业务代码取得文件内容
	 * 
	 * @param data
	 * @return
	 */
	public RespJRso<DataBlockJRso> addResourceData(byte[] data, FinishCallback cb) {
		/*return new PromiseImpl<RespJRso<DataBlockJRso>>((suc,fail)->{});*/
		RespJRso<DataBlockJRso> resp = new RespJRso<>(RespJRso.CODE_FAIL);
		if(data == null || data.length < 8) {
			String msg = "Receive empty data";
			resp.setMsg(msg);
			log.error(msg);
			LG.log(MC.LOG_ERROR, this.getClass(), msg);
			return resp;
		}
		
		int pos = 0;
		int id = Message.readInt(data,pos);
		pos += 4; 
		
		if(id == DataBlockJRso.INIT_BLOCK) {
			//初始请求，返回块大小，总块数，文件ID等信息
			if(data.length < 12) {
				String msg = "Invalid init req";
				resp.setMsg(msg);
				log.error(msg);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				return resp;
			}
			
			long totalLen = Message.readLong(data, pos);
			if(totalLen <= 0) {
				String msg = "Invalid data length: " + totalLen+",id:" + id;
				resp.setMsg(msg);
				log.error(msg);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				return resp;
			}
			pos += 8;
			
			String extParams = JDataInput.readString(data,pos);//读取扩展参数
			pos += JDataInput.stringTakeLen(data, pos);
			
			String fn = JDataInput.readString(data, pos);//文件名最后，不需要再移动pos
			
			if(Utils.isEmpty(fn)) {
				String msg = "File name is empty";
				resp.setMsg(msg);
				log.error(msg);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				return resp;
			}
			
			DataBlockJRso db = new DataBlockJRso();
			db.setBlockSize(uploadBlockSize);
			db.setId(idGenerator.getIntId(DataBlockJRso.class));
			db.setTotalLen(totalLen);
			db.setFileName(fn);
			db.setPhase(DataBlockJRso.PHASE_UPDATE_DATA);
			db.setExtParams(extParams);
			
			int bn = (int)(totalLen / uploadBlockSize);
			if( (totalLen % uploadBlockSize) > 0 ) bn++;
			
			db.setBlockNum(bn);
			
			fileInfos.put(db.getId(), db);
			
			resp.setTotal(db.getPhase());
			resp.setData(db);
			resp.setCode(RespJRso.CODE_SUCCESS);
			return resp;
		}
		
		DataBlockJRso db = fileInfos.get(id);
		db.setLastUseTime(TimeUtils.getCurTime());
		
		if(db.getPhase() == DataBlockJRso.PHASE_PROCESS ||
				db.getPhase() == DataBlockJRso.PHASE_FINISH) {
			resp.setTotal(db.getPhase());
			return resp;
		}
		
		//正常数据块
		int blockNum = Message.readInt(data,pos);//块号
		pos += 4;
		
		int fn = 0;
		FileOutputStream bs = null;
		try {
			File tdir = new File(this.tempDir.getAbsolutePath() + "/" + id);
			if(!tdir.exists()) {
				tdir.mkdir();
			}
			
			File bp = new File(tdir,""+ blockNum);
			if(!bp.exists()) {
				bp.createNewFile();
			}
			bs = new FileOutputStream(bp);//持久化当前块内容
			bs.write(data, pos, data.length-pos);
			fn = db.addFinishBlockNum();
		}catch (IOException e1) {
			String msg = id +" " + blockNum;
			resp.setMsg(msg);
			log.error(msg,e1);
			LG.log(MC.LOG_ERROR, this.getClass(), msg);
			return resp;
		} finally {
			if(bs != null) {
				try {
					bs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(fn == db.getBlockNum()) {
			//上传完成，开始做文件合并
			mergeUploadFile(db);
			
			db.setLastUseTime(TimeUtils.getCurTime());
			db.setPhase(DataBlockJRso.PHASE_PROCESS);
			
			if(cb != null) {
				//文件业务处理
				cb.doFinish(db);
			}
			//结束
			db.setPhase(DataBlockJRso.PHASE_FINISH);
			resp.setData(db);
		}
		
		resp.setTotal(db.getPhase());
		resp.setCode(RespJRso.CODE_SUCCESS);
		return resp;
	}
	
   private void mergeUploadFile(DataBlockJRso db) {
		
		String bp = this.tempDir.getAbsolutePath() + "/" + db.getId();
		File bpDir = new File(bp);
		
		File[] blockFiles = bpDir.listFiles( (dir,fn) -> {
			return Integer.parseInt(fn) >= 0;
		});
		
		Arrays.sort(blockFiles, (f1,f2)->{
			int num1 = Integer.parseInt(f1.getName());
			int num2 = Integer.parseInt(f2.getName());
			return num1 > num2?1:num1==num2?0:-1;
		});
		
		File resFile = new File(this.tempDir.getAbsolutePath(), db.getId()+"_"+db.getFileName());
		db.setFilePath(resFile.getAbsolutePath());
		
		FileOutputStream fos = null;
		try {
			
			if(!resFile.getParentFile().exists()) {
				resFile.getParentFile().mkdirs();
			}
			
			 if(!resFile.exists()) {
				 resFile.createNewFile();
			 }
			 fos = new FileOutputStream(resFile);
			 for(File f : blockFiles) {
				 FileInputStream fis = null;
				 try {
					 fis = new FileInputStream(f);
					 byte[] d = new byte[(int)f.length()];
					 fis.read(d, 0, (int)f.length());
					 fos.write(d, 0, d.length);
				 }finally {
					 if(fis != null) {
						 fis.close();
					 }
					 f.delete();
				 }
			 }
		} catch (IOException e) {
			log.error("finishFileUpload",e);
		}finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					log.error("",e);
				}
			}
			bpDir.delete();
		}
	}
   
   private void doChecker() {
		if(fileInfos.isEmpty()) {
			return;
		}
		
		int timeout = 30*60*1000;
		
		long curTime = TimeUtils.getCurTime();
		Map<Integer,DataBlockJRso> mtemp = new HashMap<>();
		mtemp.putAll(this.fileInfos);

		for(Map.Entry<Integer,DataBlockJRso> e : mtemp.entrySet()) {
			if(e.getValue().getPhase() == DataBlockJRso.PHASE_FINISH) {
				fileInfos.remove(e.getKey());
				continue;
			}
			
			if(curTime - e.getValue().getLastUseTime() > timeout) {
				fileInfos.remove(e.getKey());
			}
		}
	}
   

	public void jready() {
		this.dir = new File(resDir);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String td = resDir + "/.utemp";
		tempDir = new File(td);
		if(!tempDir.exists()) {
			tempDir.mkdir();
		}
		
		TimerTicker.doInBaseTicker(120, "ResourceReponsitoryChecker", null, (key,att)->{
			try {
				doChecker();
			} catch (Throwable e) {
				log.error("doChecker",e);
			}
		});
		
	}
}
