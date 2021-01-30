package cn.jmicro.choreography.agent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.task.TaskManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.choreography.api.IResourceResponsitory;
import cn.jmicro.choreography.api.PackageResource;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;

@Component
@Service(version="0.0.1",retryCnt=0,external=true)
public class ResourceReponsitoryService implements IResourceResponsitory{

	//private static final String ID2NAME_SEP = PackageResource.ID2NAME_SEP;
	private static final Logger LOG = LoggerFactory.getLogger(ResourceReponsitoryService.class);
	
	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	//分块上传文件中每块大小
	@Cfg(value="/ResourceReponsitoryService/uploadBlockSize", defGlobal=true)
	private int uploadBlockSize = 62000;//1024*1024;
	
	@Cfg(value="/ResourceReponsitoryService/openDebug", defGlobal=false)
	private boolean openDebug = true;//1024*1024;
	
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
	
	//private String zkDir = Config.BASE_DIR + "/resDataDir";
	
	//当前资源存放目录
	private File dir = null;
	
	private File tempDir = null;
	
	//private Map<String,PackageResource> blockFile = new HashMap<>();
	
	//private Map<Integer,PackageResource> blockIndexFiles = new HashMap<>();
	
	private Map<Integer,InputStream> downloadReses = new HashMap<>();
	
	private Map<Integer,Long> downloadResourceTimeout = Collections.synchronizedMap(new HashMap<>());
	
	@Inject
	private IObjectStorage os;
	
	@Inject
	private MongoDatabase mongoDb;
	
	private TaskManager taskMng = null;
	
	/*
	@Inject
	private IDataOperator op;
	*/
	
