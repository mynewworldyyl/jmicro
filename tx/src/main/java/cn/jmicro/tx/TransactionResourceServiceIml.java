package cn.jmicro.tx;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.tx.ITransactionResource;
import cn.jmicro.api.tx.TxConfig;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.api.tx.genclient.ITransationService$JMAsyncClient;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.ext.mybatis.ILocalTransactionResource;

@Component
@Service(version="0.0.1", debugMode=0,
monitorEnable=0, logLevel=MC.LOG_DEBUG, retryCnt=3, showFront=false, external=false,
infs=ITransactionResource.class)
public class TransactionResourceServiceIml implements ITransactionResource, ILocalTransactionResource {

	private static final Class<?> TAG = TransactionResourceServiceIml.class;
	
	private long localTxTimeout = 30000;
	
	@Inject
	private ProcessInfo pi;
	
	@Reference
	private ITransationService$JMAsyncClient tsServer;
	
	private Map<Long,TxEntry> txEntries = new HashMap<>();
	
	public void ready() {
		new Thread(this::check).start();
	}
	
	private void check() {
		Object synLocker = new Object();
		Set<Long> txids = new HashSet<>();
		while(true) {
			try {
				if(txEntries.isEmpty()) {
					synchronized(synLocker) {
						synLocker.wait(5000);
					}
					continue;
				}
				
				synchronized(txEntries) {
					txids.addAll(txEntries.keySet());
				}
				
				for(Long txid: txids) {
					TxEntry te = this.txEntries.get(txid);
					if(te == null || !te.valid) {
						continue;
					}
					long interval = TimeUtils.getCurTime() - te.startTime;
					if(interval > localTxTimeout) {
						synchronized(te) {
							if(!te.valid) {
								continue;
							}
							
							if(te.phase == TxEntry.PHASE_VOTE_FAIL) {
								if(++te.retryCnt<3) {
									LG.log(MC.LOG_WARN, TAG, "Revote: " + te.voted + " txid: " + txid+", timeout: " + localTxTimeout+",interval: " +interval);
									this.vote(txid, false);
								} else {
									LG.log(MC.LOG_ERROR, TAG, "Vote: " + te.voted + " do vote timeout txid: " + txid+", timeout: " + localTxTimeout+",interval: " +interval);
									forceRollback(te);
								}
							}else if(te.phase == TxEntry.PHASE_VOTE_SUCC) {
								LG.log(MC.LOG_ERROR, TAG, "Vote: " + te.voted + " wait txserver notify timeout txid: " + txid+", timeout: " + localTxTimeout+",interval: " +interval);
								if(te.voted) {
									//投同意票，3PC段做事务回滚
									forceRollback(te);
								} else {
									forceRollback(te);
								}
							}
						}
					}
				}
				txids.clear();
			}catch(Throwable e) {
				LG.log(MC.LOG_ERROR, TAG, "Checker error:" + pi.getInstanceName(),e);
			}
		}
	}
	
	private void forceRollback(TxEntry txe) {
		String msg = "Force rollback tx:"+txe.txid+",insId:" + this.pi.getInstanceName();
		try {
			LG.log(MC.LOG_ERROR, TAG, msg);
			synchronized(txEntries) {
				txEntries.remove(txe.txid);
			}
			txe.s.rollback(true);
		} finally {
			if(txe.p != null) {
				txe.p.setFail(Resp.CODE_TX_FAIL, msg);
				txe.p.done();
			}
		}
	}

