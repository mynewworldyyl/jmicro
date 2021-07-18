package cn.jmicro.monitor.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.internal.validator.CollectibleDocumentFieldNameValidator;

import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.basket.BasketFactory;
import cn.jmicro.api.basket.IBasket;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfigJRso;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.ILogMonitorServerJMSrv;
import cn.jmicro.api.monitor.IMonitorAdapterJMSrv;
import cn.jmicro.api.monitor.JMLogItemJRso;
import cn.jmicro.api.monitor.LogWarningConfigJRso;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorAndService2TypeRelationshipManager;
import cn.jmicro.api.monitor.MonitorInfoJRso;
import cn.jmicro.api.monitor.MonitorServerStatusJRso;
import cn.jmicro.api.monitor.OneLogJRso;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.genclient.ILogWarningJMSrv$JMAsyncClient;
import cn.jmicro.api.net.RpcRequestJRso;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

@Component
@Service(clientId=Constants.NO_CLIENT_ID,version="0.0.1",debugMode=0,monitorEnable=0,
logLevel=MC.LOG_WARN,retryCnt=0,limit2Packages="cn.jmicro.api.monitor",showFront=false,external=false)
public class LogMonitorServerImpl implements ILogMonitorServerJMSrv {

	private final static Logger logger = LoggerFactory.getLogger(LogMonitorServerImpl.class);
	
	private static final String GROUP = "monitorServer";
	
	private String logDir;
	
	//@Cfg(value="/MonitorServerImpl/monitoralbe", changeListener = "")
	private boolean monitoralbe = false;
	
	//@Reference(namespace="*", version="*", type="ins",required=false,changeListener="subscriberChange")
	//private Map<String,ILogWarningJMSrv$JMAsyncClient> subsribers = new HashMap<>();
	
	//@Inject
	//private MonitorClient monitorManager;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MonitorAndService2TypeRelationshipManager mtManager;
	
	private RaftNodeDataListener<LogWarningConfigJRso> configListener;
	
	private ServiceCounter sc = null;
	
	//private Queue<MRpcItem> cacheItems = new ConcurrentLinkedQueue<>();
	
	private BasketFactory<JMLogItemJRso> basketFactory = null;
	
	private Map<String,LogWarningConfigJRso> warningConfigs = new HashMap<>();
	
	//clientId to config include -1 which match all items clientId
	private Map<Integer,Set<String>> client2Configs = new HashMap<>();
	
	private Set<String> deleteMonitors = new HashSet<>();
	
	private Map<String,LogWarningConfigJRso> addMonitors = new HashMap<>();
	
	private Object cacheItemsLock = new Object();
	
	private ExecutorService executor = null;
	
	private long lastStatusTime = TimeUtils.getCurTime();
	
	private MonitorServerStatusAdapter statusAdapter;
	
