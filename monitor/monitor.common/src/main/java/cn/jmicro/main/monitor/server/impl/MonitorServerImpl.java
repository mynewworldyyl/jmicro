package cn.jmicro.main.monitor.server.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.basket.BasketFactory;
import cn.jmicro.api.basket.IBasket;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.executor.ExecutorConfig;
import cn.jmicro.api.executor.ExecutorFactory;
import cn.jmicro.api.monitor.IMonitorAdapter;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.IMonitorServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.monitor.MonitorTypeManager;
import cn.jmicro.api.monitor.OneItem;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxy;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.common.Constants;

@Component
@Service(namespace="monitorServer",version="0.0.1",debugMode=0,
monitorEnable=0,logLevel=MC.LOG_WARN,retryCnt=0)
public class MonitorServerImpl implements IMonitorServer {

	private final static Logger logger = LoggerFactory.getLogger(MonitorServerImpl.class);
	
	private static final String GROUP = "monitorServer";
	
	//@Cfg(value="/MonitorServerImpl/monitoralbe", changeListener = "")
	private boolean monitoralbe = false;
	
	@Reference(required=false,changeListener="subscriberChange")
	private Set<IMonitorDataSubscriber> subsribers = new HashSet<>();
	
	//@Inject
	//private MonitorClient monitorManager;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MonitorTypeManager mtManager;
	
	private ServiceCounter sc = null;
	
	private Set<RegItem> regSubs = new HashSet<>();
	
	//private Queue<MRpcItem> cacheItems = new ConcurrentLinkedQueue<>();
	
	private BasketFactory<MRpcItem> basketFactory = null;
	
	private Set<IMonitorDataSubscriber> deleteMonitors = new HashSet<>();
	
	private Set<IMonitorDataSubscriber> addMonitors = new HashSet<>();
	
	private Set<MRpcItem> sentItems = new HashSet<>();
	
	private Object cacheItemsLock = new Object();
	
	private ExecutorService executor = null;
	
	private long lastStatusTime = System.currentTimeMillis();
	
	private MonitorServerStatusAdapter statusAdapter;
	
	@Cfg(value="/MonitorServerImpl/openDebug")
	private boolean openDebug = false;
	
	public void init() {
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("MonitorServer");
		executor = ExecutorFactory.createExecutor(config);
		basketFactory = new BasketFactory<MRpcItem>(1000,10);	
	}
	
	@JMethod("ready")
	public void ready() {
		
		statusAdapter = new MonitorServerStatusAdapter();
		//statusAdapter.init();
		of.regist("monitorServerStatusAdapter", statusAdapter);
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(IMonitorAdapter.class, 
				Config.getInstanceName()+"."+MonitorServerStatusAdapter.class.getName(), "0.0.1", null);
		sl.registService(si,statusAdapter);
		
		new Thread(this::doCheck,Config.getInstanceName()+"_MonitorServer_doCheck").start();
		
		//服务启动时执行一次
		if(!subsribers.isEmpty()) {
			for(IMonitorDataSubscriber m : this.subsribers){
				subscriberChange((AbstractClientServiceProxy)m,IServiceListener.ADD);
			}
			//doSubmitCacheItems();
		}
		
	}
	
