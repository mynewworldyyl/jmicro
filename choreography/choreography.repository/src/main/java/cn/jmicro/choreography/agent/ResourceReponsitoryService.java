package cn.jmicro.choreography.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.api.PackageResource;

@Component
@Service(namespace="rrs",version="0.0.1",retryCnt=0,external=true)
public class ResourceReponsitoryService implements IResourceResponsitory{

	private static final Logger LOG = LoggerFactory.getLogger(ResourceReponsitoryService.class);
	
	private static final String REF_FILE_SUBFIX = "-jar-with-dependencies.jar";
	
	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	//分块上传文件中每块大小
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 65300;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/openDebug", defGlobal=false)
	private boolean openDebug = true;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/resTimeout", defGlobal=true)
	private long resTimeout = 3*60*1000;
	
	@Cfg(value="/ResourceReponsitoryService/devMode", defGlobal=true)
	private boolean devMode = false;//1024*1024;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	//private String zkDir = Config.BASE_DIR + "/resDataDir";
	
	private File dir = null;
	
	private File tempDir = null;
	
	//private Map<String,PackageResource> blockFile = new HashMap<>();
	
	private Map<String,PackageResource> blockIndexFiles = new HashMap<>();
	
	private Map<Integer,InputStream> downloadReses = new HashMap<>();
	
	private Map<Integer,Long> downloadResourceTimeout = Collections.synchronizedMap(new HashMap<>());
	
	/*
	@Inject
	private IDataOperator op;
	*/
	
	public void ready() {
		dir = new File(resDir);
		if(!dir.exists()) {
			dir.mkdir();
		}
		
		String td = resDir + "/temp";
		tempDir = new File(td);
		if(!tempDir.exists()) {
			tempDir.mkdir();
		}
		
		TimerTicker.doInBaseTicker(120, "ResourceReponsitoryChecker", null, (key,att)->{
			try {
				doChecker();
			} catch (Throwable e) {
				LOG.error("doChecker",e);
			}
		});
	}
	
