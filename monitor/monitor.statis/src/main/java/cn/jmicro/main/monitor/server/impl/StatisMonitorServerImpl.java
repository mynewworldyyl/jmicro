package cn.jmicro.main.monitor.server.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
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
import cn.jmicro.api.monitor.IStatisMonitorServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcStatisItem;
import cn.jmicro.api.monitor.MonitorAndService2TypeRelationshipManager;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.monitor.MonitorStatisConfigManager;
import cn.jmicro.api.monitor.ServiceCounter;
import cn.jmicro.api.monitor.StatisItem;
import cn.jmicro.api.monitor.genclient.IMonitorDataSubscriber$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.registry.IServiceListener;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.service.ServiceLoader;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;

@Component
@Service(namespace="monitorServer", version="0.0.1", debugMode=0,
monitorEnable=0, logLevel=MC.LOG_WARN, retryCnt=0)
public class StatisMonitorServerImpl implements IStatisMonitorServer {

	private final static Logger logger = LoggerFactory.getLogger(StatisMonitorServerImpl.class);
	
	private static final String GROUP = "monitorServer";
	
	//@Cfg(value="/MonitorServerImpl/monitoralbe", changeListener = "")
	private boolean monitoralbe = false;
	
	@Reference(namespace="*", version="*", type="ins",required=false,changeListener="subscriberChange")
	private Set<IMonitorDataSubscriber$JMAsyncClient> subsribers = new HashSet<>();
	
	@Inject
	private MonitorStatisConfigManager mcm;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MonitorAndService2TypeRelationshipManager mtManager;
	
	private ServiceCounter sc = null;
	
	private Set<RegItem> regSubs = new HashSet<>();
	
	//private Queue<MRpcItem> cacheItems = new ConcurrentLinkedQueue<>();
	
	private BasketFactory<MRpcStatisItem> basketFactory = null;
	
	private Set<IMonitorDataSubscriber> deleteMonitors = new HashSet<>();
	
	private Set<IMonitorDataSubscriber> addMonitors = new HashSet<>();
	
	private Set<MRpcStatisItem> sentItems = new HashSet<>();
	
	private Object cacheItemsLock = new Object();
	
	private ExecutorService executor = null;
	
	private long lastStatusTime = TimeUtils.getCurTime();
	
	private MonitorServerStatusAdapter statusAdapter;
	
	//@Cfg(value="/MonitorServerImpl/openDebug")
	private boolean openDebug = false;
	
	public void init() {
	}
	
	@JMethod("ready")
	public void ready() {
		
		ExecutorConfig config = new ExecutorConfig();
		config.setMsMaxSize(60);
		config.setTaskQueueSize(500);
		config.setThreadNamePrefix("StatisMonitorServer");
		
		executor = of.get(ExecutorFactory.class).createExecutor(config);
		basketFactory = new BasketFactory<MRpcStatisItem>(1000,10);
		
		statusAdapter = new MonitorServerStatusAdapter();
		//statusAdapter.init();
		of.regist("statisMonitorServerStatusAdapter", statusAdapter);
		
		ServiceLoader sl = of.get(ServiceLoader.class);
		ServiceItem si = sl.createSrvItem(IMonitorAdapter.class, 
				Config.getInstanceName()+"."+MonitorServerStatusAdapter.class.getName(), "0.0.1", null);
		sl.registService(si,statusAdapter);
		
		new Thread(this::doCheck,Config.getInstanceName()+"_MonitorServer_doCheck").start();
		
		//服务启动时执行一次
		if(!subsribers.isEmpty()) {
			for(IMonitorDataSubscriber m : this.subsribers){
				subscriberChange((AbstractClientServiceProxyHolder)m,IServiceListener.ADD);
			}
			//doSubmitCacheItems();
		}
		
	}
	
	@Override
	@SMethod(timeout=5000,retryCnt=0,needResponse=false,debugMode=0,monitorEnable=0,logLevel=MC.LOG_ERROR
			,maxPacketSize=32768)
	public void submit(MRpcStatisItem[] items) {
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
			log(items);
		}*/
		