	//@Cfg(value="/MonitorServerImpl/openDebug")
	private boolean openDebug = true;
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	private IRaftListener<LogWarningConfigJRso> lis = new IRaftListener<LogWarningConfigJRso>() {
		public void onEvent(int type,String key, LogWarningConfigJRso lw) {

			if(type == IListener.DATA_CHANGE) {

				lw = parseWarningConfig(lw,key);
				if(lw == null) {
					return;
				}
				
				if(!lw.isEnable() && !warningConfigs.containsKey(lw.getId())) {
					return;
				}

				LogWarningConfigJRso olw = warningConfigs.get(key);
				
				if(!lw.isEnable()) {
					if(olw != null && olw.isEnable()) {
						//禁用配置
						synchronized(deleteMonitors) {
							deleteMonitors.add(key);
						}
					}
					return;
				}
				
				if(olw == null) {
					//启动配置
					synchronized(addMonitors) {
						addMonitors.put(key,lw);
					}
					
					//op.addDataListener(path, warnDataChangeListener);
					synchronized(cacheItemsLock) {
						cacheItemsLock.notify();
					}
					return;
				}
				
				if(olw.getType() != lw.getType()) {
					if(LogWarningConfigJRso.TYPE_FORWARD_SRV == lw.getType() 
							|| LogWarningConfigJRso.TYPE_SAVE_FILE == lw.getType()) {
						if(initLogWarningConfig(lw)) {
							if(olw.getBw() != null) {
								try {
									olw.getBw().close();
								} catch (IOException e) {
									logger.error("Close writer fail");
								}
							}
							olw.setBw(lw.getBw());
							olw.setSrv(lw.getSrv());
						} else {
							logger.error("Init LogWarningConfigJRso fail: "+JsonUtils.getIns().toJson(lw));
							return;
						}
					}
					olw.setType(lw.getType());
				} else if((LogWarningConfigJRso.TYPE_FORWARD_SRV == lw.getType() 
							|| LogWarningConfigJRso.TYPE_SAVE_FILE == lw.getType()) 
						&& !lw.getCfgParams().equals(olw.getCfgParams())) {
					if(initLogWarningConfig(lw)) {
						if(olw.getBw() != null) {
							try {
								olw.getBw().close();
							} catch (IOException e) {
								logger.error("Close writer fail");
							}
						}
						olw.setBw(lw.getBw());
						olw.setSrv(lw.getSrv());
					}else {
						logger.error("Init LogWarningConfigJRso fail: "+JsonUtils.getIns().toJson(lw));
						return;
					}
				}
				
				olw.setExp(lw.getExp());
				olw.setExpStr(lw.getExpStr());
				olw.setMinNotifyInterval(lw.getMinNotifyInterval());
				olw.setTag(lw.getTag());
				olw.setCfgParams(lw.getCfgParams());
				
				synchronized(cacheItemsLock) {
					cacheItemsLock.notify();
				}
			
			}else if(type == IListener.REMOVE){
				if(!warningConfigs.containsKey(key)) {
					//删除没有启用的配置，无需处理
					return;
				}
				synchronized(deleteMonitors) {
					deleteMonitors.add(key);
				}
				//op.removeDataListener(LOG_WARNING_ROOT+"/"+key, warnDataChangeListener);
				synchronized(cacheItemsLock) {
					cacheItemsLock.notify();
				}
			}else if(type == IListener.ADD) {
				lw = parseWarningConfig(lw,key);
				if(lw == null || !lw.isEnable()) {
					return;
				}
				
				synchronized(addMonitors) {
					addMonitors.put(key,lw);
				}
				
				//op.addDataListener(LOG_WARNING_ROOT+"/"+key, warnDataChangeListener);
				synchronized(cacheItemsLock) {
					cacheItemsLock.notify();
				}
			}
		}
	};
	
	private LogWarningConfigJRso parseWarningConfig(LogWarningConfigJRso lw, String id) {
		//LogWarningConfigJRso lw = JsonUtils.getIns().fromJson(data, LogWarningConfigJRso.class);
		
		if(Utils.isEmpty(lw.getExpStr())) {
			logger.error("Invalid config: " + JsonUtils.getIns().toJson(lw));
			return null;
		}
		
		List<String> suffix = ExpUtils.toSuffix(lw.getExpStr());
		if(!ExpUtils.isValid(suffix)) {
			logger.error("Invalid exp: " + id + "---> " + lw.getExpStr());
			return null;
		}
		
		if((LogWarningConfigJRso.TYPE_FORWARD_SRV == lw.getType()
				|| LogWarningConfigJRso.TYPE_SAVE_DB == lw.getType()
				|| LogWarningConfigJRso.TYPE_SAVE_FILE == lw.getType()) && Utils.isEmpty(lw.getCfgParams())) {
			logger.error("Log table name cannot be NULL: " + lw.getId());
			return null;
		}
		
		if(LogWarningConfigJRso.TYPE_CONSOLE == lw.getType()) {
			lw.setCfgParams("console");
		}
		
		Exp exp = new Exp();
		exp.setSuffix(suffix);
		exp.setOriEx(lw.getExpStr());
		
		lw.setExp(exp);
		lw.setId(id);
		
		return lw;
	}
	
