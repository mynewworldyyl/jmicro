package cn.jmicro.api.tx.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Holder;
import cn.jmicro.api.IListener;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.mng.ProcessInstanceManager;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.tx.ITransactionResourceJMSrv;
import cn.jmicro.api.tx.ITransationServiceJMSrv;
import cn.jmicro.api.tx.TxConfigJRso;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.api.tx.TxInfoJRso;
import cn.jmicro.api.tx.genclient.ITransactionResourceJMSrv$JMAsyncClient;
import cn.jmicro.api.utils.TimeUtils;

@Component
@Service(version="0.0.1",external=false,timeout=10000,debugMode=1,showFront=true,
clientId=-1,logLevel=MC.LOG_DEBUG)
public class TransationServiceImpl implements ITransationServiceJMSrv{

	private static final Class<?> TAG = TransationServiceImpl.class;
	
	private final static Logger logger = LoggerFactory.getLogger(TransationServiceImpl.class);
	
	@Reference(namespace="*", version="*", type="ins",required=false,changeListener="resourceServiceChangeListener")
	private Set<ITransactionResourceJMSrv$JMAsyncClient> resourceServices = Collections.synchronizedSet(new HashSet<>());
	
	private Map<Integer,ITransactionResourceJMSrv$JMAsyncClient> rsMap = Collections.synchronizedMap(new HashMap<>());
	
	//private Object resLocker = new Object();
	
	private Map<Long,TxGroup> txGroups = new HashMap<>();
	
	private Set<Long> finishTxids = new HashSet<>();
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ProcessInstanceManager insMng;
	
	@Inject
	private ProcessInfoJRso pi;
	
	private Object syno = new Object();
	
	public void jready() {
		new Thread(this::check).start();
		if(!resourceServices.isEmpty()) {
			for(ITransactionResourceJMSrv$JMAsyncClient r : this.resourceServices) {
				rsMap.put(r.getItem().getInsId(), r);
			}
		}
	}
	
	public void resourceServiceChangeListener(AbstractClientServiceProxyHolder po,int opType) {
		if(opType == IListener.ADD) {
			rsMap.put(po.getInsId(), (ITransactionResourceJMSrv$JMAsyncClient)po);
		}else if(opType == IListener.REMOVE) {
			rsMap.remove(po.getInsId());
		}
	}
	
