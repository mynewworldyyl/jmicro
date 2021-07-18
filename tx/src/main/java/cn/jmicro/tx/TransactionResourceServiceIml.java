package cn.jmicro.tx;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.api.registry.ServiceItemJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.registry.UniqueServiceKeyJRso;
import cn.jmicro.api.service.ServiceManager;
import cn.jmicro.api.tx.ITransactionResourceJMSrv;
import cn.jmicro.api.tx.ITransationServiceJMSrv;
import cn.jmicro.api.tx.ITxListener;
import cn.jmicro.api.tx.ITxListenerManager;
import cn.jmicro.api.tx.TxConfigJRso;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.api.tx.TxInfoJRso;
import cn.jmicro.api.tx.genclient.ITransationServiceJMSrv$JMAsyncClient;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.ext.mybatis.ILocalTransactionResource;

@Component
@Service(version="0.0.1", debugMode=1,
monitorEnable=0, logLevel=MC.LOG_DEBUG, retryCnt=3, showFront=false, external=false,
infs=ITransactionResourceJMSrv.class)
public class TransactionResourceServiceIml implements ITransactionResourceJMSrv, ILocalTransactionResource,ITxListenerManager {

	private final static Logger logger = LoggerFactory.getLogger(TransactionResourceServiceIml.class);
	
	private static final Class<?> TAG = TransactionResourceServiceIml.class;
	
	private static final String STR_TAG = ITransactionResourceJMSrv.STR_TAG;
	
	private long localTxTimeout = 30000;
	
	@Inject
	private ServiceManager srvMng;
	
	@Inject
	private ProcessInfoJRso pi;
	
	@Inject
	private IRegistry reg;
	