	public void ready() {
		
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
				downloadRes();		
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
	public Resp<List<Map<String,Object>>> waitingResList(int resId) {
		Resp<List<Map<String,Object>>> resp = new Resp<>();
		List<Map<String,Object>> l = new ArrayList<>();
		resp.setData(l);
		
		Map<String,Object> filter = new HashMap<>();
		filter.put(IObjectStorage._ID, resId);
		PackageResource pr = os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
		
		if(pr != null && pr.getWaitingRes() != null) {
			for(Integer r : pr.getWaitingRes()) {
				filter.put(IObjectStorage._ID, r);
				PackageResource pr0 = os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
				if(pr0 != null) {
					Map<String,Object> e = new HashMap<>();
					e.put("name", pr0.getName());
					e.put("status", pr0.getStatus());
					e.put("id", pr0.getId());
					l.add(e);
				}
			}
		}
		
		return resp;
	}
	
	@Override
	public Resp<List<Map<String,Object>>> dependencyList(int resId) {

		Resp<List<Map<String,Object>>> resp = new Resp<>();
		List<Map<String,Object>> l = new ArrayList<>();
		resp.setData(l);
		
		Map<String,Object> filter = new HashMap<>();
		filter.put(IObjectStorage._ID, resId);
		PackageResource pr = os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
		
		if(pr != null && pr.getDepIds() != null) {
			for(Integer r : pr.getDepIds()) {
				filter.put(IObjectStorage._ID, r);
				PackageResource pr0 = os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
				if(pr0 != null) {
					Map<String,Object> e = new HashMap<>();
					e.put("name", pr0.getName());
					e.put("status", pr0.getStatus());
					e.put("id", pr0.getId());
					e.put("group", pr0.getGroup());
					l.add(e);
				}
			}
		}
		
		return resp;
	
	}
	
	@Override
	public Resp<PackageResource> getAgentPackage(String version) {
		Map<String,Object> filter = new HashMap<>();
		
		filter.put("version", version);
		filter.put("group", "cn.jmicro");
		filter.put("artifactId", "jmicro.agent");
		filter.put("status", PackageResource.STATUS_ENABLE);
		
		List<PackageResource> res = os.query(PackageResource.TABLE_NAME, filter,PackageResource.class,1, 0);
		
		Resp<PackageResource> resp = new Resp<>();
		if(res == null || res.isEmpty()) {
			resp.setCode(Resp.CODE_FAIL);
		} else {
			resp.setData(res.iterator().next());
		}
		
		return resp;
	}

	@Override
	public Resp<List<Map<String,Object>>> getResourceListForDeployment(Map<String,Object> qry) {
		Resp<List<Map<String,Object>>> resp = new Resp<>(0);
		
		Map<String,Object> conditions = new HashMap<>();
		
		String key = "version";
		if(conditions.containsKey(key)) {
			conditions.put(key, new Document("$regex",conditions.get(key)));
		}
		
		key = "group";
		if(conditions.containsKey(key)) {
			conditions.put(key, new Document("$regex",conditions.get(key)));
		}
		
		key = "artifactId";
		if(conditions.containsKey(key)) {
			conditions.put(key, new Document("$regex",conditions.get(key)));
		}
		
		conditions.put("main",true);
		conditions.put("status",3);
		
		ActInfo ai = JMicroContext.get().getAccount();
		
		if(!PermissionManager.isCurAdmin()) {
			List<Document> ql = new ArrayList<>();
			ql.add(new Document("clientId",ai.getId()));
			ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
			conditions.put("$or",ql);
		}else if(qry.containsKey("clientId")) {
			conditions.put(key,new Double(Double.parseDouble(qry.get("clientId").toString())).intValue());
		}
		
		List<PackageResource> res = os.query(PackageResource.TABLE_NAME, conditions,PackageResource.class,
				Integer.MAX_VALUE, 0);
		
		if(res != null && !res.isEmpty()) {
			List<Map<String,Object>> l = new ArrayList<>();
			for(PackageResource pr : res) {
				Map<String,Object> m = new HashMap<>();
				m.put("id", pr.getId());
				m.put("name", pr.getName());
				l.add(m);
			}
			resp.setData(l);
		}
		
		return resp;
	}

	@Override
	public Resp<List<PackageResource>> getResourceList(Map<String,Object> qry,int pageSize,int curPage) {
		Resp<List<PackageResource>> resp = new Resp<>(0);
		
		Map<String,Object> conditions = new HashMap<>();
		/*if(qry != null && !qry.isEmpty()) {
			conditions.putAll(qry);
		}*/
		
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
			conditions.put(key, new Document("$regex",conditions.get(key)));
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
		
		ActInfo ai = JMicroContext.get().getAccount();
		//Document clientCond = new Document("$OR",);
		if(!qry.containsKey(Constants.CLIENT_ID) && !PermissionManager.isCurAdmin()) {
			List<Document> ql = new ArrayList<>();
			ql.add(new Document("clientId",ai.getId()));
			ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
			conditions.put("$or",ql);
		}
		
		int val =(int)os.count(PackageResource.TABLE_NAME, conditions);
		resp.setTotal(val);
		resp.setPageSize(pageSize);
		resp.setCurPage(curPage-1);
		if(val <= 0) {
			resp.setCode(0);
			resp.setData(Collections.EMPTY_LIST);
			return resp;
		}
		
		List<PackageResource> res = os.query(PackageResource.TABLE_NAME, conditions,PackageResource.class, 
				pageSize, curPage-1);
		resp.setData(res);
		return resp;
	}
	
	@Override
	public Resp<Boolean> deleteResource(int id) {
		Resp<Boolean> resp = new Resp<>(0);
		
		Map<String,Object> queryConditions = new HashMap<>();
		queryConditions.put(IObjectStorage._ID, id);
		
		PackageResource pr = os.getOne(PackageResource.TABLE_NAME, queryConditions, PackageResource.class);
		
		if(pr == null) {
			return resp;
		}
		
		if(!PermissionManager.isOwner(pr.getCreatedBy())) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setData(false);
			return resp;
		}
		
		os.deleteById(PackageResource.TABLE_NAME, pr.getId(), IObjectStorage._ID);
		
		File res = new File(getResParentDir(pr),pr.getName());
		if(res.exists()) {
			res.delete();
		}
		
		resp.setData(true);
		return resp;
	}

	private String getResParentDir(PackageResource pr) {
		return this.resDir + "/" + this.getJarFileSubDir(pr.getGroup());
	}

	@Override
	public Resp<PackageResource> updateResource(PackageResource pr0,boolean updateFile) {
		Resp<PackageResource> resp = new Resp<>(0);
		
		PackageResource pr = this.getPkg(pr0.getId());
		if(pr == null) {
			String msg = "Resource [" + pr0.getId() + "] not found";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_ERROR, this.getClass(), msg);
			return resp;
		}
		