	@JMethod("ready")
	public void ready() {
		
		logDir = System.getProperty("user.dir")+"/logs/molog/";
		File d = new File(logDir);
		if(!d.exists()) {
			d.mkdirs();
		}
		
		ExecutorConfigJRso config = new ExecutorConfigJRso();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("LogMonitorServer");
		
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		basketFactory = new BasketFactory<JMLogItemJRso>(1000,10);
		
		statusAdapter = new MonitorServerStatusAdapter();
		//statusAdapter.init();
		of.regist("logMonitorServerStatusAdapter", statusAdapter);

		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItemJRso si = sl.createSrvItem(IMonitorAdapterJMSrv.class, 
				Config.getNamespace()+".LogMonitorServer", "0.0.1", null,Config.getClientId());
		sl.registService(si,statusAdapter);
		
		configListener = new RaftNodeDataListener<>(op,LOG_WARNING_ROOT,LogWarningConfigJRso.class,false);
		configListener.addListener(lis);
		
		//op.addChildrenListener(LOG_WARNING_ROOT, lis);
				
		new Thread(this::doCheck,Config.getInstanceName()+"_MonitorServer_doCheck").start();
		
	}
	
	@Override
	@SMethod(timeout=5000,retryCnt=0,needResponse=false,debugMode=0,monitorEnable=0,
	logLevel=MC.LOG_NO,maxPacketSize=1048576,needLogin=false)
	public void submit(JMLogItemJRso[] items) {
		if(items == null || items.length == 0) {
			/*if(monitoralbe) {
				sc.add(MonitorConstant.Ms_SubmitCnt, 1);
			}*/
			logger.error("Cannot submit NULL items");
			return;
		}
		
		if(monitoralbe) {
			//sc.add(MonitorConstant.Ms_SubmitCnt, 1);
			sc.add(MC.Ms_ReceiveItemCnt, items.length);
		}
		
		/*if(openDebug) {
			log(Arrays.asList(items));
		}*/
		
		int pos = 0;
		while(pos < items.length) {
			IBasket<JMLogItemJRso> b = basketFactory.borrowWriteBasket(true);
			if(b != null) {
				int re = b.remainding();
				int len = re;
				if(items.length - pos < re) {
					len = items.length - pos;
				}	
				if(b.write(items,pos,len)) {
					boolean rst = basketFactory.returnWriteBasket(b, true);
					if(rst) {
						pos += len;
						continue;
					}else {
						if(monitoralbe) {
							sc.add(MC.Ms_FailReturnWriteBasket, 1);
							sc.add(MC.Ms_FailItemCount, items.length);
						}
						logger.error("Fail to return basket fail size: "+ (items.length - pos));
						break;
					}
					
				} else {
					basketFactory.returnWriteBasket(b, true);
					logger.error("Fail write basket size: "+ (items.length - pos));
					if(monitoralbe) {
						//sc.add(MonitorConstant.Ms_Fail2BorrowBasket, 1);
						sc.add(MC.Ms_FailItemCount, items.length - pos);
					}
					break;
				}	
			}else {
				if(monitoralbe) {
					sc.add(MC.Ms_Fail2BorrowBasket, 1);
					sc.add(MC.Ms_FailItemCount, items.length - pos);
				}
				logger.error("Fail size: "+ (items.length - pos));
				break;
			}
		}
		
		if(pos > 0) {
			synchronized(cacheItemsLock) {
				cacheItemsLock.notify();
			}
		}
	}
	
