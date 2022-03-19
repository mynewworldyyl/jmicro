package cn.jmicro.gateway.fs.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.task.TaskManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.Base64Utils;
import cn.jmicro.gateway.fs.api.FileJRso;
import cn.jmicro.gateway.fs.api.IFileJMSrv;

@Component
@Service(version="0.0.1",retryCnt=0,external=true,showFront=false)
public class FileService implements IFileJMSrv{

	//private static final String ID2NAME_SEP = PackageResource.ID2NAME_SEP;
	private static final Logger LOG = LoggerFactory.getLogger(FileService.class);
	
	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	//分块上传文件中每块大小
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 32767;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/openDebug", defGlobal=false)
	private boolean openDebug = false;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/resTimeout", defGlobal=true)
	private long resTimeout = 3*60*1000;
	
	@Cfg(value="/ResourceReponsitoryService/devMode", defGlobal=true)
	private boolean devMode = false;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/respServerUrl", defGlobal=true)
	private String resServerUrl = "https://repo1.maven.org/maven2/";
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject(required=false)
	private GridFS fs;
	
	//private String zkDir = Config.BASE_DIR + "/resDataDir";
	
	//当前资源存放目录
	private File dir = null;
	
	private File tempDir = null;
	
	private Map<Integer,InputStream> downloadReses = new HashMap<>();
	
	private Map<Integer,Long> downloadResourceTimeout = new ConcurrentHashMap<>();
	
	@Inject
	private IObjectStorage os;
	
	@Inject
	private MongoDatabase mongoDb;
	
	private TaskManager taskMng = null;
	
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
		
		taskMng = new TaskManager(5000);
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
	public IPromise<RespJRso<List<FileJRso>>> getFileList(Map<String,Object> qry,int pageSize,int curPage) {
		
		return new Promise<RespJRso<List<FileJRso>>>((reso,reje)->{
			RespJRso<List<FileJRso>> resp = new RespJRso<>(RespJRso.CODE_FAIL);
			
			Map<String,Object> conditions = new HashMap<>();
			
			String key = "status";
			if(qry.containsKey(key)) {
				conditions.put(key,new Double(Double.parseDouble(qry.get(key).toString())).intValue());
			}
			
			key = "clientId";
			if(qry.containsKey(key)) {
				conditions.put(key,new Double(Double.parseDouble(qry.get(key).toString())).intValue());
			}
			
			 key = "version";
			if(qry.containsKey(key)) {
				conditions.put(key, new Document("$regex",qry.get(key)));
			}
			
			 key = "group";
			if(qry.containsKey(key)) {
				conditions.put(key, new Document("$regex",qry.get(key)));
			}
			
			key = "artifactId";
			if(qry.containsKey(key)) {
				conditions.put(key, new Document("$regex",qry.get(key)));
			}
			
			key = "artifactIds";
			if(qry.containsKey(key)) {
				List<String> ats = Arrays.asList(qry.get(key).toString().split(","));
				conditions.put("artifactId", new Document("$in",ats));
			}
			
			
			key = "main";
			if(qry.containsKey(key)) {
				conditions.put(key,qry.get(key));
			}
			
			ActInfoJRso ai = JMicroContext.get().getAccount();
			//Document clientCond = new Document("$OR",);
			if(!qry.containsKey(Constants.CLIENT_ID) && !PermissionManager.isCurAdmin(Config.getClientId())) {
				List<Document> ql = new ArrayList<>();
				ql.add(new Document("clientId",ai.getClientId()));
				ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
				conditions.put("$or",ql);
			}
			
			int val =(int)os.count(FileJRso.TABLE, conditions);
			resp.setTotal(val);
			resp.setPageSize(pageSize);
			resp.setCurPage(curPage-1);
			if(val <= 0) {
				resp.setCode(0);
				resp.setData(Collections.EMPTY_LIST);
				reso.success(resp);
				return;
			}
			
			List<FileJRso> res = os.query(FileJRso.TABLE, conditions,FileJRso.class, 
					pageSize, curPage-1);
			resp.setData(res);
			reso.success(resp);
			return;
		});
		
	}
	
