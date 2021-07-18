package cn.jmicro.ext.mybatis;

import org.apache.ibatis.session.SqlSession;

import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.registry.ServiceMethodJRso;

public interface ILocalTransactionResource {

	boolean begin(ServiceMethodJRso sm);
	
	boolean takePartIn(long txid,byte txPhase,SqlSession s);
	
	boolean vote(long txid,boolean commit);
	
	void waitTxFinish(long txid,PromiseImpl<Object> asyP);
	
	//boolean rollback(long txid);
}
