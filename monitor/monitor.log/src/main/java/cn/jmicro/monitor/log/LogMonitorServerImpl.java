package cn.jmicro.monitor.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

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
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.monitor.ILogMonitorServer;
import cn.jmicro.api.monitor.IMonitorAdapter;
import cn.jmicro.api.monitor.LogWarningConfig;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcLogItem;
import cn.jmicro.api.monitor.MonitorAndService2TypeRelationshipManager;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.monitor.OneLog;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisConfig;
import cn.jmicro.api.monitor.genclient.ILogWarning$JMAsyncClient;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.IRaftListener;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.UniqueServiceMethodKey;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

@Component
@Service(version="0.0.1",debugMode=0,monitorEnable=0,logLevel=MC.LOG_WARN,retryCnt=0)
public class LogMonitorServerImpl implements ILogMonitorServer {

	private final static Logger logger = LoggerFactory.getLogger(LogMonitorServerImpl.class);
	
	private static final String GROUP = "monitorServer";
	
	private String logDir;
	
	//@Cfg(value="/MonitorServerImpl/monitoralbe", changeListener = "")
	private boolean monitoralbe = false;
	
	//@Reference(namespace="*", version="*", type="ins",required=false,changeListener="subscriberChange")
	//private Map<String,ILogWarning$JMAsyncClient> subsribers = new HashMap<>();
	
	//@Inject
	//private MonitorClient monitorManager;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MonitorAndService2TypeRelationshipManager mtManager;
	
	private RaftNodeDataListener<LogWarningConfig> configListener;
	
	private ServiceCounter sc = null;
	
	//private Queue<MRpcItem> cacheItems = new ConcurrentLinkedQueue<>();
	
	private BasketFactory<MRpcLogItem> basketFactory = null;
	
	private Map<String,LogWarningConfig> warningConfigs = new HashMap<>();
	
	private Set<String> deleteMonitors = new HashSet<>();
	
	private Map<String,LogWarningConfig> addMonitors = new HashMap<>();
	
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
	