	private void check() {
		
		Set<Long> keys = new HashSet<>();
		Set<Long> txids = new HashSet<>();
		while(true) {
			try {
				
				if(!finishTxids.isEmpty()) {
					synchronized(finishTxids) {
						txids.addAll(finishTxids);
						finishTxids.clear();
					}
					
					for(Long txid: txids) {
						TxGroup g = txGroups.remove(txid);
						synchronized(g) {
							finishOneGroup(g);
						}
					}
					
					txids.clear();
				}
				
				if(txGroups.size() > 0) {
					synchronized(txGroups) {
						keys.addAll(txGroups.keySet());
					}
					
					for(Long txid: keys) {
						TxGroup g = txGroups.get(txid);
						if(g == null || g.cfg.getTimeout() <= 0 ) {
							continue;
						}
						
						synchronized(g) {
							if(TimeUtils.getCurTime() - g.startedTime < g.cfg.getTimeout()) {
								continue;
							}
							txGroups.remove(txid);
							
							LG.log(MC.LOG_ERROR, TAG, "Transactin "+txid+" timeout with "+
							(TimeUtils.getCurTime() - g.startedTime)+" cfg timeout: " + g.cfg.getTimeout());

							finishOneGroup(g);
						}
					}
					keys.clear();
				}
				
				synchronized(syno) {
					try {
						syno.wait(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				
			}catch(Throwable e) {
				LG.log(MC.LOG_ERROR, TAG, "check error",e);
			}
		}
	}
	
	private void finishOneGroup(TxGroup g) {
		boolean commit = true;
		for(TxVoter v : g.voters.values()){
			if(!rsMap.containsKey(v.pid)) {
				//只要有一个参与者没在线，事务需要回滚
				LG.log(MC.LOG_ERROR, TAG, "Rollback by resource client  "+ v.pid + " not found for txid: " + g.txId+",insName: " + v.insName);
				commit = false;
				break;
			}
			
			if(v.status != TxGroup.STATUS_COMMITED) {
				commit = false;
				break;
			}
		}
		
		Holder<Boolean> suc = new Holder<>(commit);
		if(commit) {

			CountDownLatch cd = new CountDownLatch(g.voters.values().size());
			for(TxVoter v : g.voters.values()) {
				if(v.txPhase == TxConstants.TX_2PC) {
					cd.countDown();
					continue;
				}
				ITransactionResourceJMSrv$JMAsyncClient client = this.rsMap.get(v.pid);
				if(client != null) {
					client.canCommitJMAsync(g.txId)
					.success((rst,cxt)->{
						Boolean s = (Boolean)rst.getData();
						if(rst.getCode() != 0 || !s.booleanValue()) {
							suc.set(false);
						}
						cd.countDown();
					})
					.fail((code,msg,cxt)->{
						suc.set(false);
						cd.countDown();
						LG.log(MC.LOG_ERROR, TAG, "Fail to check commit status txid:" + g.txId +", commit: false,insName: "+v.insName+",code:"+ code +",msg:"+msg);
					});
				} else {
					suc.set(false);
					cd.countDown();
					if(LG.isLoggable(MC.LOG_WARN, null)) {
						LG.log(MC.LOG_WARN, TAG, "Transaction client not found when check commit status: " +v.pid+",insName: "+v.insName);
					}
					break;
				}
			}
		}
		
		final boolean succ = suc.get();
		
		for(TxVoter v : g.voters.values()) {
			ITransactionResourceJMSrv$JMAsyncClient client = this.rsMap.get(v.pid);
			if(client != null) {
				if(LG.isLoggable(MC.LOG_DEBUG, null)) {
					LG.log(MC.LOG_DEBUG, TAG, "Notify transaction "+g.txId+" client pid:" +v.pid+",commit: " + succ+",insName: "+v.insName);
				}
				client.finishJMAsync(g.txId,succ)
				.success((rst,cxt)->{
					if(LG.isLoggable(MC.LOG_INFO, null)) {
						LG.log(MC.LOG_INFO, TAG, "Commit success "+g.txId+" client pid:" +v.pid+",commit: " + succ+",insName: "+v.insName);
					}
				}).fail((code,msg,cxt)->{
					LG.log(MC.LOG_ERROR, ITransactionResourceJMSrv.STR_TAG, "fail to commit txid:" + g.txId +", commit: "+ succ+",insName: "+v.insName+",code:"+ code +",insId:"+v.pid+",msg:"+msg);
				});
			} else {
				LG.log(MC.LOG_ERROR, ITransactionResourceJMSrv.STR_TAG, "Client not found txid:" + g.txId +", commit: "+ succ+",insName: "+v.insName +",insId:"+v.pid);
			}
		}
	}

	@Override
	@SMethod(retryCnt=0,timeout=3000)
	public RespJRso<TxInfoJRso> start(TxConfigJRso cfg) {
		Long txid = idGenerator.getLongId(TxConfigJRso.class);
		RespJRso<TxInfoJRso> r = new RespJRso<>(RespJRso.CODE_TX_FAIL,"");
		
		if(txid <= 0) {
			ProcessInfoJRso pi = this.insMng.getInstanceById(cfg.getPid());
			r.setMsg("create txid failure!");
			LG.log(MC.LOG_ERROR, TAG, r.getMsg()+",by insName: " + pi.getInstanceName());
			return r;
		}
		
		if(LG.isLoggable(MC.LOG_DEBUG, null)) {
			ProcessInfoJRso pi = this.insMng.getInstanceById(cfg.getPid());
			LG.log(MC.LOG_DEBUG, TAG, "Start transaction: " +txid + ",by insName: " + pi.getInstanceName());
		}
		
		TxGroup g = new TxGroup(cfg,txid);
		synchronized(txGroups) {
			txGroups.put(txid, g);
		}
		
		TxInfoJRso ti = new TxInfoJRso();
		ti.setServerId(pi.getId());
		ti.setTxid(txid);
		r.setData(ti);
		r.setCode(RespJRso.CODE_SUCCESS);
		
		return r;
	}

	@Override
	@SMethod(retryCnt=3,timeout=3000)
	public RespJRso<Boolean> takePartIn(int pid, long txid,byte txPhase) {
		
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL,false);
		
		ProcessInfoJRso pi = this.insMng.getInstanceById(pid);
		
		TxGroup g = txGroups.get(txid);
		if(g == null) {
			r.setMsg("Transaction "+ txid +" not found for pid: " + pid+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		if(!rsMap.containsKey(pid)) {
			r.setMsg("Resource client "+ pid +" not found for txid: " + txid+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		if(g.status != TxGroup.STATUS_ON_GOING) {
			r.setMsg("Pid: "+pid+" Transaction "+txid+" status is not on going with status: " + g.status+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG, TAG, "Resource client "+ pid +" take part in txid: " + txid+",insName:" + pi.getInstanceName());
		}
		
		TxVoter v = new TxVoter(pid);
		v.insName = pi.getInstanceName();
		v.status = TxGroup.STATUS_ON_GOING;
		v.txPhase = txPhase;
		g.addVoter(v);
		
		r.setCode(RespJRso.CODE_SUCCESS);
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(retryCnt=0,timeout=3000)
	public RespJRso<Boolean> vote(int pid, long txid, boolean commit) {
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL,false);
		ProcessInfoJRso pi = this.insMng.getInstanceById(pid);
		
		TxGroup g = txGroups.get(txid);
		if(g == null) {
			r.setMsg("Transaction"+ txid +" not found for pid: " + pid+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		if(!rsMap.containsKey(pid)) {
			r.setMsg("Resource client "+ pid +" not found for txid: " + txid+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		if(g.status != TxGroup.STATUS_ON_GOING) {
			r.setMsg("Pid: "+pid+" Transaction "+txid+" status is not on going with status: " + g.status+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		TxVoter v = g.getVoter(pid);
		if(v == null) {
			r.setMsg("Transaction "+txid+" not found voter pid: " + pid+",insName:" + pi.getInstanceName());
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return r;
		}
		
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG, TAG, "Resource client "+ pid +" vote txid: " + txid+" with: " + commit+",insName:" + pi.getInstanceName());
		}
		
		v.status = commit ? TxGroup.STATUS_COMMITED : TxGroup.STATUS_ROLLBACK;
		
		boolean fi = true;
		synchronized(g) {
			for(TxVoter tv : g.voters.values()) {
				if(tv.status == TxGroup.STATUS_ON_GOING) {
					//没有全部投票完成
					fi = false;
					break;
				}
			}
		}
		
		if(fi) {
			
			//全部投票完成
			synchronized(finishTxids) {
				finishTxids.add(txid);
			}
			
			synchronized(syno) {
				syno.notify();
			}
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		r.setData(true);
		
		return r;
	}
	
	private class TxGroup{
		
		private static final byte STATUS_ON_GOING = 1;
		private static final byte STATUS_COMMITED = 2;
		private static final byte STATUS_ROLLBACK = 3;
		
		private byte status = STATUS_ON_GOING;
		
		private long txId;
		
		private TxConfigJRso cfg;
		
		private long startedTime;
		
		private Map<Integer,TxVoter> voters = new HashMap<>();
		
		private TxGroup(TxConfigJRso cfg,long txId) {
			this.cfg = cfg;
			this.txId = txId;
			this.startedTime = TimeUtils.getCurTime();
		}
		
		private void addVoter(TxVoter v) {
			voters.put(v.pid, v);
		}
		
		private TxVoter getVoter(int pid) {
			return voters.get(pid);
		}
		
	}
	
	private class TxVoter{
		private int pid;
		private byte txPhase = TxConstants.TX_2PC;
		private String insName;
		private byte status = TxGroup.STATUS_ON_GOING;
		
		//private ITransactionResource$JMAsyncClient resClientSrv;
		
		private TxVoter(int pid) {
			this.pid = pid;
		}
	}
}