	private void doChecker() {
		if(downloadResourceTimeout.isEmpty()) {
			return;
		}
		
		long curTime = TimeUtils.getCurTime();
		Map<Integer,Long> mtemp = new HashMap<>();
		mtemp.putAll(this.downloadResourceTimeout);
				
		for(Map.Entry<Integer,Long> e : mtemp.entrySet()) {
			if(curTime - e.getValue() > resTimeout) {
				InputStream is = this.downloadReses.get(e.getKey());
				if(is != null) {
					try {
						LOG.warn("Remove timeout resource: " + e.getKey()+", timeout: " + this.resTimeout);
						is.close();
						this.downloadReses.remove(e.getKey());
						this.downloadResourceTimeout.remove(e.getKey());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		
	}

	@Override
	public List<PackageResource> getResourceList(boolean onlyFinish) {
		//Resp<List<PackageResource>> resp = new Resp<>(0);
		
		List<PackageResource> l = new ArrayList<>();
		
		File[] fs = null;
		if(this.devMode) {
			Map<String,File> fileMaps = new HashMap<>();
			findFile0(fileMaps, this.dir);
			fs = new File[fileMaps.size()];
			fileMaps.values().toArray(fs);
		}else {
			fs = dir.listFiles();
		}
		
		for(File f : fs) {
			if(f.isDirectory()) {
				continue;
			}
			
			if(openDebug) {
				LOG.debug("Return resource: " + f.getName());
			}
			
			PackageResource rr = new PackageResource();
			rr.setName(f.getName());
			rr.setSize(f.length());
			rr.setBlockSize(this.uploadBlockSize);
			
			int blockNum = (int)(f.length() / this.uploadBlockSize);
			
			if(f.length() % this.uploadBlockSize > 0) {
				++blockNum;
			}
			
			rr.setTotalBlockNum(blockNum);
			rr.setFinishBlockNum(blockNum);
			
			l.add(rr);
		}
		
		if(!onlyFinish) {
			File[] indexFiles = tempDir.listFiles((dir,fn) ->{
				return new File(dir,fn).isDirectory();
			});
			
			for(File f : indexFiles) {
				String n = f.getName();
				if(!this.blockIndexFiles.containsKey(n)) {
					if(openDebug) {
						LOG.debug("Return temp resource: " + n);
					}
					PackageResource zkrr = new PackageResource();
					l.add(zkrr);
					zkrr.setName(n);
				}
			}
			
			l.addAll(this.blockIndexFiles.values());
		}
		//resp.setData(l);
		return l;
	}
	
	@Override
	public Resp<Boolean> deleteResource(String name) {
		Resp<Boolean> resp = new Resp<>(0);
		File res = new File(resDir+"/"+name);
		if(res.exists()) {
			res.delete();
		}
		
		if(blockIndexFiles.containsKey(name)) {
			blockIndexFiles.remove(name);
		}
		
		File resD = new File(this.tempDir,name);
		if(resD.exists()) {
			File[] fs = resD.listFiles();
			for(File f : fs) {
				f.delete();
			}
			resD.delete();
		}
		resp.setData(true);
		return resp;
	}

	@Override
	public Resp<Integer> addResource(String name, int totalSize) {
		Resp<Integer> resp = new Resp<>(0);
		File resFile = new File(this.dir,name);
		if(resFile.exists()) {
			String msg = "Resource exist: " + name;
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			return resp;
		}
		
		PackageResource rr = new PackageResource();
		rr.setName(name);
		rr.setSize(totalSize);
		rr.setFinishBlockNum(0);
		
		File resD = new File(this.tempDir,name);
		if(resD.exists()) {
			File[] fs = resD.listFiles();
			rr.setFinishBlockNum(fs.length);
		} else {
			resD.mkdir();
		}
	
		int bn = totalSize/this.uploadBlockSize;
		if(totalSize % this.uploadBlockSize > 0) {
			bn++;
		}
		rr.setTotalBlockNum(bn);
		
		saveIndex(rr);
		
		LOG.info("Add resource: " + rr.toString());
		resp.setData(this.uploadBlockSize);
		return resp;
	}

	@Override
	public Resp<Boolean> addResourceData(String name, byte[] data, int blockNum) {
		Resp<Boolean> resp = new Resp<>(0);
		PackageResource zkrr = this.getIndex(name);
		synchronized(zkrr) {
			if(zkrr == null) {
				String msg = "Resource is not ready to upload!";
				resp.setMsg(msg);
				resp.setCode(1);
				LOG.error(msg);
				return resp;
			}
			
			FileOutputStream bs = null;
			try {
				String bp = this.tempDir.getAbsolutePath() + "/" + name+"/" + blockNum;
				bs = new FileOutputStream(bp);
				bs.write(data, 0, data.length);
				zkrr.setFinishBlockNum(zkrr.getFinishBlockNum() +1);
			} catch (IOException e1) {
				String msg = name +" " + blockNum;
				resp.setMsg(msg);
				resp.setCode(1);
				LOG.error(msg,e1);
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
				finishFileUpload(name,zkrr);
				deleteIndex(name);
				LOG.info("Add resource success: " + zkrr.toString());
			} else {
				if(openDebug) {
					LOG.debug("Name: " +zkrr.getName() +" blockNum: " + zkrr.getFinishBlockNum());
				}
				this.saveIndex(zkrr);
			}
			
			return resp;
		}
	}
	
	@Override
	public Resp<Integer> initDownloadResource(String name) {

		Resp<Integer> resp = new Resp<>(0);
		
		Integer downloadId = this.idGenerator.getIntId(PackageResource.class);
		
		File resFile = new File(this.dir,name);
		if(this.devMode) {
			File devFile = findResFile(name);
			if(devFile != null) {
				resFile = devFile;
			}
		}
		
		if(!resFile.exists()) {
			String msg = "File [" +name + "] not found!";
			resp.setMsg(msg);
			resp.setCode(1);
			LOG.error(msg);
			return resp;
		}
		
		LOG.info("Init download resource name : " + name + ", downloadId: " +downloadId);
		
		try {
			this.downloadReses.put(downloadId, new FileInputStream(resFile));
			downloadResourceTimeout.put(downloadId, TimeUtils.getCurTime());
		} catch (FileNotFoundException e) {
			String msg = "File [" + downloadId+"] not found";
			resp.setMsg(msg);
			resp.setCode(1);
			LOG.error(msg);
			return resp;
		}
		resp.setData(downloadId);
		return resp;
	}

	private File findResFile(String name) {
		if(!(name.endsWith(REF_FILE_SUBFIX) || name.startsWith("jmicro-agent-"))) {
			LOG.warn("Resource name invalid: " + name);
			return null;
		}
		Map<String,File> rst = new HashMap<>();
		findFile0(rst,dir);
		return rst.get(name);
	}

	private void findFile0(Map<String,File> rst, File file) {
		if(file.isFile()) {
			String n = file.getName();
			if(file.getAbsolutePath().indexOf("target") > 1 && (n.endsWith(REF_FILE_SUBFIX) || n.startsWith("jmicro-agent-"))) {
				rst.put(n, file);
			}
		}else {
			File[] fs = file.listFiles((File dir, String name)->{
				if(name.equals("mng.web")) {
					return false;
				}
				
				File f = new File(dir,name);
				if(f.isDirectory()) {
					return true;
				} else {
					return name.endsWith(".jar");
				}
			});
			
			for(File f : fs) {
				//LOG.debug(f.getAbsolutePath());
				findFile0(rst,f);
			}
		}
	}

	@Override
	@SMethod(logLevel=MC.LOG_NO,retryCnt=0)
	public byte[] downResourceData(int downloadId, int specifyBlockNum) {
		InputStream is = this.downloadReses.get(downloadId);
		if(is == null) {
			LOG.error("Resource not found ID: " + downloadId);
			return null;
		}
		
		byte[] data = new byte[this.uploadBlockSize];
		try {
			if(specifyBlockNum > 0) {
				is.skip(specifyBlockNum * uploadBlockSize);
			}
			int len = is.read(data, 0, this.uploadBlockSize);
			if(len <= 0 || len < uploadBlockSize) {
				//已经到文件结尾
				this.downloadReses.remove(downloadId);
				downloadResourceTimeout.remove(downloadId);
				is.close();
			} else {
				downloadResourceTimeout.put(downloadId, TimeUtils.getCurTime());
			}
			
			if(len > 0 && len < uploadBlockSize) {
				//最后一块
				byte[] destData = new byte[len];
				System.arraycopy(data, 0, destData, 0, len);
				data = destData;
				LOG.info("Download resource finish ID: " + downloadId);
			}
			
		} catch (IOException e) {
			LOG.error("File [" + downloadId+"] error",e);
		}
		
		return data;
	}


	private void finishFileUpload(String name, PackageResource zkrr) {
		
		String bp = this.tempDir.getAbsolutePath() + "/" + name;
		File bpDir = new File(bp);
		
		File[] blockFiles = bpDir.listFiles( (dir,fn) -> {
			return Integer.parseInt(fn) >= 0;
		});
		
		Arrays.sort(blockFiles, (f1,f2)->{
			int num1 = Integer.parseInt(f1.getName());
			int num2 = Integer.parseInt(f2.getName());
			return num1 > num2?1:num1==num2?0:-1;
		});
		
		File resFile = new File(this.dir,name);
		FileOutputStream fos = null;
		try {
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
			 
			 bpDir.delete();
		} catch (IOException e) {
			LOG.error("finishFileUpload",e);
		}finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					LOG.error("",e);
				}
			}
		}
	}
	
	
	private void saveIndex(PackageResource res) {
		
		blockIndexFiles.put(res.getName(), res);
		
		/*FileOutputStream indexOut = null;
		try {
			String bp = this.tempDir.getAbsolutePath() + "/" + res.getName() + ".index";
			JDataOutput jo = new JDataOutput();
			TypeCoderFactory.getDefaultCoder().encode(jo, res, null, null);
			indexOut = new FileOutputStream(bp);
			ByteBuffer buf = jo.getBuf();
			indexOut.write(buf.array(), 0, buf.remaining());
		}catch(IOException e) {
			LOG.error("addResource",e);
		}finally {
			if(indexOut != null) {
				try {
					indexOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}*/
		
		
	}
	
	private PackageResource getIndex(String  name) {
		return blockIndexFiles.get(name);
		/*FileInputStream indexInput = null;
		
		try {
			String bp = this.tempDir.getAbsolutePath() + "/" +name+".index";
			indexInput = new FileInputStream(bp);
			byte[] data = new byte[indexInput.available()];
			indexInput.read(data, 0, data.length);
			JDataInput in = new JDataInput(ByteBuffer.wrap(data));
			return (PackageResource)TypeCoderFactory.getDefaultCoder().decode(in, null, null);
		}catch(IOException e) {
			LOG.error("addResource",e);
		}finally {
			if(indexInput != null) {
				try {
					indexInput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;*/
	}
	
	
	private void deleteIndex(String name) {
		/*String bp = this.tempDir.getAbsolutePath() + "/" +name+".index";
		new File(bp).delete();*/
		blockIndexFiles.remove(name);
	}

}