	private void doCheck() {
		int checkInterval = 2000;
		while (true) {
			try {
				if(!addMonitors.isEmpty()) {
					synchronized (addMonitors) {
						Set<String> rmKeys = new HashSet<>();
						for (Map.Entry<String, LogWarningConfigJRso> cfg : this.addMonitors.entrySet()) {
							if(addOneMonitor(cfg.getValue())) {
								rmKeys.add(cfg.getKey());
							}
						}
						for(String k : rmKeys) {
							this.addMonitors.remove(k);
						}
					}
				}

				if(!deleteMonitors.isEmpty()) {
					synchronized (deleteMonitors) {
						for (Iterator<String> ite = this.deleteMonitors.iterator(); ite.hasNext();) {
							String m = ite.next();
							if (deleteOneMonitor(m)) {
								ite.remove();
							}
						}
					}
				}
				
				//单线程操作，无需同步
				Set<JMLogItemJRso> sentItems = new HashSet<>();
				IBasket<JMLogItemJRso> b = null;
				while((b = basketFactory.borrowReadSlot()) != null) {
					JMLogItemJRso[] mis = new JMLogItemJRso[b.remainding()];
					b.readAll(mis);
					sentItems.addAll(Arrays.asList(mis));
					if(!basketFactory.returnReadSlot(b, true)) {
						logger.error("doCheck Fail to return IBasket");
					}
					b = null;
				}
				
				/*if(monitoralbe) {
					sc.add(MC.Ms_CheckLoopCnt, 1);
				}*/
				
				if(sentItems.isEmpty()) {
					long st = TimeUtils.getCurTime();
					checkStatusMonitor();
					deleteInvalidLog();
					
					long cost = checkInterval - (TimeUtils.getCurTime() - st);
					if(cost > 0) {
						synchronized(cacheItemsLock) {
							cacheItemsLock.wait(cost);
						}
					}
					continue;
				} else {
					//直接入库
					this.executor.submit(new Worker(sentItems));
				}
				
			} catch (Throwable ex) {
				//永不结束线程
				logger.error("doCheck", ex);
				if(monitoralbe) {
					sc.add(MC.Ms_CheckerExpCnt, 1);
				}
			}
		}
	}
	
	private void deleteInvalidLog() {
		Document match = new Document();
		//match.put("items.desc", new Document("$eq",Constants.INVALID_LOG_DESC));
		match.put("items", new Document("$size",0));
		match.put("delCheck", new Document("$exists",false));
		match.put("provider", false);
		match.put("createTime",  new Document("$lt",TimeUtils.getCurTime() - 60000));  //60秒前产生的数据才需要删除
		
		Document prj = new Document();
		prj.put("_id", 1);
		prj.put("linkId", 1);
		
		MongoCollection<Document> coll = this.mongoDb.getCollection(JMLogItemJRso.TABLE,Document.class);
		
		List<Document> pl = new ArrayList<>();
		pl.add(new Document("$match",match));
		pl.add(new Document("$project",prj));
		
		AggregateIterable<Document> rst = coll.aggregate(pl);
		for(Document d : rst) {
			Document smatch = new Document();
			smatch.put("linkId", d.get("linkId"));
			long cnt = coll.countDocuments(smatch);
			if(cnt < 3) {
				coll.deleteMany(smatch);
			} else {
				Document updateMatch = new Document();
				prj.put("_id", d.get("_id"));
				Document update = new Document("delCheck",1);
				coll.updateOne(updateMatch, new Document("$set",update));
			}
		}
		
		Document nmatch = new Document();
		//match.put("items.desc", new Document("$eq",Constants.INVALID_LOG_DESC));
		nmatch.put("items", new Document("$size",0));
		nmatch.put("linkId", 0);
		nmatch.put("reqId", 0);
		coll.deleteMany(nmatch);//无效非RPC日志
		
	}

