package cn.jmicro.resource.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Holder;
import cn.jmicro.api.IListener;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.tx.ITransationService;
import cn.jmicro.api.tx.TxConfig;
import cn.jmicro.api.tx.genclient.ITransactionResource$JMAsyncClient;
import cn.jmicro.api.utils.TimeUtils;

@Component
@Service(version="0.0.1",external=false,timeout=10000,debugMode=1,showFront=true,clientId=-1)
public class TransationServiceImpl implements ITransationService{

	private final static Logger logger = LoggerFactory.getLogger(TransationServiceImpl.class);
	
	@Reference(namespace="*", version="*", type="ins",required=false,changeListener="resourceServiceChangeListener")
	private Set<ITransactionResource$JMAsyncClient> resourceServices = Collections.synchronizedSet(new HashSet<>());
	
	private Map<Integer,ITransactionResource$JMAsyncClient> rsMap = Collections.synchronizedMap(new HashMap<>());
	
	//private Object resLocker = new Object();
	
	private Map<Long,TxGroup> txGroups = Collections.synchronizedMap(new HashMap<>());
	
	@Inject
	private ComponentIdServer idGenerator;
	
	public void ready() {
		new Thread(this::check).start();
		if(!resourceServices.isEmpty()) {
			for(ITransactionResource$JMAsyncClient r : this.resourceServices) {
				rsMap.put(r.getItem().getInsId(), r);
			}
		}
	}
	
	public void resourceServiceChangeListener(AbstractClientServiceProxyHolder po,int opType) {
		if(opType == IListener.ADD) {
			rsMap.put(po.getItem().getInsId(), (ITransactionResource$JMAsyncClient)po);
		}else if(opType == IListener.REMOVE) {
			rsMap.remove(po.getItem().getInsId());
		}
	}
	
	private void check() {
		Object syno = new Object();
		Set<Long> keys = new HashSet<>();
		for(;true;) {
			try {
				if(txGroups.size() > 0) {
					keys.addAll(txGroups.keySet());
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
							
							LG.log(MC.LOG_ERROR, this.getClass(), "Transactin "+txid+" timeout with "+
							(TimeUtils.getCurTime() - g.startedTime)+" cfg timeout: " + g.cfg.getTimeout());
							
							if(!endTx(g,false)) {
								LG.log(MC.LOG_ERROR, this.getClass(), "Fail to finish tx: " +txid);
							}
						}
						
					}
				}
			}catch(Throwable e) {
				LG.log(MC.LOG_ERROR, this.getClass(), "check error",e);
			}
			
