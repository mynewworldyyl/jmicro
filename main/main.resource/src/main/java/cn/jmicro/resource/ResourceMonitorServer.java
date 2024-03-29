package cn.jmicro.resource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.CfgMetadataJRso;
import cn.jmicro.api.IListener;
import cn.jmicro.api.JMicro;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.email.genclient.IEmailSenderJMSrv$JMAsyncClient;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.ResourceDataJRso;
import cn.jmicro.api.monitor.ResourceMonitorConfigJRso;
import cn.jmicro.api.monitor.StatisConfigJRso;
import cn.jmicro.api.monitor.genclient.IResourceServiceJMSrv$JMAsyncClient;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.RaftNodeDataListener;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class ResourceMonitorServer{

	private static final Logger logger = LoggerFactory.getLogger(ResourceMonitorServer.class);
	
	private String logDir;
	
	public static void main(String[] args) {
		/* RpcClassLoader cl = new RpcClassLoader(RpcClassLoader.class.getClassLoader());
		 Thread.currentThread().setContextClassLoader(cl);*/
		JMicro.getObjectFactoryAndStart(args);
		JMicro.waitForShutdown();
	}
	
	@Inject
	private ServiceInvokeManager invokeMng;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Reference
	private IEmailSenderJMSrv$JMAsyncClient mailSender;
	
	@Reference(namespace="*", version="*", type="ins",required=false,changeListener="resourceServiceChange")
	private Set<IResourceServiceJMSrv$JMAsyncClient> resourceServices = Collections.synchronizedSet(new HashSet<>());
	
	private Set<IResourceServiceJMSrv$JMAsyncClient> adds = new HashSet<>();
	
	private Set<IResourceServiceJMSrv$JMAsyncClient> dels = new HashSet<>();
	
	//private Map<String,Set<CfgMetadata>> resourceMetadatas = new ConcurrentHashMap<>();
	
	private Map<String,Reg> srv2Regs = new ConcurrentHashMap<>();
	
	private RaftNodeDataListener<ResourceMonitorConfigJRso> configListener = null;
	
	@Inject
	private IRegistry reg;
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ProcessInstanceManager insMng;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private PubSubManager pubsubMng;
	
	public void jready() {
		
		logDir = System.getProperty("user.dir")+"/logs/resLog/";
		File d = new File(logDir);
		if(!d.exists()) {
			d.mkdirs();
		}
		
		if(!op.exist(ResourceMonitorConfigJRso.RES_MONITOR_CONFIG_ROOT)) {
			op.createNodeOrSetData(ResourceMonitorConfigJRso.RES_MONITOR_CONFIG_ROOT, "", IDataOperator.PERSISTENT);
		}
		
		configListener = new RaftNodeDataListener<>(op,ResourceMonitorConfigJRso.RES_MONITOR_CONFIG_ROOT,
				ResourceMonitorConfigJRso.class,false);
		
		configListener.addListener((type,node,pi)->{
			notifyConfigEvent(type,Integer.parseInt(node),pi);
		});
		
		if(!resourceServices.isEmpty()) {
			this.adds.addAll(resourceServices);
		}
		
		/*insMng.addInstanceListner((type,pi)->{
			if(type == IListener.REMOVE) {
				return;
			}
			parseProcessMetadata(pi);
		});*/
		
		TimerTicker.doInBaseTicker(3, ResourceMonitorServer.class.getName(), null, (key,att)->{
			checkResourceServiceAdd();
			checkResourceServiceDelete();
			doWorker();
		});
	}

	/*
	private void parseProcessMetadata(ProcessInfo pi) {
		Set<CfgMetadata> cms = pi.getMetadatas();
		if(cms != null && !cms.isEmpty()) {
			for(CfgMetadata cm : cms) {
				if(!resourceMetadatas.containsKey(cm.getResName())) {
					resourceMetadatas.put(cm.getResName(), new HashSet<>());
				}
				resourceMetadatas.get(cm.getResName()).add(cm);
			}
		}
	}
	*/

	@SuppressWarnings("unchecked")
	private void doWorker() {
		if(this.srv2Regs.isEmpty()) {
			return;
		}
		
		Set<String> srvins = new HashSet<>();
		srvins.addAll(this.srv2Regs.keySet());
		
		for(String insName: srvins) {
			Reg r = srv2Regs.get(insName);
			if(r == null) {
				srv2Regs.remove(insName);
				continue;
			};
			
			if(r.configs.isEmpty() || r.running && TimeUtils.getCurTime() - r.lastCheckTime < 30000) continue;
			
			boolean con = false;
			long curTime = TimeUtils.getCurTime();
			for(ResourceMonitorConfigJRso cfg : r.configs.values()) {
				if(curTime - cfg.getLastNotifyTime() > cfg.getT()) {
					con = true;
					break;
				}
			}
			
			if(!con) {
				continue;
			}
			
			Set<String> resNames = new HashSet<>();
			Map<String,String> expMap = new HashMap<>();
			
			for(ResourceMonitorConfigJRso cfg : r.configs.values()) {
				resNames.add(cfg.getResName());
				if(StringUtils.isNotEmpty(cfg.getExpStr())) {
					expMap.put(cfg.getResName(), cfg.getExpStr());
				}
			}
			
			r.running = true;
			try {
				r.lastCheckTime = TimeUtils.getCurTime();
				r.resSrv.getResourceJMAsync(resNames,new HashMap<>(),expMap,r)
				.success((result,reg)->{
					Reg rr = (Reg)reg;
					notifyResourceMonitorResult(rr,(List<ResourceDataJRso>)result);
					r.lastCheckTime = TimeUtils.getCurTime();
					rr.running = false;
				}).fail((code,msg,reg)->{
					Reg rr = (Reg)reg;
					logger.error("code: " + code + ", msg: " + msg +",srv: "+ r.resSrv.getItem().getKey().fullStringKey());
					rr.running = false;
					rr.lastCheckTime = TimeUtils.getCurTime();
				});
			}catch(Throwable e) {
				logger.error("",e);
				LG.logWithNonRpcContext(MC.LOG_ERROR, this.getClass().getName(),"invoke getResourceJMAsync error", e,MC.MT_DEFAULT,true);
				r.lastCheckTime = TimeUtils.getCurTime();
			}
		}
	}

	private void notifyResourceMonitorResult(Reg rr, List<ResourceDataJRso> result) {
		
		Set<Integer> cids = new HashSet<>();
		cids.addAll(rr.configs.keySet());
		
		Map<String,ResourceDataJRso> rdsMap = new HashMap<>();
		for(ResourceDataJRso rd : result) {
			rdsMap.put(rd.getResName(), rd);
		}
		
		for(Integer cid : cids) {
			//每一个Config只对一种资源感兴趣
			ResourceMonitorConfigJRso cfg = rr.configs.get(cid);
			if(cfg == null) continue;
			
			ResourceDataJRso rd = rdsMap.get(cfg.getResName());
			if(rd == null) continue;
			
			if(TimeUtils.getCurTime() - cfg.getLastNotifyTime() < cfg.getT()) {
				//没达到一个通知周期
				continue;
			}
			
			if(cfg.getExp() != null) {
				rd = filterByExp(cfg.getExp(),rd);
			}
			
			if(rd == null) {
				continue;
			}
			
			rd.setCid(cfg.getId());
			
			cfg.setLastNotifyTime(TimeUtils.getCurTime());
			switch(cfg.getToType()) {
			case StatisConfigJRso.TO_TYPE_SERVICE_METHOD:
				this.invokeMng.call(cfg.getToSn(),cfg.getToNs(),cfg.getToVer(),
						cfg.getToMt(), IPromise.class, new Class[]{ResourceDataJRso.class}, new Object[]{rd})
				.fail((code,msg,cxt)->{
					logger.error("Notify fail: " + cfg.getToSn() +"##"+cfg.getToNs() +"##" + cfg.getToVer()+"##"+ cfg.getToMt());
				});
				break;
			case StatisConfigJRso.TO_TYPE_CONSOLE:
				logger.info(JsonUtils.getIns().toJson(rd));
				break;
			case StatisConfigJRso.TO_TYPE_MESSAGE:
				PSDataJRso pd = new PSDataJRso();
				pd.setData(rd);
				pd.setTopic(cfg.getToParams());
				pd.setPersist(false);
				pd.setId(idGenerator.getIntId(PSDataJRso.class));
				this.pubsubMng.publish(pd);
				break;
			case StatisConfigJRso.TO_TYPE_DB:
				rd.setTag(cfg.getExtParams());
				MongoCollection<Document> coll = mongoDb.getCollection(cfg.getToParams());
				coll.insertOne(Document.parse(JsonUtils.getIns().toJson(rd)));
				break;
			case StatisConfigJRso.TO_TYPE_MONITOR_LOG:
				rd.setTag(cfg.getToParams());
				LG.log(MC.LOG_INFO, cfg.getToParams(), JsonUtils.getIns().toJson(rd));
				break;
			case StatisConfigJRso.TO_TYPE_EMAIL:
				if(mailSender != null) {
					mailSender.sendJMAsync(cfg.getToParams(), cfg.getExtParams(),  
							JsonUtils.getIns().toJson(rd), cfg)
					.fail((code,msg,cxt)->{
						logger.error("code:" + code+", msg: " + msg);
					});
				} else {
					logger.error("Email sender not found!");
				}
				break;
			case StatisConfigJRso.TO_TYPE_FILE:
				try {
					cfg.getBw().write(JsonUtils.getIns().toJson(rd)+"\n");
				} catch (IOException e) {
					logger.error("",JsonUtils.getIns().toJson(rd));
				}
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private ResourceDataJRso filterByExp(Exp exp, ResourceDataJRso rd) {
		if(rd.getMetaData() == null || rd.getMetaData().isEmpty()) {
			return null;
		}
		
		if(rd.getMetaData().size() == 1) {
			Map.Entry<String, Object> vo = rd.getMetaData().entrySet().iterator().next();
			if(vo.getValue() instanceof Collection) {
				List<Map<String,Object>> rst = new ArrayList<>();
				
				Collection<Map<String,Object>> col = (Collection<Map<String,Object>>)vo.getValue();
				for(Map<String,Object> omap: col) {
					if(ExpUtils.compute(exp, omap, Boolean.class)) {
						rst.add(omap);
					}
				}
				
				if(!rst.isEmpty()) {
					ResourceDataJRso rd0 = new ResourceDataJRso();
					rd0.setBelongInsId(rd.getBelongInsId());
					rd0.setBelongInsName(rd0.getBelongInsName());
					rd0.setResName(rd.getResName());
					rd0.setHttpHost(rd.getHttpHost());
					rd0.setSocketHost(rd.getSocketHost());
					rd0.setBelongInsName(rd.getBelongInsName());
					rd0.setTime(rd.getTime());
					rd0.putData(vo.getKey(), rst);
					rd0.setOsName(rd.getOsName());
					rd0.setClientId(rd.getClientId());
					return rd0;
				}
				return null;
			}
		}
		return ExpUtils.compute(exp, rd.getMetaData(), Boolean.class) ? rd : null;
	}

	private void checkResourceServiceDelete() {
		if(dels.isEmpty()) {
			return;
		}
		
		synchronized(dels) {
			for(IResourceServiceJMSrv$JMAsyncClient srv : dels) {
				ServiceItemJRso si = srv.getItem();
				String insName = si.getKey().getInstanceName();
				if(this.srv2Regs.containsKey(insName)) {
					Reg r = this.srv2Regs.remove(insName);
					
					if(LG.isLoggable(MC.LOG_DEBUG)){
						LG.log(MC.LOG_DEBUG, ResourceMonitorServer.class, "Resource service remove for instance: " + insName);
					}
					
					/*for(Integer cid : r.configs.keySet()) {
						Set<String> s = this.config2Ins.get(cid);
						if(s != null) {
							s.remove(insName);
						}
					}*/
				}
			}
			dels.clear();
		}
	}


	private void checkResourceServiceAdd() {
		if(adds.isEmpty()) {
			return;
		}
		synchronized(adds) {
			for(IResourceServiceJMSrv$JMAsyncClient srv : adds) {
				String insName = srv.getItem().getKey().getInstanceName();
				if(this.srv2Regs.containsKey(insName)) {
					continue;
				}
				
				Reg r = new Reg(srv);
				
				this.configListener.forEachNode((cfg)->{
					if(cfg.isEnable()) {
						this.addConfig2Reg(cfg, r);
					}
				});
				
				if(LG.isLoggable(MC.LOG_DEBUG)){
					LG.log(MC.LOG_DEBUG, ResourceMonitorServer.class, "Resource service add for instance: " + insName);
				}
				
				this.srv2Regs.put(insName, r);
			}
			adds.clear();
		}
	}


	public void resourceServiceChange(AbstractClientServiceProxyHolder srv,int type) {
		IResourceServiceJMSrv$JMAsyncClient djm = (IResourceServiceJMSrv$JMAsyncClient)srv;
		if(type == IListener.ADD) {
			if(LG.isLoggable(MC.LOG_DEBUG)) {
				LG.log(MC.LOG_DEBUG, this.getClass(), "Resource service add: " + djm.getItem().getKey().getInstanceName());
			}
			adds.add(djm);
		}else if(type == IListener.REMOVE) {
			if(LG.isLoggable(MC.LOG_INFO)) {
				LG.log(MC.LOG_INFO, this.getClass(), "Resource service remove: " + djm.getItem().getKey().getInstanceName());
			}
			dels.add(djm);
		}
	}
	
	private void notifyConfigEvent(int type,int cid, ResourceMonitorConfigJRso cfg) {
		if(type == IListener.ADD) {
			if(cfg.isEnable()) {
				configAdd(cfg);
			}
		}else if(type == IListener.DATA_CHANGE) {
			Set<String> srvKeys = new HashSet<>();
			srvKeys.addAll(this.srv2Regs.keySet());
			for(String insName : srvKeys) {
				Reg r = this.srv2Regs.get(insName);
				if(r != null && r.configs.containsKey(cfg.getId())) {
					r.configs.remove(cfg.getId());
					if(!cfg.isEnable()) {
						continue;
					}
				}
				if(cfg.isEnable()) {
					this.addConfig2Reg(cfg, r);
				}
			}
		}else if(type == IListener.REMOVE) {
			configRemove(cid);
		}
	}
	
	private void configAdd(ResourceMonitorConfigJRso cfg) {
		Set<String> srvKeys = new HashSet<>();
		srvKeys.addAll(this.srv2Regs.keySet());
		for(String insName : srvKeys) {
			Reg r = this.srv2Regs.get(insName);
			if(r != null) {
				this.addConfig2Reg(cfg, r);
			}
		}
	}
	
	private void configRemove(Integer cid) {
		Set<String> srvKeys = new HashSet<>();
		srvKeys.addAll(this.srv2Regs.keySet());
		for(String insName : srvKeys) {
			Reg r = this.srv2Regs.get(insName);
			if(r != null && r.configs.containsKey(cid)) {
				ResourceMonitorConfigJRso cfg = r.configs.remove(cid);
				if(cfg != null && cfg.getBw() != null) {
					try {
						cfg.getBw().close();
					} catch (IOException e) {
						logger.error("",e);
					}
				}
			}
		}
	}
	
	private void addConfig2Reg(ResourceMonitorConfigJRso cfg, Reg r) {
		
		String macherInsName = cfg.getMonitorInsName();
		if(macherInsName != null && "*".equals(macherInsName)) {
			macherInsName = null;
		}else if(macherInsName != null && macherInsName.endsWith("*")) {
			macherInsName = macherInsName.substring(0,macherInsName.length()-1);
		}
		
		ServiceItemJRso si = r.resSrv.getItem();
		String insName = si.getKey().getInstanceName();
		if(StringUtils.isNotEmpty(macherInsName)) {
			if(!insName.startsWith(macherInsName)) {
				return;
			}
		}
		
		String resName = cfg.getResName();
		ProcessInfoJRso pi = this.insMng.getProcessByName(insName);
		
		boolean f = false;
		Set<CfgMetadataJRso> metadata = pi.getMetadatas(resName);
		if(metadata != null) {
			for(CfgMetadataJRso cm : metadata) {
				if(cm.getResName().equals(resName)) {
					f = true;
				}
			}
		}
		
		
		if(f) {
			/*if(!config2Ins.containsKey(cfg.getId())) {
				config2Ins.put(cfg.getId(), new HashSet<>());
			}
			config2Ins.get(cfg.getId()).add(insName);*/
			reparseConfig(cfg);
			
			r.configs.put(cfg.getId(), cfg);
		}
	}

	private void reparseConfig(ResourceMonitorConfigJRso cfg) {
		if(StatisConfigJRso.TO_TYPE_SERVICE_METHOD == cfg.getToType()) {
			 String[] ps =  cfg.getToParams().split(UniqueServiceKeyJRso.SEP);
			 cfg.setToSn(ps[UniqueServiceKeyJRso.INDEX_SN]);
			 cfg.setToNs(ps[UniqueServiceKeyJRso.INDEX_NS]);
			 cfg.setToVer(ps[UniqueServiceKeyJRso.INDEX_VER]);
			 cfg.setToMt(ps[UniqueServiceKeyJRso.INDEX_METHOD]);
		}
		
		if(!StringUtils.isEmpty(cfg.getExpStr())) {
			List<String> suffix = ExpUtils.toSuffix(cfg.getExpStr());
			if(ExpUtils.isValid(suffix)) {
				Exp ex = new Exp();
				ex.setOriEx(cfg.getExpStr());
				ex.setSuffix(suffix);
				cfg.setExp(ex);
			} else {
				logger.error("Invalid exp:" + cfg.getExpStr()+" for cfg: " + cfg.getId());
			}
		}
		
		if(StatisConfigJRso.TO_TYPE_FILE == cfg.getToType()) {
			File logFile = new File(this.logDir + cfg.getId() + "_" + cfg.getToParams());
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					String msg ="Create log file fail";
					logger.error(msg, e);
					LG.logWithNonRpcContext(MC.LOG_ERROR, this.getClass().getName(), msg, e,MC.MT_DEFAULT,true);
				}
			}

			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
				cfg.setBw(bw);
			} catch (FileNotFoundException e) {
				String msg ="Create writer fail";
				logger.error(msg, e);
				LG.logWithNonRpcContext(MC.LOG_ERROR, this.getClass().getName(), msg, e,MC.MT_DEFAULT,true);
			}
		}
		
		if(StatisConfigJRso.TO_TYPE_DB == cfg.getToType()) {
			if(StringUtils.isEmpty(cfg.getToParams())) {
				cfg.setToParams(ResourceMonitorConfigJRso.DEFAULT_RESOURCE_TABLE_NAME);
			}else if(!cfg.getToParams().startsWith(ResourceMonitorConfigJRso.DEFAULT_RESOURCE_TABLE_PREFIX)){
				cfg.setToParams(ResourceMonitorConfigJRso.DEFAULT_RESOURCE_TABLE_PREFIX + cfg.getToParams());
			}
		}
		
	}
	
	public Set<CfgMetadataJRso> getResMetadata(String resName) {
		Set<CfgMetadataJRso> ms = new HashSet<>();
		this.insMng.forEach((pi)->{
			if(pi.getMetadatas(resName) != null) {
				ms.addAll(pi.getMetadatas(resName)) ;
			}
		});
		
		return ms;
	}
	
	public Map<String,Map<String,Set<CfgMetadataJRso>>> getInstanceResourceList() {
		Map<String,Map<String,Set<CfgMetadataJRso>>> ms = new HashMap<>();
		this.insMng.forEach((pi)->{
			Map<String,Set<CfgMetadataJRso>> maps = new HashMap<String,Set<CfgMetadataJRso>>();
			ms.put(pi.getInstanceName(), maps);
			maps.putAll(pi.getMetadatas());
		});
		return ms;
	}

	private class Reg{
		
		private Reg(IResourceServiceJMSrv$JMAsyncClient resSrv) {
			this.resSrv = resSrv;
		}
		
		private IResourceServiceJMSrv$JMAsyncClient resSrv;
		private Map<Integer,ResourceMonitorConfigJRso> configs = new HashMap<>();
		
		private long lastCheckTime = 0;
		private boolean running = false;
		
	}

	public IPromise<RespJRso<Map<String, Set<ResourceDataJRso>>>> getInstanceResourceData(String insName) {
		
		RespJRso<Map<String,Set<ResourceDataJRso>>> r = new RespJRso<>();
		//Map<String,Set<ResourceData>> resDatas = new HashMap<>();
		Promise<RespJRso<Map<String, Set<ResourceDataJRso>>>> p = new Promise<>();
		p.setResult(r);
		
		Reg reg = srv2Regs.get(insName);
		if(reg == null) {
			r.setMsg("Instance [" + insName + "] offline!");
			r.setCode(RespJRso.CODE_FAIL);
			p.done();
			return p;
		}
		
		if(reg.configs == null || reg.configs.isEmpty()) {
			r.setMsg("Instance [" + insName + "] support on resources!");
			r.setCode(RespJRso.CODE_FAIL);
			p.done();
			return p;
		}
		
		
		ProcessInfoJRso pi = this.insMng.getProcessByName(insName);
		if(pi == null) {
			r.setMsg("Instance [" + insName + "] offline!");
			r.setCode(RespJRso.CODE_FAIL);
			p.done();
			return p;
		}
		
		Set<String> resNames = new HashSet<>();
		resNames.addAll(pi.getMetadatas().keySet());
		
		Map<String,String> expMap = new HashMap<>();
		
		try {
			reg.resSrv.getResourceJMAsync(resNames,new HashMap<>(),expMap,reg)
			.success((result,reg0)->{
				//Reg rr = (Reg)reg0;
				r.setData((Map<String,Set<ResourceDataJRso>>)result);
				p.done();
				
			}).fail((code,msg,reg0)->{
				Reg rr = (Reg)reg0;
				String desc = "code: " + code + ", msg: " + msg +",srv: "+ rr.resSrv.getItem().getKey().fullStringKey();
				logger.error(desc);
				LG.log(MC.LOG_ERROR, this.getClass(), desc);
				r.setMsg("Instance [" + insName + "] offline!");
				r.setCode(RespJRso.CODE_FAIL);
				p.done();
			});
		}catch(Throwable e) {
			logger.error("",e);
		}
		return p;
	}

	@SuppressWarnings("unchecked")
	public IPromise<RespJRso<Map<String, List<ResourceDataJRso>>>> getDirectResourceData(ResourceDataReqJRso req) {
		
		Promise<RespJRso<Map<String, List<ResourceDataJRso>>>> p = new Promise<>();
		RespJRso<Map<String,List<ResourceDataJRso>>> r = new RespJRso<>();
		p.setResult(r);
		
		Set<String> resNames = new HashSet<>();
		if(req.getResNames() != null && req.getResNames().length > 0) {
			resNames.addAll(Arrays.asList(req.getResNames()));
		}
		
		if(req.getInsNames() != null && req.getInsNames().length > 0) {
			//查询指定实例上的资源
			p.setCounter(req.getInsNames().length);
			Map<String,List<ResourceDataJRso>> resMap = new HashMap<>();
			r.setData(resMap);
			for(String insName : req.getInsNames()) {
				final Reg reg = this.srv2Regs.get(insName);
				if(reg == null) {resMap.put(insName, Collections.EMPTY_LIST);p.decCounter(1, true);continue;}
				reg.resSrv.getResourceJMAsync(resNames,new HashMap<>(),null)
				.success((result,rrr)->{
					resMap.put(insName, result);
					p.decCounter(1, true);
				}).fail((code,msg,rrr)->{
					String errMsg = "code: " + code + ", msg: " + msg +",srv: "+ reg.resSrv.getItem().getKey().fullStringKey();
					logger.error(errMsg);
					p.setFail(code, msg);
					p.decCounter(1, true);
					LG.log(MC.LOG_ERROR, ResourceMonitorServer.class, errMsg);
				});
			}
		} else {
			Map<String,List<ResourceDataJRso>> resMap = new HashMap<>();
			r.setData(resMap);
			//查询全部实例上的资源
			p.setCounter(this.srv2Regs.size());
			for(Map.Entry<String, Reg> reg : this.srv2Regs.entrySet()) {
				if(LG.isLoggable(MC.LOG_DEBUG)){
					LG.log(MC.LOG_DEBUG, ResourceMonitorServer.class, reg.getKey());
				}
				reg.getValue().resSrv.getResourceJMAsync(resNames,new HashMap<>(),null,reg)
				.success((result,cxt)->{
					Map.Entry<String, Reg> rrr = (Map.Entry<String, Reg>)cxt;
					resMap.put(rrr.getKey(), result);
					p.decCounter(1, true);
				}).fail((code,msg,cxt)->{
					Map.Entry<String, Reg> rrr = (Map.Entry<String, Reg>)cxt;
					String errMsg = "code: " + code + ", msg: " + msg +",srv: "+ 
					rrr.getValue().resSrv.getItem().getKey().fullStringKey();
					logger.error(errMsg);
					LG.log(MC.LOG_ERROR, this.getClass(), errMsg);
					//p.setFail(code, msg);
					p.decCounter(1, true);
				});
			}
		}
		
		return p;
	}
	
}