	@Override
	@SMethod
	public Resp<Boolean> finish(long txid, boolean commit) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_SUCCESS,true);
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null) {
			LG.log(MC.LOG_ERROR, this.getClass(), txid +" not found, commit: " + commit);
			r.setCode(Resp.CODE_TX_FAIL);
			r.setData(false);
			return r;
		}
		
		synchronized(txe) {

			try {
				
				if(commit) {
					txe.phase = TxEntry.PHASE_GOT_TX_COMMIT;
					if(LG.isLoggable(MC.LOG_DEBUG)) {
						LG.log(MC.LOG_DEBUG, this.getClass(), "Client "+pi.getId()+" commit transaction: " + txid);
					}
				} else {
					txe.phase = TxEntry.PHASE_GOT_TX_ROLLBACK;
					LG.log(MC.LOG_WARN, this.getClass(), "Rollback transaction: " + txid);
				}
				
				txe.valid = false;
				if(commit) {
					txe.s.commit(true);
				} else {
					txe.s.rollback(true);
				}
			} finally {
				
				synchronized(txEntries) {
					this.txEntries.remove(txid);
				}
				
				if(txe.p != null) {
					if(!commit) {
						txe.p.setFail(Resp.CODE_TX_FAIL, "Rollback tx:"+txid);
					}
					txe.p.done();
				}
			}
		}
	
		return r;
	}

	@Override
	public boolean begin(ServiceMethod sm) {
		TxConfig cfg = new TxConfig();
		cfg.setTimeout(sm.getTimeout());
		cfg.setPid(pi.getId());
		Resp<Long> r = tsServer.start(cfg);
		if(r.getCode() != 0) {
			LG.log(MC.LOG_ERROR, this.getClass(), "Start tx failure: "+r.getMsg());
			return false;
		}
		JMicroContext.get().setLong(TxConstants.TYPE_TX_KEY, r.getData());
		return true;
	}
	
	@Override
	public void waitTxFinish(long txid,PromiseImpl<Object> asyP) {
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null || !txe.valid) {
			LG.log(MC.LOG_ERROR, this.getClass(), txid +" not found, commit false ");
			if(asyP != null) {
				asyP.done();
			}
		} else {
			if(LG.isLoggable(MC.LOG_INFO)) {
				String msg = "Wait tx finish txid: " + 
						(JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L));
				LG.log(MC.LOG_INFO, TransactionResourceServiceIml.class, msg);
			}
			txe.p = asyP;
		}
	}
	

	@Override
	public boolean takePartIn(long txid,SqlSession s) {
		
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG, this.getClass(), "Client "+pi.getId()+" take part in transaction: " + txid);
		}
		
		Resp<Boolean> r = tsServer.takePartIn(this.pi.getId(), txid);
		
		if(!r.getData() || r.getCode() != Resp.CODE_SUCCESS) {
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return false;
		}
		
		TxEntry txe = new TxEntry();
		txe.startTime = TimeUtils.getCurTime();
		txe.timeout=10000;//5秒事务超时
		txe.txid = txid;
		txe.s = s;
		txe.phase = TxEntry.PHASE_TAKE_PART_IN;
		
		synchronized(txEntries) {
			this.txEntries.put(txid, txe);
		}
		
		return true;
	}

	@Override
	public boolean vote(long txid, boolean commit) {
		if(LG.isLoggable(MC.LOG_INFO)) {
			LG.log(MC.LOG_INFO, this.getClass(), "Client "+pi.getId()+" vote transaction: " + txid+" with: "+commit);
		}
		
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null) {
			LG.log(MC.LOG_ERROR, TAG, "TxEntry is null,txid: " + txid+",commit:"+commit);
			return false;
		}
		
		Resp<Boolean> r = tsServer.vote(this.pi.getId(), txid,commit);
		
		synchronized(txe) {
			txe.voted = commit;
			txe.startTime = TimeUtils.getCurTime();
			if(r.getData()) {
				txe.phase = TxEntry.PHASE_VOTE_SUCC;
			}else {
				LG.log(MC.LOG_ERROR, this.getClass(), "Vote fail txid: " + txid + r.getMsg());
				txe.phase = TxEntry.PHASE_VOTE_FAIL;
			}
		}
		
		return r.getData();
	}
	
	private class TxEntry {
		
		private static final int PHASE_TAKE_PART_IN=1;//加入事务群
		private static final int PHASE_VOTE_SUCC=2;//投票成功
		private static final int PHASE_VOTE_FAIL=3;//投票失败
		private static final int PHASE_GOT_TX_COMMIT=4;//已经取得事务协调器通知
		private static final int PHASE_GOT_TX_ROLLBACK=5;//已经取得事务协调器通知
		private static final int PHASE_TIMEOUT_ROLLBACK=6;//已经取得事务协调器通知
		
		private int retryCnt = 0;
		
		private long timeout;
		private long startTime;
		private long txid;
		private SqlSession s;
		private boolean valid=true;
		
		private boolean voted = false;//同意票，否定票
		
		private int phase = PHASE_TAKE_PART_IN;
		private PromiseImpl<Object> p;
	}
	
}
