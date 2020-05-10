package org.jmicro.choreography.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.choreography.api.IResourceResponsitory;
import org.jmicro.choreography.api.PackageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(namespace="rrs",version="0.0.1",timeout=0,retryCnt=0)
public class ResourceReponsitoryService implements IResourceResponsitory{

	private static final Logger LOG = LoggerFactory.getLogger(ResourceReponsitoryService.class);
	
	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	//分块上传文件中每块大小
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 65300;//1024*1024;
	
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
	}
	
	@Override
	public List<PackageResource> getResourceList(boolean onlyFinish) {
		List<PackageResource> l = new ArrayList<>();
		File[] fs = dir.listFiles();
		
		for(File f : fs) {
			if(f.isDirectory()) {
				continue;
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
					PackageResource zkrr = new PackageResource();
					l.add(zkrr);
					zkrr.setName(n);
				}
			}
			
			l.addAll(this.blockIndexFiles.values());
		}
		
		return l;
	}
	
	@Override
	public boolean deleteResource(String name) {
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
		
		return true;
	}

	@Override
	public int addResource(String name, int totalSize) {
		File resFile = new File(this.dir,name);
		if(resFile.exists()) {
			return -2;
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
		
		return this.uploadBlockSize;
	}

	@Override
	public boolean addResourceData(String name, byte[] data, int blockNum) {
		
		PackageResource zkrr = this.getIndex(name);
		synchronized(zkrr) {
			if(zkrr == null) {
				LOG.error("Resource is not ready to upload!");
				return false;
			}
			
			FileOutputStream bs = null;
			try {
				String bp = this.tempDir.getAbsolutePath() + "/" + name+"/" + blockNum;
				bs = new FileOutputStream(bp);
				bs.write(data, 0, data.length);
				zkrr.setFinishBlockNum(zkrr.getFinishBlockNum() +1);
			} catch (IOException e1) {
				LOG.error(name +" " + blockNum,e1);
				return false;
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
			} else {
				this.saveIndex(zkrr);
			}
			
			return true;
		}
	}
	
	

	@Override
	public int initDownloadResource(String name) {

		Integer downloadId = this.idGenerator.getIntId(PackageResource.class);
		
		File resFile = new File(this.dir,name);
		if(!resFile.exists()) {
			return -1;
		}
		
		try {
			this.downloadReses.put(downloadId, new FileInputStream(resFile));
		} catch (FileNotFoundException e) {
			LOG.error("File [" + downloadId+"] not found");
			return -1;
		}
		
		return downloadId;
	}

	@Override
	public byte[] downResourceData(int downloadId, int specifyBlockNum) {
		InputStream is = this.downloadReses.get(downloadId);
		if(is == null) {
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
				is.close();
			}
			
			if(len > 0 && len < uploadBlockSize) {
				//最后一块
				byte[] destData = new byte[len];
				System.arraycopy(data, 0, destData, 0, len);
				
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
