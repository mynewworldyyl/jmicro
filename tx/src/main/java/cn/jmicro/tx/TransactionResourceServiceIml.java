package cn.jmicro.tx;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.tx.ITransactionResource;
import cn.jmicro.api.tx.ITransationService;
import cn.jmicro.api.tx.TxConfig;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.ext.mybatis.ILocalTransactionResource;

@Component
@Service(version="0.0.1", debugMode=0,
monitorEnable=0, logLevel=MC.LOG_WARN, retryCnt=3, showFront=false, external=false,
infs=ITransactionResource.class)
public class TransactionResourceServiceIml implements ITransactionResource, ILocalTransactionResource {

	@Inject
	private ProcessInfo pi;
	
	@Reference
	private ITransationService tsServer;
	
	private Map<Long,TxEntry> txEntries = Collections.synchronizedMap(new HashMap<>());
	
	public void ready() {
		
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
		
		if(commit) {
			if(LG.isLoggable(MC.LOG_INFO)) {
				LG.log(MC.LOG_INFO, this.getClass(), "Commit transaction: " + txid);
			}
		} else {
			LG.log(MC.LOG_ERROR, this.getClass(), "Rollback transaction: " + txid);
		}
		
		if(commit) {
			txe.s.commit(true);
		} else {
			txe.s.rollback(true);
		}
	
		return r;
	}

	@Override
	public boolean rollback(long txid) {
		TxEntry txe = this.txEntries.get(txid);
		if(txe == null) {
			LG.log(MC.LOG_ERROR, this.getClass(), txid +" not found, commit false ");
			return false;
		}
		this.txEntries.remove(txid);
		txe.s.rollback(true);
		return true;
	}

	@Override
	public boolean begin(ServiceMethod sm) {
		TxConfig cfg = new TxConfig();
		cfg.setTimeout(sm.getTimeout());
		Resp<Long> r = tsServer.start(cfg);
		if(r.getCode() != 0) {
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
			return false;
		}
		JMicroContext.get().setLong(TxConstants.TYPE_TX_KEY, r.getData());
		return true;
	}

	@Override
	public boolean end(long txid) {
		Resp<Boolean> r = tsServer.end(txid);
		if(!r.getData()) {
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
		}
		return r.getData();
	}

	@Override
	public boolean takePartIn(long txid,SqlSession s) {
		Resp<Boolean> r = tsServer.takePartIn(this.pi.getId(), txid);
		if(!r.getData() || r.getCode() != Resp.CODE_SUCCESS) {
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
		}
		
		TxEntry txe = new TxEntry();
		txe.txid = txid;
		txe.s = s;
		this.txEntries.put(txid, txe);
		return true;
	}

	@Override
	public boolean vote(long txid, boolean commit) {
		Resp<Boolean> r = tsServer.vote(this.pi.getId(), txid,commit);
		if(!r.getData()) {
			LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
		}
		return r.getData();
	}
	
	private class TxEntry{
		private long txid;
		private SqlSession s;
	}
	
}
