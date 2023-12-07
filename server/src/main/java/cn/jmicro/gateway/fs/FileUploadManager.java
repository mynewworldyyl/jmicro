package cn.jmicro.gateway.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileUploadManager {

	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	//分块上传文件中每块大小
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 32767;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/openDebug", defGlobal=false)
	private boolean openDebug = false;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/resTimeout", defGlobal=true)
	private long resTimeout = 3*60*1000;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	private Map<String,FileJRso> files = new ConcurrentHashMap<>();
	
	//当前资源存放目录
	private File dir = null;
	
	private File tempDir = null;
	
	public void jready() {
		this.dir = new File(resDir);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String td = resDir + "/.temp";
		tempDir = new File(td);
		if(!tempDir.exists()) {
			tempDir.mkdir();
		}
		
		log.info("tempDir=" + tempDir);
		
		//taskMng = new TaskManager(5000);
		TimerTicker.doInBaseTicker(120, "fileUploadMngChecker", null,
		(key,att)->{
			try {
				doChecker();
			} catch (Throwable e) {
				log.error("doChecker",e);
			}
		});
	}
	
	public RespJRso<FileJRso> addFileData(String id, byte[] data, int blockNum) {

		FileJRso zkrr = this.files.get(id);
		RespJRso<FileJRso> resp = RespJRso.d(RespJRso.CODE_FAIL,zkrr);

		if(zkrr == null) {
			String msg = "Resource is not ready to upload!";
			resp.setMsg(msg);
			resp.setCode(1);
			log.error(msg);
			LG.log(MC.LOG_ERROR, this.getClass(), msg);
			return resp;
		}
		
		zkrr.setUpdatedTime(TimeUtils.getCurTime());
		
		FileOutputStream bs = null;
		try {
			//当前上传文件缓存目录
			File tdir = new File(this.tempDir.getAbsolutePath() + "/" + id);
			if(!tdir.exists()) {
				tdir.mkdir();
			}
			
			//当前块文件
			File bp = new File(tdir,""+ blockNum);
			if(!bp.exists()) {
				bp.createNewFile();
			}
			bs = new FileOutputStream(bp);
			bs.write(data, 0, data.length);
			zkrr.setFinishBlockNum(zkrr.getFinishBlockNum() +1);
		} catch (IOException e1) {
			String msg = id +" " + blockNum;
			resp.setMsg(msg);
			resp.setCode(1);
			log.error(msg,e1);
			LG.log(MC.LOG_ERROR, this.getClass(), msg);
			
			Map<String,Object> updater = new HashMap<>();
			updater.put("status", FileJRso.S_ERROR);
			
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, zkrr.getId());
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
		
		if(zkrr.getFinishBlockNum() == zkrr.getTotalBlockNum()) {
			//上传完成
			mergeUploadFile(zkrr);
			String msg = "Add resource success: " + zkrr.toString();
			log.info(msg);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			zkrr.setStatus(FileJRso.S_FINISH);
		} else {
			if(openDebug) {
				log.debug("Name: " +zkrr.getName() +" blockNum: " + zkrr.getFinishBlockNum());
			}
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(), "Name: " +zkrr.getName() +" blockNum: " + zkrr.getFinishBlockNum());
			}
		}
		
		resp.setCode(RespJRso.CODE_SUCCESS);
		return resp;
	}
	
	public RespJRso<FileJRso> addFile(FileJRso pr) {
		RespJRso<FileJRso> resp = new RespJRso<>(RespJRso.CODE_FAIL);
		if(pr.getClientId() == Constants.NO_CLIENT_ID && !PermissionManager.isCurAdmin(Config.getClientId())) {
			String msg = "You cannot upload public resource" + pr.getName();
			log.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_ERROR, this.getClass(), msg);
			return resp;
		}
		
		if(Utils.isEmpty(pr.getType()) && pr.getName() != null) {
			int extIdx = pr.getName().lastIndexOf(".");
			if(extIdx < 0) {
				String msg = "Resource extention name cannot be null";
				log.error(msg);
				resp.setMsg(msg);
				resp.setCode(1);
				return resp;
			}
			pr.setType(pr.getName().substring(extIdx+1));
		}
		
		//pr.setUpdateTime(TimeUtils.getCurTime());
		if(Utils.isEmpty(pr.getId())) {
			pr.setId(this.idGenerator.getStringId(FileJRso.class)+"."+FileUtils.getFileExt(pr.getName()));
		}
		
		if(JMicroContext.get().isDevAccount()) {
			pr.setBlockSize(4096);//物联网终端强制为4096字节块
		}else {
			pr.setBlockSize(this.uploadBlockSize);
		}
		
		File resD = new File(this.tempDir,pr.getId());
		if(!resD.exists()) {
			resD.mkdir();
		}
		
		pr.setStatus(FileJRso.S_UPING);
		pr.setFinishBlockNum(0);
		pr.setTotalBlockNum(getBlockNum(pr.getSize(),pr.getBlockSize()));
		
		files.put(pr.getId(), pr);
		
		log.info("Add resource: " + pr.toString());
		resp.setCode(RespJRso.CODE_SUCCESS);
		resp.setData(pr);
		return resp;
	
	}
	
	private void mergeUploadFile(FileJRso zkrr) {
		
		File bpDir = new File(this.tempDir,zkrr.getId());
		
		File[] blockFiles = bpDir.listFiles((dir,fn) -> {
			return Integer.parseInt(fn) >= 0;
		});
		
		Arrays.sort(blockFiles, (f1,f2)->{
			int num1 = Integer.parseInt(f1.getName());
			int num2 = Integer.parseInt(f2.getName());
			return num1 > num2 ? 1 : num1 == num2 ? 0 : -1;
		});
		
		OutputStream fos = null;
		try {
			
			zkrr.setLocalPath(this.resDir +"/"+ zkrr.getId() + "_" + zkrr.getName());
			File of = new File(zkrr.getLocalPath());
			if(!of.exists()) {
				of.createNewFile();
			}
			
		    fos = new FileOutputStream(of);
			
			if(zkrr.isTochar()) {
				int size = 0;
				//如果是字符流，则默认为BASE64字符串转为UTF8编码字节流，
				OutputStreamWriter w = new OutputStreamWriter(fos);
				int i = 0;
				 String str = null;
				for(File f : blockFiles) {
					 FileInputStream fis = null;
					 try {
						 fis = new FileInputStream(f);
						 byte[] d = new byte[(int)f.length()];
						 fis.read(d, 0, (int)f.length());
						//BASE64字符全为ASCII码，以字节对齐，所以可以直接转为字符串
						 str = new String(d,Constants.CHARSET);
						/* if(i == 0) {
							 if(str.startsWith(Constants.BASE64_IMAGE_PREFIX)) {
								 str = str.substring(Constants.BASE64_IMAGE_PREFIX.length());
							 }
						 }*/
						 //LOG.info(str);
						  w.write(str);
						/*if(i == blockFiles.length-1) {
							 LOG.info(str);
							 if(str.endsWith(Constants.BASE64_IMAGE_SUBFIX)) {
								 str = str.substring(0,str.length()-2);
							 }
						 }*/
						 
						/* i++;
						 byte[] dd = Base64Utils.decode(str.trim());
						 size += dd.length;
						 
						 fos.write(dd);*/
					 }catch(Exception e) {
						 log.error(str,e);
					 }finally {
						 if(fis != null) {
							 fis.close();
						 }
						 f.delete();
					 }
				 }
				
				zkrr.setSize(size);
				
				//w.close();
			} else {
				for(File f : blockFiles) {
					 FileInputStream fis = null;
					 try {
						 fis = new FileInputStream(f);
						 byte[] d = new byte[(int)f.length()];
						 int len = fis.read(d, 0, (int)f.length());
						 if(len != d.length) {
							 log.error("read block file len invalid, need len="+d.length+", rlen="+len);
						 }
						 fos.write(d, 0, len);
					 }finally {
						 if(fis != null) {
							 fis.close();
						 }
						 f.delete();
					 }
				 }
			}
		} catch (IOException e) {
			log.error("finishFileUpload",e);
		} finally {
			bpDir.delete();
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					log.error("",e);
				}
			}
		}
	}
	
	public int getBlockNum(long size, int blockSize) {
		int bn = (int)(size/blockSize);
		if(size % blockSize> 0) {
			bn++;
		}
		return bn;
	}
	

	private void doChecker() {
		
		long curTime = TimeUtils.getCurTime();
		
		if(!this.files.isEmpty()) {
			Set<String> fids = new HashSet<>();
			fids.addAll(this.files.keySet());
			for(String fid: fids) {
				FileJRso f = this.files.get(fid);
				if(curTime - f.getUpdatedTime() > resTimeout) {
					log.warn("Resource update timeout: "+f.toString());
					this.files.remove(fid);
				}
			}
		}
	}
}