	@Override
	@SMethod(timeout=5000,retryCnt=0,needResponse=false,debugMode=0,
			monitorEnable=0,logLevel=MC.LOG_ERROR)
	public void submit(MRpcItem[] items) {
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
		
		if(openDebug) {
			log(items);
		}
		
		int pos = 0;
		while(pos < items.length) {
			IBasket<MRpcItem> b = basketFactory.borrowWriteBasket(true);
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
	
	public void subscriberChange(AbstractClientServiceProxy po,int opType) {
		IMonitorDataSubscriber mds = (IMonitorDataSubscriber)po;
		logger.info("subscriberChange");
		if(opType == IServiceListener.ADD) {
			synchronized(addMonitors) {
				this.addMonitors.add(mds);
			}
		}else if(opType == IServiceListener.REMOVE) {
			synchronized(deleteMonitors) {
				this.deleteMonitors.add(mds);
			}
		}
		
		synchronized(cacheItemsLock) {
			cacheItemsLock.notify();
		}
	}
	
	private void doCheck() {
		
		int sendInterval = 1000;
		
		while (true) {
			try {
				if (!addMonitors.isEmpty()) {
					synchronized (addMonitors) {
						for (Iterator<IMonitorDataSubscriber> ite = this.addMonitors.iterator(); ite.hasNext();) {
							IMonitorDataSubscriber m = ite.next();
							if (addOneMonitor(m)) {
								ite.remove();
							}
						}
					}
					// doSubmitCacheItems();
				}

				if (!deleteMonitors.isEmpty()) {
					synchronized (deleteMonitors) {
						for (Iterator<IMonitorDataSubscriber> ite = this.deleteMonitors.iterator(); ite.hasNext();) {
							IMonitorDataSubscriber m = ite.next();
							if (deleteOneMonitor(m)) {
								ite.remove();
							}
						}
					}
				}
				
				IBasket<MRpcItem> b = null;
				while((b = basketFactory.borrowReadSlot()) != null) {
					MRpcItem[] mis = new MRpcItem[b.remainding()];
					b.readAll(mis);
					sentItems.addAll(Arrays.asList(mis));
					if(!basketFactory.returnReadSlot(b, true)) {
						logger.error("doCheck Fail to return IBasket");
					}
				}
				
				if(monitoralbe) {
					sc.add(MC.Ms_CheckLoopCnt, 1);
				}
				
				if(sentItems.isEmpty()) {
					checkStatusMonitor();
					synchronized(cacheItemsLock) {
						cacheItemsLock.wait(2000);
					}
					continue;
				}
				
				long curTime = System.currentTimeMillis();
				Iterator<RegItem> ite = regSubs.iterator();
				while(ite.hasNext()) {
					RegItem ri = ite.next();
					
					for(MRpcItem mi : sentItems) {
						List<OneItem> ios = mi.getOneItems(ri.types);
						if(ios != null && !ios.isEmpty()) {
							MRpcItem mri = mi.copy();
							mri.setItems(ios);
							ri.cacheItems.add(mri);
						}
					}
					
					if(ri.isWorking || ri.cacheItems.isEmpty()) {
						continue;
					}
					
					if(curTime - ri.lastSendTime >= sendInterval) {
						if(monitoralbe) {
							sc.add(MC.Ms_CheckerSubmitItemCnt, ri.cacheItems.size());
						}
						ri.isWorking = true;
						ri.sendItems.addAll(ri.cacheItems);
						ri.cacheItems.clear();
						this.executor.submit(new Worker(ri));
						ri.lastSendTime = System.currentTimeMillis();
					}
				}
				
				if(!regSubs.isEmpty()) {
					sentItems.clear();
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
	
	private boolean addOneMonitor(IMonitorDataSubscriber m) {
		try {
			AbstractClientServiceProxy po = (AbstractClientServiceProxy)((Object)m);
			if(po.getItem() == null) {
				return false;
			}
			String skey = po.getItem().serviceKey();
			Set<Short> types = this.mtManager.intrest(skey);
			if(types != null && types.size() > 0) {
				RegItem ri = new RegItem();
				try {
					//订阅者和服务器部署同一个JVM，做本地调用，不使用RPC
					Class<?> cls = MonitorServerImpl.class.getClassLoader().loadClass(po.getItem().getImpl());
					if(cls != null && IMonitorDataSubscriber.class.isAssignableFrom(cls) && cls.isAnnotationPresent(Component.class)) {
						IMonitorDataSubscriber sub = (IMonitorDataSubscriber)of.get(cls);
						ri.sub = sub;
					}
				} catch (Throwable e) {
				}
				
				if(ri.sub == null) {
					ri.sub = m;
				}
				
				Short[] ts = new Short[types.size()];
				types.toArray(ts);
				ri.types = ts;
				this.regSubs.add(ri);
				return true;
			}
			return false;
		} catch (Throwable e) {
			logger.error("Call intrest got error:",e);
			return false;
		}
	}
	
	private boolean deleteOneMonitor(IMonitorDataSubscriber m) {
		for(Iterator<RegItem> ite = this.regSubs.iterator(); ite.hasNext(); ) {
			RegItem ri = ite.next();
			if(ri == m) {
				ite.remove();
				return true;
			}
		}
		return false;
	}
	
	private void checkStatusMonitor() {
		if(monitoralbe && (System.currentTimeMillis() - lastStatusTime > 300000)) {//5分钏没有状态请求
			logger.warn("ServiceCounter timeout 5 minutes, and stop it");
			statusAdapter.enableMonitor(false);
		}
	}
	
	private class Worker implements Runnable{
		
		public Worker(RegItem ri) {
			this.ri = ri;
		}
		
		private RegItem ri;
		
		@Override
		public void run() {
			try {
				if(ri.sendItems.isEmpty()){
					ri.isWorking = false;
					return;
				}
				//自身所有代码不加入日志统计，否则会进入死循环
				//如果有需要，可以选择其他方式，如slf4j等
				JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR, true);
				
				MRpcItem[] items = new MRpcItem[ri.sendItems.size()];
				ri.sendItems.toArray(items);
				ri.sub.onSubmit(items);
				if(monitoralbe) {
					sc.add(MC.Ms_TaskSuccessItemCnt, items.length);
				}
				ri.sendItems.clear();
			} catch (Throwable e) {
				logger.error("",e);
				if(monitoralbe) {
					sc.add(MC.Ms_TaskFailItemCnt, ri.sendItems.size());
				}
			} finally {
				ri.isWorking = false;
				
			}
		}
	}

	private class RegItem {
		public IMonitorDataSubscriber sub;
		public Short[] types = null;
		
		public List<MRpcItem> cacheItems = new ArrayList<>();
		
		public List<MRpcItem> sendItems = new ArrayList<>();
		
		public long lastSendTime = 0;
		public boolean isWorking = false;
	}
	
	public class MonitorServerStatusAdapter implements IMonitorAdapter{
		
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
			
			lastStatusTime = System.currentTimeMillis();
			
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
							sc = new ServiceCounter("MonitorServerImpl-Statis-"+Config.getInstanceName(),
									TYPES,60*3L,1,TimeUnit.SECONDS);
						}
					}
				}
				sc.start();
				monitoralbe = enable;
			} else if(!enable && monitoralbe) {
				if(sc != null) {
					sc.stop();
				}
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
	
	private void log(MRpcItem[] sis) {
		for(MRpcItem si : sis) {
			for(OneItem oi : si.getItems()) {
				StringBuffer sb = new StringBuffer();
				sb.append("GOT: " + MC.MONITOR_VAL_2_KEY.get(oi.getType()));
				if(si.getSm() != null) {
					sb.append(", SM: ").append(si.getSm().getKey().getMethod());
				}
				sb.append(", reqId: ").append(si.getReqId());
				logger.debug(sb.toString()); 
			}
		}
	}
	
}