			synchronized(syno) {
				try {
					syno.wait(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
		}
	}
	
	@Override
	@SMethod(retryCnt=0,timeout=3000,logLevel=MC.LOG_INFO)
	public Resp<Long> start(TxConfig cfg) {
		Long txid = idGenerator.getLongId(TxConfig.class);
		Resp<Long> r = new Resp<>(Resp.CODE_SUCCESS,txid);
		
		if(txid <= 0) {
			r.setCode(Resp.CODE_FAIL);
			r.setData(txid);
			r.setMsg("create txid failure!");
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		if(LG.isLoggable(MC.LOG_INFO, null)) {
			LG.log(MC.LOG_INFO, this.getClass(), "Start transaction: " +txid);
		}
		
		TxGroup g = new TxGroup(cfg,txid);
		txGroups.put(txid, g);
		
		return r;
	}
	
	@Override
	@SMethod(retryCnt=0,timeout=3000,logLevel=MC.LOG_INFO)
	public Resp<Boolean> end(long txid) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		TxGroup g = txGroups.get(txid);
		if(g == null) {
			r.setMsg("Transaction "+txid+ " not started!");
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		synchronized(g) {
			if(g.status != TxGroup.STATUS_ON_GOING) {
				r.setMsg("Transaction  "+ txid + "  status is not on going!");
				LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
				return r;
			}
			
			txGroups.remove(txid);
			
			boolean success = true;
			for(TxVoter v : g.voters.values()) {
				
				if(!rsMap.containsKey(v.pid)) {//只要有一个参数者没在线，事务需要回滚
					r.setMsg("Resource client  "+ v.pid + " not found for txid: " + txid);
					LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
					success = false;
					break;
				}
				
				if(v.status == TxGroup.STATUS_ROLLBACK) {
					success = false;
				}
			}
			
			if(endTx(g,success)) {
				r.setCode(Resp.CODE_SUCCESS);
				r.setData(true);
				return r;
			}
		
		}
		
		return r;
	}

	private boolean endTx(TxGroup g,boolean success) {
		CountDownLatch cd = new CountDownLatch(g.voters.size());

		Holder<Boolean> holder = new Holder<>(true);
		
		for(TxVoter v : g.voters.values()) {
			ITransactionResource$JMAsyncClient client = this.rsMap.get(v.pid);
			if(client != null) {
				if(LG.isLoggable(MC.LOG_INFO, null)) {
					LG.log(MC.LOG_INFO, this.getClass(), "Notify transaction "+g.txId+" client pid:" +v.pid+",commit: " + success);
				}
				client.finishJMAsync(g.txId,success)
				.success((rst,cxt)->{
					cd.countDown();
				}).fail((code,msg,cxt)->{
					holder.set(false);
					cd.countDown();
					LG.log(MC.LOG_ERROR, this.getClass(), "txid:" + g.txId+",code:"+ code +",msg:"+msg);
				});
			} else {
				if(LG.isLoggable(MC.LOG_WARN, null)) {
					LG.log(MC.LOG_WARN, this.getClass(), "Transaction client not found for: " +v.pid);
				}
				holder.set(false);
				cd.countDown();
			}
		}
		
		try {
			cd.await(g.cfg.getTimeout(),TimeUnit.MICROSECONDS);
			return holder.get();
		} catch (InterruptedException e) {
			LG.log(MC.LOG_ERROR, this.getClass(), "Wain transaction "+ g.txId +" failure!",e);
			return false;
		}
		
	}

	@Override
	@SMethod(retryCnt=3,timeout=3000,logLevel=MC.LOG_INFO)
	public Resp<Boolean> takePartIn(int pid, long txid) {
		
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		
		TxGroup g = txGroups.get(txid);
		if(g == null) {
			r.setMsg("Transaction"+ txid +" not found for pid: " + pid);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		if(!rsMap.containsKey(pid)) {
			r.setMsg("Resource client "+ pid +" not found for txid: " + txid);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		if(g.status != TxGroup.STATUS_ON_GOING) {
			r.setMsg("Pid: "+pid+" Transaction "+txid+" status is not on going with status: " + g.status);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		TxVoter v = new TxVoter(pid);
		v.status = TxGroup.STATUS_ON_GOING;
		g.addVoter(v);
		
		r.setCode(Resp.CODE_SUCCESS);
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(retryCnt=-1,timeout=3000,logLevel=MC.LOG_INFO)
	public Resp<Boolean> vote(int pid, long txid, boolean commit) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		
		TxGroup g = txGroups.get(txid);
		if(g == null) {
			r.setMsg("Transaction"+ txid +" not found for pid: " + pid);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		if(!rsMap.containsKey(pid)) {
			r.setMsg("Resource client "+ pid +" not found for txid: " + txid);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		if(g.status != TxGroup.STATUS_ON_GOING) {
			r.setMsg("Pid: "+pid+" Transaction "+txid+" status is not on going with status: " + g.status);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		TxVoter v = g.getVoter(pid);
		if(v == null) {
			r.setMsg("Transaction "+txid+" not found voter pid: " + pid);
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return r;
		}
		
		v.status = commit ? TxGroup.STATUS_COMMITED:TxGroup.STATUS_ROLLBACK;
		
		r.setCode(Resp.CODE_SUCCESS);
		r.setData(true);
		
		return r;
	}
	
	private class TxGroup{
		
		private static final byte STATUS_ON_GOING = 1;
		private static final byte STATUS_COMMITED = 2;
		private static final byte STATUS_ROLLBACK = 3;
		
		private byte status = STATUS_ON_GOING;
		
		private long txId;
		
		private TxConfig cfg;
		
		private long startedTime;
		
		private Map<Integer,TxVoter> voters = new HashMap<>();
		
		private TxGroup(TxConfig cfg,long txId) {
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
		private byte status = TxGroup.STATUS_ON_GOING;
		
		//private ITransactionResource$JMAsyncClient resClientSrv;
		
		private TxVoter(int pid) {
			this.pid = pid;
		}
	}
}