	private boolean initLogWarningConfig(LogWarningConfigJRso lw) {
		if(LogWarningConfigJRso.TYPE_FORWARD_SRV == lw.getType()) {
			UniqueServiceMethodKeyJRso key = UniqueServiceMethodKeyJRso.fromKey(lw.getCfgParams());
			ILogWarningJMSrv$JMAsyncClient srv = of.getRemoteServie(key.getServiceName(), key.getNamespace(), key.getVersion(),
					null);
			if(srv == null) {
				logger.error("Service not found for key: " + lw.getCfgParams()+", id: " + lw.getId());
				return false;
			}
			lw.setSrv(srv);
		}/*else if(LogWarningConfigJRso.TYPE_SAVE_DB == lw.getType()) {
			if(Utils.isEmpty(lw.getCfgParams())) {
				logger.error("Log table name cannot be NULL: " + lw.getId());
				return true;
			}
		}*/else if(LogWarningConfigJRso.TYPE_SAVE_FILE == lw.getType()) {
			File logFile = new File(this.logDir + lw.getId()+"_"+lw.getCfgParams());
			if(!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					logger.error("Create log file fail",e);
					return true;
				}
			}
			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
				lw.setBw(bw);
			} catch (FileNotFoundException e) {
				logger.error("Create writer fail",e);
				return true;
			}
		}
		
		return true;
	}
	
	private boolean addOneMonitor(LogWarningConfigJRso lw) {
		try {
			//this.subsribers.put(cfg.getId(),srv);
			if(initLogWarningConfig(lw)) {
				this.warningConfigs.put(lw.getId(), lw);
				Set<String> cfgs = this.client2Configs.get(lw.getClientId());
				if(cfgs == null) {
					cfgs = new HashSet<>();
					client2Configs.put(lw.getClientId(), cfgs);
				}
				cfgs.add(lw.getId());
				return true;
			}
			return false;
		} catch (Throwable e) {
			logger.error("Call intrest got error:",e);
			return false;
		}
	}
	
	private boolean deleteOneMonitor(String m) {
		//subsribers.remove(m);
		LogWarningConfigJRso lw = warningConfigs.remove(m);
		Set<String> cfgs = this.client2Configs.get(lw.getClientId());
		if(cfgs != null) {
			cfgs.remove(lw.getId());
		}
		
		if(lw != null) {
			if(LogWarningConfigJRso.TYPE_SAVE_FILE == lw.getType()) {
				try {
					lw.getBw().close();
				} catch (IOException e) {
					logger.error("deleteOneMonitor",e);
				}
			}
		}
		
		return true;
	}
	
	private void checkStatusMonitor() {
		if(monitoralbe && (TimeUtils.getCurTime() - lastStatusTime > 300000)) {//5分钏没有状态请求
			logger.warn("ServiceCounter timeout 5 minutes, and stop it");
			statusAdapter.enableMonitor(false);
		}
	}
	
	private class Worker implements Runnable{
		
		public Worker(Set<JMLogItemJRso> mis) {
			this.mis = mis;
		}
		
		private Set<JMLogItemJRso> mis;
		
		@Override
		public void run() {
			try {
				
				if(mis.isEmpty()){
					return;
				}
				
				JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR, true);
				
				/*if(openDebug) {
					logger.debug("Submit items: " + mis.size());
					//log(mis);
				}*/
				
				//saveLog(mis);
				
				doNotify(mis);
				
				if(monitoralbe) {
					sc.add(MC.Ms_TaskSuccessItemCnt, mis.size());
				}
			} catch (Throwable e) {
				logger.error("",e);
				if(monitoralbe) {
					sc.add(MC.Ms_TaskFailItemCnt, mis.size());
				}
			}
		}
	}
	
	private void doNotify(Set<JMLogItemJRso> mis) {
		long curTime = TimeUtils.getCurTime();
		
		Map<String,Object> cxt = new HashMap<>();
		Iterator<JMLogItemJRso> ite = mis.iterator();
		
		for(;ite.hasNext();) {
			
			JMLogItemJRso mi = ite.next();
			ite.remove();
			mi.setId(idServer.getLongId(JMLogItemJRso.class));
			
			cxt.put("curTime", curTime);
			cxt.put("actClientId", mi.getActClientId());
			cxt.put("sysClientId", mi.getSysClientId());
			cxt.put("actName", mi.getActName());
			
			if(mi.getSmKey() != null) {
				cxt.put("serviceName", mi.getSmKey().getServiceName());
				cxt.put("namespace", mi.getSmKey().getNamespace());
				cxt.put("version", mi.getSmKey().getVersion());
				cxt.put("method", mi.getSmKey().getMethod());
			}
			
			cxt.put("implCls", mi.getImplCls());
			cxt.put("localHost", mi.getLocalHost());
			cxt.put("localPort", mi.getLocalPort());
			cxt.put("remoteHost", mi.getRemotePort());
			cxt.put("remotePort", mi.getRemotePort());
			cxt.put("instanceName", mi.getInstanceName());
			cxt.put("createTime", mi.getCreateTime());
			cxt.put("inputTime", mi.getInputTime());
			cxt.put("costTime", mi.getInputTime());
			
			Map<String,Set<Long>> sendItems = new HashMap<>();
			
			if(mi.getSysClientId() != Constants.NO_CLIENT_ID) {
				processOneItem(cxt,mi,this.client2Configs.get(mi.getSysClientId()),sendItems);
			}
			
			if(mi.getActClientId() != Constants.NO_CLIENT_ID && mi.getActClientId() != mi.getSysClientId()) {
				processOneItem(cxt,mi,this.client2Configs.get(mi.getActClientId()),sendItems);
			}
			
			processOneItem(cxt,mi,this.client2Configs.get(Constants.NO_CLIENT_ID),sendItems);
			
			cxt.clear();
		}
	}

	private void processOneItem(Map<String, Object> cxt, JMLogItemJRso mi, Set<String> cfgs,Map<String,Set<Long>> sendItems) {
		
		if(cfgs == null || cfgs.isEmpty()) {
			return;
		}
		
		List<OneLogJRso> backupItems = mi.getItems();
		
		long curTime = TimeUtils.getCurTime();
		
		synchronized(cfgs) {
			for(String id : cfgs) {
				LogWarningConfigJRso cfg = this.warningConfigs.get(id);
				
				if(sendItems.containsKey(cfg.getCfgParams()) && 
						sendItems.get(cfg.getCfgParams()).contains(mi.getId())) {
					continue;
				}
				
				if((curTime - cfg.getLastNotifyTime()) < cfg.getMinNotifyInterval()) {
					continue;
				}
				
				/*int oriClientId = mi.getClientId();
				if(Config.getAdminClientId() == cfg.getClientId()) {
				}*/
				
				Exp exp = cfg.getExp();
				if(exp != null && (exp.containerVar("tag") 
				   || exp.containerVar("level")
				   || exp.containerVar("time")
				   || exp.containerVar("ex"))) {
					
					List<OneLogJRso> items = new ArrayList<>();
					for(OneLogJRso ol : mi.getItems()) {
						cxt.put("tag",  ol.getTag() );
						cxt.put("level", ol.getLevel());
						cxt.put("time", ol.getTime());
						cxt.put("ex", ol.getEx());
						if(ExpUtils.compute(cfg.getExp(), cxt, Boolean.class)) {
							items.add(ol);
						}
					}
					
					if(items.isEmpty()) {
						continue;
					}
					mi.setItems(items);
				}
				
				mi.setTag(cfg.getTag());
				mi.setConfigId(cfg.getId());
				
				if(!sendItems.containsKey(cfg.getCfgParams())) {
					sendItems.put(cfg.getCfgParams(), new HashSet<Long>());
				}
				sendItems.get(cfg.getCfgParams()).add(mi.getId());
				
				if(cfg.getType() == LogWarningConfigJRso.TYPE_FORWARD_SRV) {
					if(!sendItems.containsKey(cfg.getCfgParams())) {
						sendItems.put(cfg.getCfgParams(), new HashSet<Long>());
					}
					ILogWarningJMSrv$JMAsyncClient srv = cfg.getSrv();
					if(srv.isReady()) {
						cfg.setLastNotifyTime(curTime);
						srv.warnJMAsync(mi,mi)
						.fail((code,msg,mi0)->{
							logger.error("Notify fail for: " + code +":" + msg+ ": " + mi0.toString());
						});
					} else {
						logger.warn("Service not ready for log: " +cfg.getCfgParams());
					}
				}else if(cfg.getType() == LogWarningConfigJRso.TYPE_SAVE_DB) {
					saveLog(mi,cfg.getCfgParams());
				}else {
					String logStr = toLogStr(mi);
					if(cfg.getType() == LogWarningConfigJRso.TYPE_CONSOLE) {
						logger.info(logStr);
					}else if(cfg.getType() == LogWarningConfigJRso.TYPE_SAVE_FILE) {
						try {
							cfg.getBw().write(logStr);
							cfg.getBw().newLine();
							cfg.getBw().flush();
						} catch (IOException e) {
							logger.error("logStr",e);
						}
					}
				}
				mi.setItems(backupItems);
			}
		}
	}

	private String toLogStr(JMLogItemJRso mi) {
		return JsonUtils.getIns().toJson(mi);
	}

	private CollectibleDocumentFieldNameValidator validtor = new CollectibleDocumentFieldNameValidator();
	
	private void saveLog(JMLogItemJRso mi,String tableName) {
		long curTime = TimeUtils.getCurTime();
		mi.setInputTime(curTime);
		if(mi.getReq() instanceof RpcRequestJRso) {
			RpcRequestJRso req = (RpcRequestJRso)mi.getReq();
			UniqueServiceKeyJRso siKey = reg.getServiceByCode(req.getSvnHash());
			if(siKey != null) {
				ServiceItemJRso si = this.srvManager.getServiceByKey(siKey.fullStringKey());
				mi.setImplCls(si.getImpl());
			}
		}
		
		MongoCollection<Document> coll = mongoDb.getCollection(tableName);
		String json = JsonUtils.getIns().toJson(mi);
		/*if(json.contains("cn.jmicro.api.monitor.IMonitorDataSubscriber")) {
			logger.warn(json);
		}*/
		
		Document doc = Document.parse(json);
		//避免java.lang.IllegalArgumentException: Invalid BSON field name错误
		if(mi.getResp() != null && mi.getResp().getResult() != null) {
			Document rsp = doc.get("resp", Document.class);
			doc.put("resp",checkDocument(rsp));
		}
		
		if(mi.getReq() != null && mi.getReq().getArgs() != null) {
			Document rsp = doc.get("req", Document.class);
			doc.put("req",checkDocument(rsp));
		}
		
		coll.insertOne(doc);
		
	}
	
	private Document checkDocument(Document rst) {
		Set<String> keys = new HashSet<>();
		keys.addAll(rst.keySet());
		for(String key : keys) {
			Object jo = rst.get(key);
			String k = key;
			if(!validtor.validate(key)) {
				k = key.replaceAll("\\.", "/");
				rst.put(k, jo);
				rst.remove(key);
			}
			if(jo instanceof Document) {
				rst.put(k,checkDocument((Document)jo));
			}else if (jo instanceof Collection) {
				Collection col = (Collection)jo;
				for(Object ov : col) {
					if(ov instanceof Document) {
						checkDocument((Document)ov);
					}
				}
			}
		}
		
		return rst;
	}

	private void saveLog(Set<JMLogItemJRso> temp) {
		if(this.openDebug) {
			logger.debug("printLog One LOOP");
		}
		
		if(temp == null || temp.isEmpty()) {
			return;
		}
		
		List<Document> llDocs = new ArrayList<>();
		//List<Document> notRpcDocs = new ArrayList<>();
		
		synchronized(temp) {
			long curTime = TimeUtils.getCurTime();
			Iterator<JMLogItemJRso> itesm = temp.iterator();
			for(;itesm.hasNext();) {
				JMLogItemJRso mi =  itesm.next();
				mi.setInputTime(curTime);
				if(mi.getReq() instanceof RpcRequestJRso) {
					RpcRequestJRso req = (RpcRequestJRso)mi.getReq();
					UniqueServiceKeyJRso siKey = reg.getServiceByCode(req.getSvnHash());
					if(siKey != null) {
						ServiceItemJRso si = this.srvManager.getServiceByKey(siKey.fullStringKey());
						mi.setImplCls(si.getImpl());
					}
				}
				
				Document d = Document.parse(JsonUtils.getIns().toJson(mi));
				llDocs.add(d);
			
				/*if(mi.getReqId() > 0) {
					Document d = Document.parse(JsonUtils.getIns().toJson(mi));
					d.put("inputTime", System.currentTimeMillis());
					if(mi.getReq() instanceof RpcRequestJRso) {
						RpcRequestJRso req = (RpcRequestJRso)mi.getReq();
						ServiceItemJRso si = reg.getServiceByCode(Integer.parseInt(req.getImpl()));
						if(si != null) {
							d.put("implCls", si.getImpl());
						}
					}
					llDocs.add(d);
				} else {
					if(mi.getItems() != null && mi.getItems().size() > 0) {
						for(OneItem oi : mi.getItems()) {
							oi.setInstanceName(mi.getInstanceName());
							Document d = Document.parse(JsonUtils.getIns().toJson(oi));
							notRpcDocs.add(d);
						}
					}
				}*/
				//itesm.remove();
			}
		}
		
		MongoCollection<Document> coll = mongoDb.getCollection("rpc_log");
		coll.insertMany(llDocs);
		/*
		if(!llDocs.isEmpty()) {
			MongoCollection<Document> coll = mongoDb.getCollection(JMLogItemJRso.TABLE);
			coll.insertMany(llDocs);
		}else if(!notRpcDocs.isEmpty()){
			MongoCollection<Document> coll = mongoDb.getCollection(JMLogItemJRso.TABLE);
			coll.insertMany(notRpcDocs);
		}*/
		
	}

	
	public class MonitorServerStatusAdapter implements IMonitorAdapterJMSrv {
		
		public final Short[] TYPES  = {
				MC.Ms_ReceiveItemCnt,MC.Ms_TaskSuccessItemCnt,MC.Ms_CheckLoopCnt,
				MC.Ms_FailItemCount,MC.Ms_CheckerSubmitItemCnt,
				MC.Ms_Fail2BorrowBasket,MC.Ms_FailReturnWriteBasket,
				MC.Ms_CheckerExpCnt,MC.Ms_TaskFailItemCnt
		};
		
		public String[] typeLabels  = null;
		
		private MonitorServerStatusAdapter() {
			this.init0();
		}
		
		private void init0() {
			typeLabels = new String[TYPES.length];
			for(int i = 0; i < TYPES.length; i++) {
				typeLabels[i] = MC.MONITOR_VAL_2_KEY.get(TYPES[i]);
			}
		}
		
		@Override
		public MonitorServerStatusJRso status() {
			if(!monitoralbe) {
				enableMonitor(true);
			}
			
			lastStatusTime = TimeUtils.getCurTime();
			
			MonitorServerStatusJRso s = new MonitorServerStatusJRso(); 
			//s.setInstanceName(Config.getInstanceName());
			//s.setSubsriberSize(regSubs.size());
			//s.getSubsriber2Types().putAll(this.monitorManager.getMkey2Types());
			//s.setSendCacheSize(sentItems.size());
			
			double[] qpsArr = new double[TYPES.length];
			double[] curArr = new double[TYPES.length];
			double[] totalArr = new double[TYPES.length];
			
			s.setCur(curArr);
			s.setQps(qpsArr);
			s.setTotal(totalArr);
			
			for(int i = 0; i < TYPES.length; i++) {
				Short t = TYPES[i];
				
				totalArr[i] = sc.getTotal(t);
				 curArr[i] = new Double(sc.get(t));
				 if(t == MC.Ms_CheckLoopCnt) {
					 //System.out.println("");
				 }
				 qpsArr[i] = sc.getQps(TimeUnit.SECONDS, t);
				
			}
			
			return s;
		}

		
		@Override
		public void enableMonitor(boolean enable) {
			if(enable && !monitoralbe) {
				if(sc == null) {
					synchronized(this) {
						if(sc == null) {
							sc = new ServiceCounter("LogMonitorServerImpl-"+Config.getInstanceName(),
									TYPES,60*3L,1,TimeUnit.SECONDS);
						}
					}
				}
				monitoralbe = enable;
			} else if(!enable && monitoralbe) {
				monitoralbe = enable;
			}
		}

		@Override
		public MonitorInfoJRso info() {
			MonitorInfoJRso info = new MonitorInfoJRso();
			info.setGroup(GROUP);
			info.setTypeLabels(typeLabels);
			info.setTypes(TYPES);
			info.setRunning(monitoralbe);
			info.setInstanceName(Config.getInstanceName());
			info.getSubsriber2Types().putAll(mtManager.getMkey2Types());;
			return info;
		}

		@Override
		public void reset() {
			
		}
		
	}
	
}