	@Override
	public IPromise<RespJRso<Boolean>> deleteFile(int id) {
		
		return new Promise<RespJRso<Boolean>>((reso,reje)->{
			RespJRso<Boolean> resp = new RespJRso<>(RespJRso.CODE_FAIL);
			
			Map<String,Object> queryConditions = new HashMap<>();
			queryConditions.put(IObjectStorage._ID, id);
			
			FileJRso pr = os.getOne(FileJRso.TABLE, queryConditions, FileJRso.class);
			
			if(pr == null) {
				resp.setCode(RespJRso.CODE_SUCCESS);
				reso.success(resp);
				return;
			}
			
			if(!PermissionManager.isOwner(pr.getCreatedBy())) {
				resp.setCode(RespJRso.CODE_FAIL);
				resp.setData(false);
				reso.success(resp);
				return;
			}
			
			os.deleteById(FileJRso.TABLE, pr.getId(), IObjectStorage._ID);
			
			File res = new File(getResParentDir(pr),pr.getName());
			if(res.exists()) {
				res.delete();
			}
			
			
			DBObject mt = new BasicDBObject();
			mt.put(IObjectStorage._ID, pr.getId());
			fs.remove(mt);
			
			resp.setData(true);
			resp.setCode(RespJRso.CODE_SUCCESS);
			reso.success(resp);
			return;
			
		});
		
	}

	private String getResParentDir(FileJRso pr) {
		return getResParentDir(pr.getGroup());
	}
	
	public String getResParentDir(String group) {
		return this.resDir + "/" + this.getJarFileSubDir(group);
	}

	public String getResDir() {
		return this.resDir;
	}