	@Reference
	private ITransationServiceJMSrv$JMAsyncClient tsServer;
	
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
			LG.log(MC.LOG_ERROR, STR_TAG, msg);
			synchronized(txEntries) {
				txEntries.remove(txe.txid);
			}
			txe.s.rollback(true);
		} finally {
			if(txe.p != null) {
				txe.p.setFail(RespJRso.CODE_TX_FAIL, msg);
				txe.p.done();
			}
			this.notifyTxListener(txe, false);
		}
	}

	@Override
	public RespJRso<Boolean> canCommit(long txid) {
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL,false);
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null || txe.s == null) {
			if(LG.isLoggable(MC.LOG_WARN)) {
				LG.log(MC.LOG_WARN, TAG, txid +" entry not found when canCommit ");
			}
			return r;
		}
		
		try {
			if(!txe.s.getConnection().isValid(txe.timeout)) {
				LG.log(MC.LOG_WARN, TAG, txid +" connection invalid");
				return r;
			}
		} catch (SQLException e) {
			LG.log(MC.LOG_ERROR, TAG, txid +" canCommit", e);
			return r;
		}

		r.setCode(RespJRso.CODE_SUCCESS);
		r.setData(true);
		return r;
	}

	@Override
	@SMethod(debugMode=1)
	public RespJRso<Boolean> finish(long txid, boolean commit) {
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_SUCCESS,true);
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null) {
			LG.log(MC.LOG_ERROR, this.getClass(), txid +" not found, commit: " + commit);
			r.setCode(RespJRso.CODE_TX_FAIL);
			r.setData(false);
			return r;
		}
		
		synchronized(txe) {

			try {
				
				if(commit) {
					txe.phase = TxEntry.PHASE_GOT_TX_COMMIT;
					if(LG.isLoggable(MC.LOG_DEBUG)) {
						LG.log(MC.LOG_DEBUG, this.getClass(), "Client "+pi.getId()+" commit:" + commit + " txid: " + txid);
					}
				} else {
					txe.phase = TxEntry.PHASE_GOT_TX_ROLLBACK;
					LG.log(MC.LOG_WARN, this.getClass(), "Rollback txid: " + txid);
				}
				
				txe.valid = false;
				if(commit) {
					txe.s.commit(true);
				} else {
					//LG.log(MC.LOG_WARN, this.getClass(),  "Begin do rollback:"+txid);
					txe.s.rollback(true);
					//LG.log(MC.LOG_WARN, this.getClass(),  "Rollback finish:"+txid);
				}
				
			} catch(Throwable e) {
				//在此进入3PC阶段
				String msg = "Error txid: "+txid +" commit: " + commit+",insName: "+
						this.pi.getInstanceName()+",insId: " + pi.getId();
				LG.log(MC.LOG_ERROR,STR_TAG,msg,e);
				logger.error(msg,e);
				r.setCode(RespJRso.CODE_TX_FAIL);//事务提交失败
				r.setData(false);
				r.setMsg(msg);
			} finally {
				synchronized(txEntries) {
					this.txEntries.remove(txid);
				}
				/*if(JMicroContext.get().isDebug()) {
					JMicroContext.get().appendCurUseTime("Notify caller rollback",true);
				}*/
				if(txe.p != null) {
					/*if(!commit) {
						LG.log(MC.LOG_WARN, this.getClass(), "Notify caller rollback" + txid);
					}*/
					txe.p.done();
				}
				/*if(!commit) {
					LG.log(MC.LOG_WARN, this.getClass(), "Notify listener" + txid);
				}*/
				this.notifyTxListener(txe, commit);
			}
		}
		/*if(!commit) {
			LG.log(MC.LOG_WARN, this.getClass(), "Rollback finish return: " + txid);
		}*/
		
	/*	if(JMicroContext.get().isDebug()) {
			JMicroContext.get().appendCurUseTime("Rollback finish return",true);
		}*/
		
		return r;
	}

	@Override
	public boolean begin(ServiceMethodJRso sm) {
		TxConfigJRso cfg = new TxConfigJRso();
		cfg.setTimeout(sm.getTimeout());
		cfg.setPid(pi.getId());
		RespJRso<TxInfoJRso> r = tsServer.start(cfg);
		if(r.getCode() != 0) {
			LG.log(MC.LOG_ERROR, this.getClass(), "Start tx failure: "+r.getMsg());
			return false;
		}
		TxInfoJRso ti = r.getData();
		JMicroContext.get().setLong(TxConstants.TX_ID, ti.getTxid());
		JMicroContext.get().setInt(TxConstants.TX_SERVER_ID, ti.getServerId());
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
						(JMicroContext.get().getLong(TxConstants.TX_ID, -1L));
				LG.log(MC.LOG_INFO, TransactionResourceServiceIml.class, msg);
			}
			txe.p = asyP;
		}
	}
	

	@Override
	public boolean takePartIn(long txid,byte txPhase,SqlSession s) {
		
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG, this.getClass(), "Client "+pi.getId()+" take part in transaction: " + txid);
		}
		
		TxEntry txe = new TxEntry();
		
		if(!setDirectItem(txe,null)) {
			return false;
		}
		
		RespJRso<Boolean> r = tsServer.takePartIn(pi.getId(), txid, txPhase);
		
		if(!r.getData() || r.getCode() != RespJRso.CODE_SUCCESS) {
			LG.log(MC.LOG_ERROR, TAG, r.getMsg());
			return false;
		}
		
		txe.startTime = TimeUtils.getCurTime();
		txe.timeout=10000;//5秒事务超时
		txe.txid = txid;
		txe.s = s;
		txe.phase = TxEntry.PHASE_TAKE_PART_IN;
		txe.txPhase = txPhase;
		
		synchronized(txEntries) {
			this.txEntries.put(txid, txe);
		}
		
		return true;
	}

	private boolean setDirectItem(TxEntry txe,ServiceItemJRso si) {
		
		if(si == null) {
			Integer insId = JMicroContext.get().getInt(TxConstants.TX_SERVER_ID, 0);
			UniqueServiceKeyJRso siKey = reg.getService(ITransationServiceJMSrv.class.getName(), insId);
			if(siKey == null) {
				LG.log(MC.LOG_ERROR, TAG, "Tx server insId: " + JMicroContext.get().getInt(TxConstants.TX_SERVER_ID, 0)+" not found");
				return false;
			}
			ServiceItemJRso sit = this.srvMng.getItem(siKey.fullStringKey());
			txe.si = sit;
		}
		
		if(txe.si == null) {
			LG.log(MC.LOG_ERROR, TAG, "Tx server insId: " + JMicroContext.get().getInt(TxConstants.TX_SERVER_ID, 0)+" not found");
			return false;
		}
		
		JMicroContext.get().setParam(Constants.DIRECT_SERVICE_ITEM, si);
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
		
		if(!setDirectItem(txe,txe.si)) {
			return false;
		}
		
		RespJRso<Boolean> r = tsServer.vote(this.pi.getId(), txid,commit);
		
		synchronized(txe) {
			txe.voted = commit;
			txe.startTime = TimeUtils.getCurTime();
			if(r.getData()) {
				txe.phase = TxEntry.PHASE_VOTE_SUCC;
			} else {
				LG.log(MC.LOG_ERROR, this.getClass(), "Vote fail txid: " + txid + r.getMsg());
				txe.phase = TxEntry.PHASE_VOTE_FAIL;
			}
		}
		
		return r.getData();
	}
	
	@Override
	public boolean addTxListener(ITxListener l) {
		long txid = JMicroContext.get().getLong(TxConstants.TX_ID, -1L);
		if(txid <= 0) {
			return false;
		}
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null) {
			LG.log(MC.LOG_ERROR, TAG, "TxEntry not found,txid: " + txid);
			return false;
		}
		
		if(txe.lis == null) {
			txe.lis = new HashSet<>();
		}
		
		if(!txe.lis.contains(l)) {
			txe.lis.add(l);
		}
		
		return true;
		
	}
	
	private void notifyTxListener(TxEntry txe,boolean commit) {
		if(txe.lis == null) {
			return;
		}
		
		for(ITxListener l : txe.lis) {
			l.onTxResult(commit, txe.txid);
		}
	}

	private class TxEntry {
		
		private static final int PHASE_TAKE_PART_IN=1;//加入事务群
		private static final int PHASE_VOTE_SUCC=2;//投票成功
		private static final int PHASE_VOTE_FAIL=3;//投票失败
		private static final int PHASE_GOT_TX_COMMIT=4;//已经取得事务协调器通知
		private static final int PHASE_GOT_TX_ROLLBACK=5;//已经取得事务协调器通知
		private static final int PHASE_TIMEOUT_ROLLBACK=6;//已经取得事务协调器通知
		
		private int retryCnt = 0;
		
		private int timeout;
		private long startTime;
		private long txid;
		private SqlSession s;
		private boolean valid=true;
		
		private boolean voted = false;//同意票，否定票
		
		private ServiceItemJRso si;
		
		private int phase = PHASE_TAKE_PART_IN;
		
		private byte txPhase = TxConstants.TX_2PC;
		
		private PromiseImpl<Object> p;
		
		private Set<ITxListener> lis;
	}
	
}