	private IRaftListener<LogWarningConfig> lis = new IRaftListener<LogWarningConfig>() {
		public void onEvent(int type,String key, LogWarningConfig lw) {

			if(type == IListener.DATA_CHANGE) {

				lw = parseWarningConfig(lw,key);
				if(lw == null) {
					return;
				}
				
				if(!lw.isEnable() && !warningConfigs.containsKey(key)) {
					return;
				}

				LogWarningConfig olw = warningConfigs.get(key);
				
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
					if(LogWarningConfig.TYPE_FORWARD_SRV == lw.getType() 
							|| LogWarningConfig.TYPE_SAVE_FILE == lw.getType()) {
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
							logger.error("Init LogWarningConfig fail: "+JsonUtils.getIns().toJson(lw));
							return;
						}
					}
					olw.setType(lw.getType());
				} else if((LogWarningConfig.TYPE_FORWARD_SRV == lw.getType() 
							|| LogWarningConfig.TYPE_SAVE_FILE == lw.getType()) 
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
						logger.error("Init LogWarningConfig fail: "+JsonUtils.getIns().toJson(lw));
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
	
	private LogWarningConfig parseWarningConfig(LogWarningConfig lw, String id) {
		//LogWarningConfig lw = JsonUtils.getIns().fromJson(data, LogWarningConfig.class);
		
		if(Utils.isEmpty(lw.getExpStr())) {
			logger.error("Invalid config: " + JsonUtils.getIns().toJson(lw));
			return null;
		}
		
		List<String> suffix = ExpUtils.toSuffix(lw.getExpStr());
		if(!ExpUtils.isValid(suffix)) {
			logger.error("Invalid exp: " + id + "---> " + lw.getExpStr());
			return null;
		}
		
		if((LogWarningConfig.TYPE_FORWARD_SRV == lw.getType()
				|| LogWarningConfig.TYPE_SAVE_DB == lw.getType()
				|| LogWarningConfig.TYPE_SAVE_FILE == lw.getType()) && Utils.isEmpty(lw.getCfgParams())) {
			logger.error("Log table name cannot be NULL: " + lw.getId());
			return null;
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
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("LogMonitorServer");
		
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		basketFactory = new BasketFactory<MRpcLogItem>(1000,10);
		
		statusAdapter = new MonitorServerStatusAdapter();
		//statusAdapter.init();
		of.regist("logMonitorServerStatusAdapter", statusAdapter);

		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(IMonitorAdapter.class, 
				Config.getNamespace()+".LogMonitorServer", "0.0.1", null,Config.getClientId());
		sl.registService(si,statusAdapter);
		
		configListener = new RaftNodeDataListener<>(op,LOG_WARNING_ROOT,LogWarningConfig.class,false);
		configListener.addListener(lis);
		
		//op.addChildrenListener(LOG_WARNING_ROOT, lis);
				
		new Thread(this::doCheck,Config.getInstanceName()+"_MonitorServer_doCheck").start();
		
	}
	
	@Override
	@SMethod(timeout=5000,retryCnt=0,needResponse=false,debugMode=0,monitorEnable=0,logLevel=MC.LOG_ERROR
	,maxPacketSize=1048576,needLogin=false)
	public void submit(MRpcLogItem[] items) {
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
			IBasket<MRpcLogItem> b = basketFactory.borrowWriteBasket(true);
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
		
		while (true) {
			try {
				if(!addMonitors.isEmpty()) {
					synchronized (addMonitors) {
						Set<String> rmKeys = new HashSet<>();
						for (Map.Entry<String, LogWarningConfig> cfg : this.addMonitors.entrySet()) {
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
				
				//单线程操作财，无需同步
				Set<MRpcLogItem> sentItems = new HashSet<>();
				IBasket<MRpcLogItem> b = null;
				while((b = basketFactory.borrowReadSlot()) != null) {
					MRpcLogItem[] mis = new MRpcLogItem[b.remainding()];
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
					checkStatusMonitor();
					synchronized(cacheItemsLock) {
						cacheItemsLock.wait(2000);
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
	
	private boolean initLogWarningConfig(LogWarningConfig lw) {
		if(LogWarningConfig.TYPE_FORWARD_SRV == lw.getType()) {
			UniqueServiceMethodKey key = UniqueServiceMethodKey.fromKey(lw.getCfgParams());
			ILogWarning$JMAsyncClient srv = of.getRemoteServie(key.getServiceName(), key.getNamespace(), key.getVersion(),
					null);
			if(srv == null) {
				logger.error("Service not found for key: " + lw.getCfgParams()+", id: " + lw.getId());
				return false;
			}
			lw.setSrv(srv);
		}/*else if(LogWarningConfig.TYPE_SAVE_DB == lw.getType()) {
			if(Utils.isEmpty(lw.getCfgParams())) {
				logger.error("Log table name cannot be NULL: " + lw.getId());
				return true;
			}
		}*/else if(LogWarningConfig.TYPE_SAVE_FILE == lw.getType()) {
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
	
	private boolean addOneMonitor(LogWarningConfig lw) {
		try {
			//this.subsribers.put(cfg.getId(),srv);
			if(initLogWarningConfig(lw)) {
				this.warningConfigs.put(lw.getId(), lw);
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
		LogWarningConfig lw = warningConfigs.remove(m);
		if(lw != null) {
			if(LogWarningConfig.TYPE_SAVE_FILE == lw.getType()) {
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
		
		public Worker(Set<MRpcLogItem> mis) {
			this.mis = mis;
		}
		
		private Set<MRpcLogItem> mis;
		
		@Override
		public void run() {
			try {
				
				if(mis.isEmpty()){
					return;
				}
				
				JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR, true);
				
				if(openDebug) {
					logger.debug("Submit items: " + mis.size());
					//log(mis);
				}
				
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
	
	private void doNotify(Set<MRpcLogItem> mis) {
		long curTime = TimeUtils.getCurTime();
		
		Map<String,Object> cxt = new HashMap<>();
		Iterator<MRpcLogItem> ite = mis.iterator();
		
		for(;ite.hasNext();) {
			
			MRpcLogItem mi = ite.next();
			ite.remove();
			
			cxt.put("curTime", curTime);
			cxt.put("clientId", mi.getClientId());
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
			
			List<OneLog> backupItems = mi.getItems();
					
			for(LogWarningConfig cfg : this.warningConfigs.values()) {
				
				if(!(mi.getClientId() == cfg.getClientId() 
						|| Config.getAdminClientId() == cfg.getClientId())) {
					continue;
				}
				
				if((curTime - cfg.getLastNotifyTime()) < cfg.getMinNotifyInterval()) {
					continue;
				}
				
				/*int oriClientId = mi.getClientId();
				if(Config.getAdminClientId() == cfg.getClientId()) {
				}*/
				
				Exp exp = cfg.getExp();
				if(exp != null && exp.containerVar("tag") 
				   || exp.containerVar("level")
				   || exp.containerVar("time")
				   || exp.containerVar("ex")) {
					
					List<OneLog> items = new ArrayList<>();
					for(OneLog ol : mi.getItems()) {
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
				
				if(cfg.getType() == LogWarningConfig.TYPE_FORWARD_SRV) {
					ILogWarning$JMAsyncClient srv = cfg.getSrv();
					if(srv.isReady()) {
						cfg.setLastNotifyTime(curTime);
						srv.warnJMAsync(mi,mi)
						.fail((code,msg,mi0)->{
							logger.error("Notify fail for: " + code +":" + msg+ ": " + mi0.toString());
						});
					} else {
						logger.warn("Service not ready for log: " +cfg.getCfgParams());
					}
				}else if(cfg.getType() == LogWarningConfig.TYPE_SAVE_DB) {
					saveLog(mi,cfg.getCfgParams());
				}else {
					String logStr = toLogStr(mi);
					if(cfg.getType() == LogWarningConfig.TYPE_CONSOLE) {
						logger.info(logStr);
					}else if(cfg.getType() == LogWarningConfig.TYPE_SAVE_FILE) {
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
			
			cxt.clear();
		}
	}

	private String toLogStr(MRpcLogItem mi) {
		return JsonUtils.getIns().toJson(mi);
	}

	private void saveLog(MRpcLogItem mi,String tableName) {
		long curTime = TimeUtils.getCurTime();
		mi.setInputTime(curTime);
		if(mi.getReq() instanceof RpcRequest) {
			RpcRequest req = (RpcRequest)mi.getReq();
			ServiceItem si = reg.getServiceByCode(Integer.parseInt(req.getImpl()));
			if(si != null) {
				mi.setImplCls(si.getImpl());
			}
		}
		
		MongoCollection<Document> coll = mongoDb.getCollection(tableName);
		coll.insertOne(Document.parse(JsonUtils.getIns().toJson(mi)));
		
	}
	
	private void saveLog(Set<MRpcLogItem> temp) {
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
			Iterator<MRpcLogItem> itesm = temp.iterator();
			for(;itesm.hasNext();) {
				MRpcLogItem mi =  itesm.next();
				mi.setInputTime(curTime);
				if(mi.getReq() instanceof RpcRequest) {
					RpcRequest req = (RpcRequest)mi.getReq();
					ServiceItem si = reg.getServiceByCode(Integer.parseInt(req.getImpl()));
					if(si != null) {
						mi.setImplCls(si.getImpl());
					}
				}
				
				Document d = Document.parse(JsonUtils.getIns().toJson(mi));
				llDocs.add(d);
			
				/*if(mi.getReqId() > 0) {
					Document d = Document.parse(JsonUtils.getIns().toJson(mi));
					d.put("inputTime", System.currentTimeMillis());
					if(mi.getReq() instanceof RpcRequest) {
						RpcRequest req = (RpcRequest)mi.getReq();
						ServiceItem si = reg.getServiceByCode(Integer.parseInt(req.getImpl()));
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
			MongoCollection<Document> coll = mongoDb.getCollection("rpc_log");
			coll.insertMany(llDocs);
		}else if(!notRpcDocs.isEmpty()){
			MongoCollection<Document> coll = mongoDb.getCollection("nonrpc_log");
			coll.insertMany(notRpcDocs);
		}*/
		
	}

	
	public class MonitorServerStatusAdapter implements IMonitorAdapter {
		
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
		public MonitorServerStatus status() {
			if(!monitoralbe) {
				enableMonitor(true);
			}
			
			lastStatusTime = TimeUtils.getCurTime();
			
			MonitorServerStatus s = new MonitorServerStatus(); 
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
		public MonitorInfo info() {
			MonitorInfo info = new MonitorInfo();
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