	@Override
	@SMethod(needLogin=true)
	public IPromise<RespJRso<FileJRso>> addFile(FileJRso pr) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		return new Promise<RespJRso<FileJRso>>((reso,reje)->{
			RespJRso<FileJRso> resp = new RespJRso<>(RespJRso.CODE_FAIL);
			
			pr.setCreatedBy(ai.getId());
			if(pr.getClientId() == Constants.NO_CLIENT_ID && !PermissionManager.isCurAdmin(Config.getClientId())) {
				String msg = "You cannot upload public resource" + pr.getName();
				LOG.error(msg);
				resp.setMsg(msg);
				resp.setCode(1);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				reso.success(resp);
				return;
			}
			
			if(!PermissionManager.isCurAdmin(Config.getClientId())) {
				pr.setClientId(ai.getClientId());
			}
			
			if(Utils.isEmpty(pr.getType()) && pr.getName() != null) {
				int extIdx = pr.getName().lastIndexOf(".");
				if(extIdx < 0) {
					String msg = "Resource extention name cannot be null";
					LOG.error(msg);
					resp.setMsg(msg);
					resp.setCode(1);
					reso.success(resp);
					return;
				}
				pr.setType(pr.getName().substring(extIdx+1));
			}
			
			//pr.setUpdateTime(TimeUtils.getCurTime());
			pr.setId(this.idGenerator.getLongId(FileJRso.class));
			pr.setBlockSize(this.uploadBlockSize);
			
			File resD = new File(this.tempDir,"" + pr.getId());
			if(!resD.exists()) {
				resD.mkdir();
			}
		
			pr.setFinishBlockNum(0);
			pr.setTotalBlockNum(getBlockNum(pr.getSize()));
			
			os.save(FileJRso.TABLE, pr, FileJRso.class, false, false);
			
			LOG.info("Add resource: " + pr.toString());
			resp.setCode(RespJRso.CODE_SUCCESS);
			resp.setData(pr);
			reso.success(resp);
			return;
		});
	}

	public int getBlockNum(long size) {
		int bn = (int)(size/this.uploadBlockSize);
		if(size % this.uploadBlockSize > 0) {
			bn++;
		}
		return bn;
	}

	@Override
	@SMethod(maxPacketSize=1024*1024*1)
	public IPromise<RespJRso<Boolean>> addFileData(int id, byte[] data, int blockNum) {
		return new Promise<RespJRso<Boolean>>((reso,reje)->{
			RespJRso<Boolean> resp = new RespJRso<>(RespJRso.CODE_FAIL);
			FileJRso zkrr = this.getPkg(id);

			if(zkrr == null) {
				String msg = "Resource is not ready to upload!";
				resp.setMsg(msg);
				resp.setCode(1);
				LOG.error(msg);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				reso.success(resp);
				return;
			}
			
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
				LOG.error(msg,e1);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				
				Map<String,Object> updater = new HashMap<>();
				updater.put("status", FileJRso.S_ERROR);
				
				Map<String,Object> filter = new HashMap<>();
				filter.put(IObjectStorage._ID, zkrr.getId());
				
				os.update(FileJRso.TABLE, filter, updater, FileJRso.class);
				
				reso.success(resp);
				return;
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
				
				Map<String,Object> updater = new HashMap<>();
				updater.put("status", FileJRso.S_FINISH);
				updater.put("size", zkrr.getSize());
				
				Map<String,Object> filter = new HashMap<>();
				filter.put(IObjectStorage._ID, zkrr.getId());
				
				os.update(FileJRso.TABLE, filter, updater, FileJRso.class);
				
				String msg = "Add resource success: " + zkrr.toString();
				LOG.info(msg);
				LG.log(MC.LOG_INFO, this.getClass(), msg);
				
			} else {
				if(openDebug) {
					LOG.debug("Name: " +zkrr.getName() +" blockNum: " + zkrr.getFinishBlockNum());
				}
				if(LG.isLoggable(MC.LOG_DEBUG)) {
					LG.log(MC.LOG_DEBUG, this.getClass(), "Name: " +zkrr.getName() +" blockNum: " + zkrr.getFinishBlockNum());
				}
				
				Map<String,Object> filter = new HashMap<>();
				filter.put(IObjectStorage._ID, zkrr.getId());
				
				Map<String,Object> updater = new HashMap<>();
				updater.put("finishBlockNum", zkrr.getFinishBlockNum());
				
				os.update(FileJRso.TABLE, filter,updater, FileJRso.class);
			}
			resp.setCode(RespJRso.CODE_SUCCESS);
			resp.setData(true);
			reso.success(resp);
			return;
		
		});
		
	}

	@Override
	public IPromise<RespJRso<FileJRso>> getFile(int resId) {
		return new Promise<RespJRso<FileJRso>>((reso,reje)->{
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, resId);
			FileJRso pr = os.getOne(FileJRso.TABLE, filter, FileJRso.class);
			RespJRso<FileJRso> resp = new RespJRso<>(RespJRso.CODE_SUCCESS);
			if(pr == null) {
				resp.setCode(RespJRso.CODE_FAIL);
				resp.setMsg("No data");
			}else {
				resp.setData(pr);
			}
			reso.success(resp);
			return;
		});
	}

	private FileJRso getPkg(int resId) {
		Map<String,Object> filter = new HashMap<>();
		filter.put(IObjectStorage._ID, resId);
		return os.getOne(FileJRso.TABLE, filter, FileJRso.class);
	}
	
	@Override
	@SMethod(needLogin=false,maxSpeed=1)
	public IPromise<RespJRso<Integer>> initDownloadFile(int actId,int resId) {

		return new Promise<RespJRso<Integer>>((reso,reje)->{
			RespJRso<Integer> resp = new RespJRso<>(RespJRso.CODE_FAIL);
			
			FileJRso pr = this.getPkg(resId);
			
			if(pr == null) {
				resp.setMsg("Resource " + resId + " not found!");
				resp.setData(-11);
				LG.log(MC.LOG_WARN, this.getClass(), resp.getMsg());
				reso.success(resp);
				return;
			}
			
			if(!(pr.getCreatedBy() == actId || pr.getClientId() == Constants.NO_CLIENT_ID)) {
				resp.setMsg("Permission reject for res: " + resId + " ,actId: " + actId);
				resp.setData(-11);
				LG.log(MC.LOG_WARN, this.getClass(), resp.getMsg());
				reso.success(resp);
				return;
			}
			
			Integer downloadId = this.idGenerator.getIntId(FileJRso.class);
			
			File resFile = new File(this.getResParentDir(pr),pr.getName());
			/*if(this.devMode) {
				File devFile = findResFile(name);
				if(devFile != null) {
					resFile = devFile;
				}
			}*/
			
			if(!resFile.exists()) {
				String msg = "File [" + resFile.getAbsolutePath() + "] not found!";
				resp.setMsg(msg);
				resp.setCode(1);
				LOG.error(msg);
				reso.success(resp);
				return;
			}
			
			//LOG.info("Init download resource name : " + pr.getPath() + ", downloadId: " +downloadId);
			LG.log(MC.LOG_INFO, this.getClass(), "Init download resource name : " + pr.getName() + ", downloadId: " +downloadId);
			try {
				this.downloadReses.put(downloadId, new FileInputStream(resFile));
				downloadResourceTimeout.put(downloadId, TimeUtils.getCurTime());
			} catch (FileNotFoundException e) {
				String msg = "File [" + downloadId+"] not found";
				resp.setMsg(msg);
				resp.setCode(1);
				LOG.error(msg);
				reso.success(resp);
				return;
			}
			
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, resId);
			
			Map<String,Object> updater = new HashMap<>();
			updater.put("downloadNum", pr.getDownloadNum()+1);
			updater.put("lastDownloadTime", TimeUtils.getCurTime());
			
			os.update(FileJRso.TABLE, filter,updater, FileJRso.class);
			
			resp.setData(downloadId);
			resp.setCode(RespJRso.CODE_SUCCESS);
			reso.success(resp);
			return;
			
		});
		
		
	}

	@Override
	@SMethod(needLogin=false,logLevel=MC.LOG_NO,retryCnt=0)
	public IPromise<RespJRso<byte[]>> downFileData(int downloadId, int specifyBlockNum) {
		return new Promise<RespJRso<byte[]>>((reso,reje)->{
			RespJRso<byte[]> resp = new RespJRso<>(RespJRso.CODE_FAIL);
			InputStream is = this.downloadReses.get(downloadId);
			if(is == null) {
				resp.setMsg("Resource not found ID: " + downloadId);
				LOG.error(resp.getMsg());
				reso.success(resp);
				return;
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
			resp.setData(data);
			resp.setCode(RespJRso.CODE_SUCCESS);
			reso.success(resp);
			return;
		});
	}

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=256)
	public IPromise<RespJRso<Map<String,Object>>> queryDict() {
		
		return new Promise<RespJRso<Map<String,Object>>>((reso,reje)->{
			RespJRso<Map<String,Object>> resp = new RespJRso<>();
			Map<String,Object> dists = new HashMap<>();
			resp.setData(dists);
			resp.setCode(RespJRso.CODE_SUCCESS);
			
			if(PermissionManager.isCurAdmin(Config.getClientId())) {
				MongoCollection<Document> rpcLogColl = mongoDb.getCollection(FileJRso.TABLE);
				
				DistinctIterable<Integer> clientIds = rpcLogColl.distinct("clientId", Integer.class);
				Set<Integer> host = new HashSet<>();
				for(Integer h : clientIds) {
					host.add(h);
				}
				Integer[] hostArr = new Integer[host.size()];
				host.toArray(hostArr);
				dists.put("clientIds", hostArr);
			} else {
				dists.put("clientIds", new Integer[] {JMicroContext.get().getAccount().getClientId()});
			}
			
			reso.success(resp);
			return;
		});
	}

	private void mergeUploadFile(FileJRso zkrr) {
		
		String bp = this.tempDir.getAbsolutePath() + "/" + zkrr.getId();
		File bpDir = new File(bp);
		
		File[] blockFiles = bpDir.listFiles( (dir,fn) -> {
			return Integer.parseInt(fn) >= 0;
		});
		
		Arrays.sort(blockFiles, (f1,f2)->{
			int num1 = Integer.parseInt(f1.getName());
			int num2 = Integer.parseInt(f2.getName());
			return num1 > num2?1:num1==num2?0:-1;
		});
		
		try {
			
			GridFSInputFile ff = this.fs.createFile();
			
			ff.setChunkSize(zkrr.getSize());
			ff.setContentType(zkrr.getType());
			ff.setFilename(zkrr.getName());
			ff.setId(zkrr.getId());
			 
			DBObject mt = new BasicDBObject();
			mt.put("createdBy", zkrr.getCreatedBy());
			mt.put("clientId", zkrr.getClientId());
			mt.put("group", zkrr.getGroup());
			ff.setMetaData(mt);
			
			OutputStream fos = ff.getOutputStream();
			
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
						/* if(i == blockFiles.length-1) {
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
						 LOG.error(str,e);
					 }finally {
						 if(fis != null) {
							 fis.close();
						 }
						 f.delete();
					 }
				 }
				
				zkrr.setSize(size);
				
				//w.close();
			}else {
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
			}
			 fos.close();
		} catch (IOException e) {
			LOG.error("finishFileUpload",e);
		} finally {
			bpDir.delete();
		}
	}
	
	public String getJarFileSubDir(String grp) {
		return grp.replaceAll("\\.", "/");
	}

	
	public static void main(String[] args) throws IOException {
		String pp = "E:\\docs\\pic\\bankcard.jpg";

		 FileInputStream fis = null;
		 try {
			 File f = new File(pp);
			 fis = new FileInputStream(f);
			 byte[] d = new byte[(int)f.length()];
			 fis.read(d, 0, (int)f.length());
			
			 String b64 = new String(Base64Utils.encode(d),Constants.CHARSET);
			 System.out.println(b64);
			 
			 Base64Utils.decode(b64);
			 
		 }finally {
			 if(fis != null) {
				 fis.close();
			 }
		 }
	 
	}
}