		int pos = 0;
		while(pos < items.length) {
			IBasket<MRpcStatisItem> b = basketFactory.borrowWriteBasket(true);
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
					} else {
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
			} else {
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
	
	public void subscriberChange(AbstractClientServiceProxyHolder po,int opType) {
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
				if(!addMonitors.isEmpty()) {
					synchronized (addMonitors) {
						for (Iterator<IMonitorDataSubscriber> ite = this.addMonitors.iterator(); ite.hasNext();) {
							IMonitorDataSubscriber m = ite.next();
							if (addOneMonitor(m)) {
								ite.remove();
							}
						}
					}
					//doSubmitCacheItems();
				}

				if(!deleteMonitors.isEmpty()) {
					synchronized (deleteMonitors) {
						for (Iterator<IMonitorDataSubscriber> ite = this.deleteMonitors.iterator(); ite.hasNext();) {
							IMonitorDataSubscriber m = ite.next();
							if (deleteOneMonitor(m)) {
								ite.remove();
							}
						}
					}
				}
				
				IBasket<MRpcStatisItem> b = null;
				while((b = basketFactory.borrowReadSlot()) != null) {
					MRpcStatisItem[] mis = new MRpcStatisItem[b.remainding()];
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
					//无数据可发送
					checkStatusMonitor();
					synchronized(cacheItemsLock) {
						cacheItemsLock.wait(2000);
					}
					continue;
				}
				
				long curTime = TimeUtils.getCurTime();
				Iterator<RegItem> ite = regSubs.iterator();
				while(ite.hasNext()) {
					RegItem ri = ite.next();
					
					for(MRpcStatisItem mi : sentItems) {
						Map<Short,StatisItem> ios = mi.getOneItems(ri.types);
						if(ios != null && !ios.isEmpty()) {
							MRpcStatisItem mri = mi.copy();
							mri.setTypeStatis(ios);
							ri.cacheItems.add(mri);
						}
					}
					
					if(ri.isWorking || ri.cacheItems.isEmpty()) {
						//对应的订阅者正在接收数据 或 无数据可发送
						continue;
					}
					
					if(curTime - ri.lastSendTime >= sendInterval) {
						if(monitoralbe) {
							sc.add(MC.Ms_CheckerSubmitItemCnt, ri.cacheItems.size());
						}
						ri.isWorking = true;
						//将全部数据放到订阅者的发送队列
						ri.sendItems.addAll(ri.cacheItems);
						ri.cacheItems.clear();
						executor.submit(new Worker(ri));
						ri.lastSendTime = TimeUtils.getCurTime();
					}
				}
				
				//如果没有订阅者，数据也将丢失，后面上线的订阅者收不到前面已经发送的消息
				sentItems.clear();
				
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
			AbstractClientServiceProxyHolder po = (AbstractClientServiceProxyHolder)((Object)m);
			if(po.getHolder().getItem() == null) {
				return false;
			}
			String skey = po.getHolder().getItem().serviceKey();
			Set<Short> types = this.mtManager.intrest(skey);
			if(types != null && types.size() > 0) {
				RegItem ri = new RegItem();

				//订阅者和服务器部署同一个JVM，做本地调用，不使用RPC
				Class<?> cls = null;
				try {
					cls = StatisMonitorServerImpl.class.getClassLoader().loadClass(po.getHolder().getItem().getImpl());
				} catch (Throwable e) {
				}
				
				if(cls != null && IMonitorDataSubscriber.class.isAssignableFrom(cls) && cls.isAnnotationPresent(Component.class)) {
					IMonitorDataSubscriber sub = (IMonitorDataSubscriber)of.get(cls);
					ri.localSub = sub;
				} else {
					ri.sub = (IMonitorDataSubscriber$JMAsyncClient)m;
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
			if(ri.sub == m) {
				ite.remove();
				return true;
			}
		}
		return false;
	}
	
	private void checkStatusMonitor() {
		if(monitoralbe && (TimeUtils.getCurTime() - lastStatusTime > 300000)) {//5分钏没有状态请求
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
				JMicroContext.get().setBoolean(JMicroContext.IS_MONITORENABLE, false);
				JMicroContext.get().setBoolean(Constants.FROM_MONITOR, true);
				
				MRpcStatisItem[] items = new MRpcStatisItem[ri.sendItems.size()];
				ri.sendItems.toArray(items);
				
				if(openDebug) {
					logger.debug("Submit items: " + items.length);
					log(items);
				}
				
				if(ri.localSub != null) {
					//本地分发
					ri.localSub.onSubmit(items);
				} else {
					//远程分发
					ri.sub.onSubmitJMAsync(items)
					.then((rst,fail,ctx)->{
						if(fail != null) {
							logger.error("submit error: " + fail.toString());
						}
					});
				}
				
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
		public IMonitorDataSubscriber$JMAsyncClient sub;
		
		public IMonitorDataSubscriber localSub;
		
		public Short[] types = null;
		
		public List<MRpcStatisItem> cacheItems = new ArrayList<>();
		
		public List<MRpcStatisItem> sendItems = new ArrayList<>();
		
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
	
	private void log(MRpcStatisItem[] sis) {
		for(MRpcStatisItem si : sis) {
			for(StatisItem oi : si.getTypeStatis().values()) {
				StringBuffer sb = new StringBuffer();
				sb.append("GOT: " + MC.MONITOR_VAL_2_KEY.get(oi.getType()));
				if(si.getSm() != null) {
					sb.append(", SM: ").append(si.getSm().getKey().getMethod());
				}
				sb.append(", actName: ").append(si.getActName());
				logger.debug(sb.toString()); 
			}
		}
	}
	
}