		if(!PermissionManager.isOwner(pr.getCreatedBy())) {
			String msg = "Permission reject to update resource [" + pr0.getId() + "]";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_ERROR, this.getClass().getName(), msg, MC.MT_ACT_PERMISSION_REJECT);
			return resp;
		}
		
		Map<String,Object> updater = new HashMap<>();
		
		if(pr0.getStatus() != pr.getStatus() && pr0.getStatus() != 0) {
			if(PermissionManager.isCurAdmin() ||
					pr.getStatus() == PackageResource.STATUS_ENABLE && pr0.getStatus() == PackageResource.STATUS_READY 
					|| pr.getStatus() == PackageResource.STATUS_READY && pr0.getStatus() == PackageResource.STATUS_ENABLE) {
				updater.put("status", pr0.getStatus());
				pr.setStatus(pr0.getStatus());
			}
		}
		
		if(pr0.getClientId() != pr.getClientId()) {
			
			if(!PermissionManager.isCurAdmin() && pr0.getClientId() != Constants.NO_CLIENT_ID ) {
				//普通账号从公有包改为私有包不被允许
				String msg = "You cannot change resource from public to private [" + pr0.getId() + "]";
				LOG.error(msg);
				resp.setMsg(msg);
				resp.setCode(1);
				LG.log(MC.LOG_ERROR, this.getClass().getName(), msg, MC.MT_ACT_PERMISSION_REJECT);
				return resp;
			}
			
			if(pr0.getClientId() == Constants.NO_CLIENT_ID ) {
				//从私有包改为公有包,检测是否有同样的包存在，如果有，则禁止修改
				PackageResource existPr = this.getPackageResourceByOr(pr.getGroup(),pr.getArtifactId(), pr.getVersion(), "jar");
				if(existPr != null && existPr.getId() != pr.getId()) {
					String msg = "Exists with same group and artifactId and version: " + existPr.getName();
					LOG.error(msg);
					resp.setMsg(msg);
					resp.setCode(1);
					LG.log(MC.LOG_INFO, this.getClass(), msg);
					return resp;
				}
			}
			
			updater.put("clientId",pr0.getClientId());
			pr.setClientId(pr0.getClientId());
		}
		
		if(updateFile) {
			if(pr.getStatus() == PackageResource.STATUS_CHECK_FOR_DOWNLOAD
					|| pr.getStatus() == PackageResource.STATUS_ERROR
					|| pr.getStatus() == PackageResource.STATUS_UPLOADING 
					|| PermissionManager.isCurAdmin()) {
				pr0.setBlockSize(this.uploadBlockSize);
				updater.put("blockSize", this.uploadBlockSize);
				updater.put("size", pr0.getSize());
				updater.put("resVer", pr.getResVer()+1);
				updater.put("totalBlockNum", this.getBlockNum(pr0.getSize()));
				updater.put("uploadTime", TimeUtils.getCurTime());
				updater.put("modifiedTime", pr0.getModifiedTime());
				updater.put("finishBlockNum", 0);
				updater.put("status", PackageResource.STATUS_UPLOADING);
			} else {
				String msg = "File not in modififable status [" + pr0.getId() + "]";
				LOG.error(msg);
				resp.setMsg(msg);
				resp.setCode(1);
				LG.log(MC.LOG_ERROR, this.getClass().getName(), msg, MC.MT_ACT_PERMISSION_REJECT);
				return resp;
			}
		}
		
		/*if(!pr0.getGroup().equals(pr.getGroup())) {
			updater.put("group", pr0.getGroup());
		}*/
		
		if(!updater.isEmpty()) {
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, pr.getId());
			os.update(PackageResource.TABLE_NAME, filter, updater, PackageResource.class);
		}
		
		if(updater.containsKey("status") && pr.getStatus() == PackageResource.STATUS_ENABLE) {
			this.updateMainWaiting(pr.getId());
		}
		
		resp.setData(pr0);
		return resp;
	}

	@Override
	@SMethod(needLogin=true)
	public Resp<PackageResource> addResource(PackageResource pr) {
		ActInfo ai = JMicroContext.get().getAccount();
		Resp<PackageResource> resp = new Resp<>(0);
		
		if(Utils.isEmpty(pr.getName())) {
			String msg = "Resource name cannot be null";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			return resp;
		}
		
		pr.setGroup(pr.getGroup().trim());
		
		if(Utils.isEmpty(pr.getGroup())) {
			String msg = "Resource group cannot be null";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			return resp;
		}
		
		if(Utils.isEmpty(pr.getArtifactId())) {
			String msg = "Resource artifactId cannot be null";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			return resp;
		}
		
		if(Utils.isEmpty(pr.getVersion())) {
			String msg = "Resource version cannot be null";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			return resp;
		}
		
		PackageResource existPr = this.getPackageResourceByOr(pr.getGroup(),pr.getArtifactId(), pr.getVersion(), "jar");
		if(existPr != null && pr.getClientId() == Constants.NO_CLIENT_ID) {
			String msg = "Resource exist with same group and artifactId and version";
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_INFO, this.getClass(), msg);
			return resp;
		}
		
	/*	File resFile = new File(this.dir,pr.getId()+".jar");
		resFile.createNewFile();*/
		
		pr.setCreatedBy(ai.getId());
		
		if(pr.getClientId() == Constants.NO_CLIENT_ID && !PermissionManager.isCurAdmin()) {
			String msg = "You cannot upload public resource" + pr.getName();
			LOG.error(msg);
			resp.setMsg(msg);
			resp.setCode(1);
			LG.log(MC.LOG_ERROR, this.getClass().getName(), msg, MC.MT_ACT_PERMISSION_REJECT);
			return resp;
		}
		
		if(!PermissionManager.isCurAdmin()) {
			pr.setClientId(ai.getId());
		}
		
		if(Utils.isEmpty(pr.getResExtType())) {
			int extIdx = pr.getName().lastIndexOf(".");
			if(extIdx < 0) {
				String msg = "Resource extention name cannot be null";
				LOG.error(msg);
				resp.setMsg(msg);
				resp.setCode(1);
				return resp;
			}
			pr.setResExtType(pr.getName().substring(extIdx+1));
		}
		
		pr.setStatus(PackageResource. STATUS_UPLOADING);
		pr.setUploadTime(TimeUtils.getCurTime());
		//pr.setUpdateTime(TimeUtils.getCurTime());
		pr.setId(this.idGenerator.getIntId(PackageResource.class));
		pr.setBlockSize(this.uploadBlockSize);
		
		File resD = new File(this.tempDir,"" + pr.getId());
		if(!resD.exists()) {
			resD.mkdir();
		}
	
		pr.setFinishBlockNum(0);
		pr.setTotalBlockNum(getBlockNum(pr.getSize()));
		pr.setResVer(1);
		
		String msg = createResDir(pr);
		if(msg != null) {
			resp.setMsg(msg);
			resp.setCode(1);
			return resp;
		}
		
		os.save(PackageResource.TABLE_NAME, pr, PackageResource.class, false, false);
		
		LOG.info("Add resource: " + pr.toString());
		resp.setData(pr);
		return resp;
	}
	
	private String createResDir(PackageResource pr) {
		
		String[] grpArr = pr.getGroup().split("\\.");
		
		File f = this.dir;
		
		for(String p : grpArr) {
			f = new File(f,p);
			if(!f.exists()) {
				f.mkdir();
			}
		}
		
		/*f = new File(f,pr.getArtifactId());
		if(!f.exists()) {
			f.mkdir();
		}
		
		f = new File(f,pr.getVersion());
		if(!f.exists()) {
			f.mkdir();
		}*/
		
		/*f = new File(f,pr.getName());
		if(!f.exists()) {
			try {
				f.createNewFile();
				return null;
			} catch (IOException e) {
				String msg = "Fail to create file: " + f.getAbsolutePath();
				LOG.error(msg,e);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				return msg;
			}
		}*/
		return null;
	}

	public int getBlockNum(long size) {
		int bn = (int)(size/this.uploadBlockSize);
		if(size % this.uploadBlockSize > 0) {
			bn++;
		}
		return bn;
	}

	@Override
	@SMethod
	public Resp<Boolean> addResourceData(int id, byte[] data, int blockNum) {
		Resp<Boolean> resp = new Resp<>(0);
		PackageResource zkrr = this.getPkg(id);
		synchronized(zkrr) {
			if(zkrr == null) {
				String msg = "Resource is not ready to upload!";
				resp.setMsg(msg);
				resp.setCode(1);
				LOG.error(msg);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				return resp;
			}
			
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
				updater.put("status", PackageResource.STATUS_ERROR);
				
				Map<String,Object> filter = new HashMap<>();
				filter.put(IObjectStorage._ID, zkrr.getId());
				
				os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
				
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
				mergeUploadFile(zkrr);
				
				Map<String,Object> filter = new HashMap<>();
				filter.put(IObjectStorage._ID, zkrr.getId());
				
				if(!checkDependencies(zkrr)) {
					String msg = zkrr.getName() + " is not valid!";
					resp.setMsg(msg);
					resp.setCode(1);
					LOG.error(msg);
					LG.log(MC.LOG_ERROR, this.getClass(), msg);
					
					Map<String,Object> updater = new HashMap<>();
					updater.put("status", PackageResource.STATUS_ERROR);
					updater.put("finishBlockNum", zkrr.getFinishBlockNum());
					updater.put("uploadTime", TimeUtils.getCurTime());
					
					os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
					
					return resp;
				} else {
					String msg = "Add resource success: " + zkrr.toString();
					LOG.info(msg);
					LG.log(MC.LOG_INFO, this.getClass(), msg);
				}
				
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
				updater.put("status", PackageResource.STATUS_UPLOADING);
				updater.put("finishBlockNum", zkrr.getFinishBlockNum());
				
				os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
			}
			
			return resp;
		}
	}
	
	@Override
	public Resp<PackageResource> getResource(int resId) {
		Map<String,Object> filter = new HashMap<>();
		filter.put(IObjectStorage._ID, resId);
		PackageResource pr = os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
		Resp<PackageResource> resp = new Resp<>();
		if(pr == null) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("No data");
		}else {
			resp.setData(pr);
		}
		return resp;
	}
	
	@Override
	public Resp<Boolean> clearInvalidDbFile() {
		this.taskMng.submitTask(()->{
			doClearInvalidDbFile();
		});
		return new Resp<Boolean>(0,true);
	}
	
	private void doClearInvalidDbFile() {
		long startTime = TimeUtils.getCurTime();
		MongoCollection<Document> coll = mongoDb.getCollection(PackageResource.TABLE_NAME);
		long size = coll.countDocuments();
		int pageSize = 100;
		
		int batchNum = (int)(size/100);
		if((size % 100) > 0) {
			batchNum++;
		}
		
		List<Document> aggregateList = new ArrayList<Document>();
		
		Document prj = new Document();
		prj.put(IObjectStorage._ID, 1);
		prj.put("name", 1);
		prj.put("group", 1);
		aggregateList.add(new Document("$project",prj));
	
		Document skip = new Document("$skip", 0);
		Document limit = new Document("$limit", pageSize);
		aggregateList.add(skip);
		aggregateList.add(limit);
		
		for(int i = 0; i < batchNum; i++) {
			skip.put("$skip", pageSize*i);
			AggregateIterable<Document> resultset = coll.aggregate(aggregateList);
			MongoCursor<Document> cursor = resultset.iterator();
			try {
				while(cursor.hasNext()) {
					Document pr = cursor.next();
					int id = pr.getInteger(IObjectStorage._ID);
					String name = pr.getString("name");
					String grp = pr.getString("group");
					if(!new File(this.resDir+"/"+this.getJarFileSubDir(grp),name).exists()) {
						coll.deleteOne(new Document(IObjectStorage._ID,id));
					}
				}
			} finally {
				cursor.close();
			}
		}
		
		String msg = "doClearInvalidDbFile cost time: " + (TimeUtils.getCurTime() - startTime)+"MS";
		LOG.info(msg);
		LG.log(MC.LOG_INFO, this.getClass(), msg);
		
	}

	@Override
	public Resp<Boolean> clearInvalidResourceFile() {
		this.taskMng.submitTask(()->{
			doClearInvalidResourceFile();
		});
		return new Resp<Boolean>(0,true);
	}

	private void doClearInvalidResourceFile() {
		long startTime = TimeUtils.getCurTime();
		File[] files = this.dir.listFiles();
		
		MongoCollection<Document> coll = mongoDb.getCollection(PackageResource.TABLE_NAME);
		Document filter = new Document(IObjectStorage._ID,0);
		for(int i = 0; i < files.length; i++) {
			File f = files[i];
			String[] arr = f.getName().split("#\\$#");
			if(arr.length == 2) {
				filter.put(IObjectStorage._ID,Integer.parseInt(arr[0]));
				if(coll.countDocuments(filter) == 0) {
					f.delete();
				}
			}
		}
		String msg = "doClearInvalidResourceFile cost time: " + (TimeUtils.getCurTime() - startTime)+"MS";
		LOG.info(msg);
		LG.log(MC.LOG_INFO, this.getClass(), msg);
	}

	private boolean checkDependencies(PackageResource zkrr) {
		
		JarFile jf = null;
		try {
			File resFile = new File(this.getResParentDir(zkrr),zkrr.getName());
			jf = new JarFile(resFile,true);
			ActInfo ai = JMicroContext.get().getAccount();
			if(!ai.isAdmin()) {
				Enumeration<JarEntry> jes = jf.entries();
				while(jes.hasMoreElements()) {
					JarEntry je  = jes.nextElement();
					String name = je.getName();
					if(name.endsWith("/")) {
						name = name.substring(0,name.length()-1);
					}
					name = name.replace("/", ".");
					for(String pkg : Constants.SYSTEM_PCK_NAME_PREFIXES) {
						if(name.startsWith(pkg)) {
							String msg = "Invalid file: " + je.getName() + " in jar file: " + zkrr.getName();
							LG.log(MC.LOG_ERROR, this.getClass(), msg);
							LOG.error(msg);
							return false;
						}
					}
				}
			}
			
			zkrr.setStatus(PackageResource.STATUS_ENABLE);
			
			Manifest mf = jf.getManifest();
			Attributes attrs = mf.getMainAttributes();
			Object attr = attrs.getValue("Main-Class");
			boolean rst = false;
			if(attr == null) {
				zkrr.setMain(false);
				rst = true;
			} else {
				zkrr.setMain(true);
				
				Set<Integer> depResIds = new HashSet<>();
				zkrr.setDepIds(depResIds);
				
				ZipEntry je = jf.getEntry(PackageResource.DEP_FILE);
				if(je != null) {
					InputStream ji = jf.getInputStream(je);
					BufferedReader br = new BufferedReader(new InputStreamReader(ji));
					//去除前面两行
					br.readLine();
					br.readLine();
					String line = br.readLine();
					while(!Utils.isEmpty(line)) {
						String[] arr = line.split(":");
						String group = arr[0].trim();
						String arti = arr[1].trim();
						String jar = arr[2].trim();
						String ver = arr[3].trim();
						
						PackageResource depRes = getPackageResourceByOr(group,arti,ver,jar);
						if(depRes == null) {
							if(ai.isGuest()) {
								//游客账号不能触发依赖包下载
								String msg = "Guest "+ai.getActName()+" cannot trigger download resource " + line + " in jar file: " + zkrr.getName();
								LG.log(MC.LOG_ERROR, this.getClass(), msg);
								LOG.error(msg);
								return false;
							}
							//异步下载资源，完成后加到本资源依赖中
							if(zkrr.getWaitingRes() == null) {
								zkrr.setWaitingRes(new HashSet<Integer>());
							}
							
							depRes = downloadResFromMaven(group,arti,ver);
						}
						
						if(depRes.getStatus() != PackageResource.STATUS_ENABLE) {
							zkrr.getWaitingRes().add(depRes.getId());
							zkrr.setStatus(PackageResource.STATUS_WAITING);
						}
						
						depResIds.add(depRes.getId());
						
						line = br.readLine();
					}
				}
				rst = true;
			}
			
			Map<String,Object> updater = new HashMap<>();
			updater.put("status", zkrr.getStatus());
			updater.put("finishBlockNum", zkrr.getFinishBlockNum());
			updater.put("main", zkrr.isMain());
			updater.put("depIds", zkrr.getDepIds());
			updater.put("waitingRes", zkrr.getWaitingRes());
			updater.put("uploadTime", TimeUtils.getCurTime());
			
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, zkrr.getId());
			
			os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
			
			if(zkrr.getStatus() == PackageResource.STATUS_ENABLE) {
				updateMainWaiting(zkrr.getId());
			}
			
			return rst;
		} catch (IOException e) {
			LG.log(MC.LOG_ERROR, this.getClass(), e.getMessage() +", "+zkrr);
			return false;
		}finally {
			if(jf != null) {
				try {
					jf.close();
				} catch (IOException e) {
					LOG.error("",e);
				}
			}
		}
	}

	private void updateMainWaiting(int prid) {
		Map<String,Object> depQry = new HashMap<>();
		//depQry.put("status", PackageResource.STATUS_WAITING);
		depQry.put("waitingRes",prid);
		
		List<PackageResource> depRes = os.query(PackageResource.TABLE_NAME, depQry,
				PackageResource.class, 10000, 0);
		if(depRes != null && !depRes.isEmpty()) {
			for(PackageResource r : depRes) {
				r.getWaitingRes().remove(prid);
				
				Map<String,Object> f = new HashMap<>();
				f.put(IObjectStorage._ID, r.getId());
				
				Map<String,Object> up = new HashMap<>();
				if(r.getWaitingRes().isEmpty()) {
					up.put("status", PackageResource.STATUS_READY);
					up.put("waitingRes", null);
					LG.log(MC.LOG_INFO, this.getClass(), "Resource ready: " + r.toString());
				}else {
					up.put("waitingRes", r.getWaitingRes());
				}
				os.update(PackageResource.TABLE_NAME, f, up, PackageResource.class);
			}
		}
		
	}

	private PackageResource downloadResFromMaven(String group, String arti, String ver) {
		
		Map<String,Object> filter = new HashMap<>();
		filter.put("group", group);
		filter.put("artifactId", arti);
		filter.put("version", ver);
		filter.put("resExtType", "jar");
		//filter.put("status", PackageResource.STATUS_ENABLE);
		
		List<Document> ql = new ArrayList<>();
		ql.add(new Document("clientId",JMicroContext.get().getAccount().getId()));
		ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
		filter.put("$or",ql);
		
		PackageResource pr = os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
		if(pr != null) {
			return pr;
		}
		
		pr = new PackageResource();
		pr.setGroup(group);
		pr.setArtifactId(arti);
		pr.setVersion(ver);
		pr.setClientId(Constants.NO_CLIENT_ID);
		pr.setCreatedBy(JMicroContext.get().getAccount().getId());
		pr.setId(this.idGenerator.getIntId(PackageResource.class));
		pr.setName(arti+"-"+ver+".jar");
		pr.setResExtType("jar");
		
		this.createResDir(pr);
		
		File jf = new File(this.getResParentDir(pr),pr.getName());
		if(jf.exists() && jf.length() > 0) {
			//文件已经存在，不用重新下载
			pr.setStatus(PackageResource.STATUS_ENABLE);
		}else {
			pr.setStatus(PackageResource.STATUS_CHECK_FOR_DOWNLOAD);
		}
		
		os.save(PackageResource.TABLE_NAME, pr,PackageResource.class, false, false);
		return pr;
		
	}

	private PackageResource getPackageResourceByOr(String group,String arti, String ver, String jar) {
		
		Map<String,Object> filter = new HashMap<>();
		if(!Utils.isEmpty(group)) {
			filter.put("group", group);
		}
		
		filter.put("artifactId", arti);
		filter.put("version", ver);
		filter.put("resExtType", jar);
		filter.put("status", PackageResource.STATUS_ENABLE);
		
		List<Document> ql = new ArrayList<>();
		ql.add(new Document("clientId",JMicroContext.get().getAccount().getId()));
		ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
		filter.put("$or",ql);
		
		return os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
	}

	private PackageResource getPkg(int resId) {
		Map<String,Object> filter = new HashMap<>();
		filter.put(IObjectStorage._ID, resId);
		return os.getOne(PackageResource.TABLE_NAME, filter, PackageResource.class);
	}
	
	@Override
	@SMethod(needLogin=false,maxSpeed=1)
	public Resp<Integer> initDownloadResource(int actId,int resId) {

		Resp<Integer> resp = new Resp<>(0);
		
		PackageResource pr = this.getPkg(resId);
		
		if(pr == null) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("Resource " + resId + " not found!");
			resp.setData(-11);
			LG.log(MC.LOG_WARN, this.getClass(), resp.getMsg());
			return resp;
		}
		
		if(!(pr.getCreatedBy() == actId || pr.getClientId() == Constants.NO_CLIENT_ID)) {
			resp.setCode(Resp.CODE_FAIL);
			resp.setMsg("Permission reject for res: " + resId + " ,actId: " + actId);
			resp.setData(-11);
			LG.log(MC.LOG_WARN, this.getClass(), resp.getMsg());
			return resp;
		}
		
		Integer downloadId = this.idGenerator.getIntId(PackageResource.class);
		
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
			return resp;
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
			return resp;
		}
		
		Map<String,Object> filter = new HashMap<>();
		filter.put(IObjectStorage._ID, resId);
		
		Map<String,Object> updater = new HashMap<>();
		updater.put("downloadNum", pr.getDownloadNum()+1);
		updater.put("lastDownloadTime", TimeUtils.getCurTime());
		
		os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
		
		resp.setData(downloadId);
		return resp;
	}
	

	@Override
	@SMethod(needLogin=false,logLevel=MC.LOG_NO,retryCnt=0)
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

	@Override
	@SMethod(perType=false,needLogin=true,maxSpeed=10,maxPacketSize=256)
	public Resp<Map<String,Object>> queryDict() {
		
		Resp<Map<String,Object>> resp = new Resp<>();
		Map<String,Object> dists = new HashMap<>();
		resp.setData(dists);
		resp.setCode(Resp.CODE_SUCCESS);
		
		if(PermissionManager.isCurAdmin()) {
			MongoCollection<Document> rpcLogColl = mongoDb.getCollection(PackageResource.TABLE_NAME);
			
			DistinctIterable<Integer> clientIds = rpcLogColl.distinct("clientId", Integer.class);
			Set<Integer> host = new HashSet<>();
			for(Integer h : clientIds) {
				host.add(h);
			}
			Integer[] hostArr = new Integer[host.size()];
			host.toArray(hostArr);
			dists.put("clientIds", hostArr);
		}else {
			dists.put("clientIds", new Integer[] {JMicroContext.get().getAccount().getId()});
		}
		
		return resp;
	}
	

	private void mergeUploadFile(PackageResource zkrr) {
		
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
		
		File resFile = new File(this.getResParentDir(zkrr), zkrr.getName());
		
		FileOutputStream fos = null;
		try {
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
			LOG.error("finishFileUpload",e);
		}finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					LOG.error("",e);
				}
			}
			bpDir.delete();
		}
	}
	
	private String getJarFileSubDir(String grp) {
		return grp.replaceAll("\\.", "/");
	}

	private boolean running = false;
	private void downloadRes() {
		//org/sdase/commons/sda-commons-server-dropwizard/2.16.1/sda-commons-server-dropwizard-2.16.1.jar
		
		Map<String,Object> conditions = new HashMap<>();
		conditions.put("status", PackageResource.STATUS_CHECK_FOR_DOWNLOAD);
		conditions.put("group", new Document("$ne","cn.jmicro"));//非JMIcro核心库才能从第三方下载
		List<PackageResource> res = os.query(PackageResource.TABLE_NAME, conditions,PackageResource.class, 
				1000, 0);
		if(res == null || res.isEmpty()) {
			return;
		}
		running = true;
		this.taskMng.submitTask(new DownloadWorker(res));
		
	}
	
	public boolean downloadFromMavenCenterJar(String url, File targetFilePath) {
		
		LG.log(MC.LOG_INFO, this.getClass(), "Begin download: " +url);
		BufferedReader in = null;
		FileOutputStream out = null;
		try {
			URL realUrl = new URL(url);
			URLConnection httpConnect = realUrl.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) httpConnect;
			httpUrlConnection.connect();
			int responseCode = httpUrlConnection.getResponseCode();
			InputStream inputStream = null;
			
			if (responseCode == 200) {
				inputStream = httpUrlConnection.getInputStream();
				out = new FileOutputStream(targetFilePath);
				byte[] data = new byte[1024];
				int len = -1;
				while((len = inputStream.read(data)) > 0) {
					out.write(data, 0, len);
				}
				return true;
			} else {
				StringBuilder result = new StringBuilder();
				inputStream = new BufferedInputStream(httpUrlConnection.getErrorStream());
				in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				String line;
				while ((line = in.readLine()) != null) {
					result.append(line);
				}
				LOG.error("Fail get: " + url);
				LOG.error(result.toString());
			}
		} catch (Exception e) {
			LOG.error(url, e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if(out != null) {
					out.close();
				}
			} catch (Exception e2) {
				LOG.error(url, e2);
			}
		}
		return false;
	}
	
	private class DownloadWorker implements Runnable{

		private List<PackageResource> resList = null;
		
		private DownloadWorker(List<PackageResource> resList) {
			this.resList = resList;
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					
					if(resList.isEmpty()) {
						running = false;
						return;
					}
					
					Iterator<PackageResource> ite = resList.iterator();
					while(ite.hasNext()) {
						PackageResource pr = ite.next();
						if(downloadOne(pr)) {
							ite.remove();
						}
					}
				}catch(Throwable e) {
					LOG.error("",e);
					LG.log(MC.LOG_ERROR, this.getClass(), "",e);
				}
			}
			
		}
		
	}

	public boolean downloadOne(PackageResource pr) {
		
		String url = resServerUrl + pr.getGroup().replace(".", "/") + "/";
		url += pr.getArtifactId()+"/"+pr.getVersion()+"/";
		url += pr.getName();
		
		File jarFile = new File(this.getResParentDir(pr),pr.getName());
		if(!jarFile.exists()) {
			try {
				jarFile.createNewFile();
			} catch (IOException e) {
				String msg = "Fail to create file: " + jarFile.getAbsolutePath();
				LOG.error(msg,e);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				return false;
			}
		}
		
		if(!downloadFromMavenCenterJar(url,jarFile)) {
			return false;
		}
		
		if(!securityChecked(jarFile)) {
			LG.log(MC.LOG_INFO, this.getClass(), "Jar with securyty issue from: " + url + " jar: " + jarFile.getAbsolutePath());
			
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, pr.getId());
			
			Map<String,Object> updater = new HashMap<>();
			updater.put("status", PackageResource.STATUS_CHECK_SECURITY);
			updater.put("size", jarFile.length());
			updater.put("uploadTime", TimeUtils.getCurTime());
			
			os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
		} else {
			LG.log(MC.LOG_INFO, this.getClass(), "Resource from:" + url + " ready: " + pr.toString());
			
			Map<String,Object> filter = new HashMap<>();
			filter.put(IObjectStorage._ID, pr.getId());
			
			Map<String,Object> updater = new HashMap<>();
			updater.put("status", PackageResource.STATUS_ENABLE);
			updater.put("size", jarFile.length());
			updater.put("uploadTime", TimeUtils.getCurTime());
			os.update(PackageResource.TABLE_NAME, filter,updater, PackageResource.class);
			
			updateMainWaiting(pr.getId());
		}
		
		return true;
	
	}

	private boolean securityChecked(File jarFile) {
		JarFile jf = null;
		try {
			jf = new JarFile(jarFile,true);
			Enumeration<JarEntry> jes = jf.entries();
			while(jes.hasMoreElements()) {
				JarEntry je  = jes.nextElement();
				String name = je.getName();
				if(name.endsWith("/")) {
					name = name.substring(0,name.length()-1);
				}
				name = name.replace("/", ".");
				for(String pkg : Constants.SYSTEM_PCK_NAME_PREFIXES) {
					if(name.startsWith(pkg)) {
						return false;
					}
				}
			}
			
		} catch (IOException e) {
			LG.log(MC.LOG_ERROR, this.getClass(), "", e);
			return false;
		}finally {
			if(jf != null) {
				try {
					jf.close();
				} catch (IOException e) {
				}
			}
		}
	
		return true;
	}
	

}
