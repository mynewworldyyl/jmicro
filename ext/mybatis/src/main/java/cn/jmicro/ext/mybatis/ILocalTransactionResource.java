package cn.jmicro.ext.mybatis;

import org.apache.ibatis.session.SqlSession;

import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.registry.ServiceMethodJRso;

public interface ILocalTransactionResource {

	boolean begin(ServiceMethodJRso sm);
	
	boolean takePartIn(long txid,byte txPhase,SqlSession s);
	
	boolean vote(long txid,boolean commit);
	
	void waitTxFinish(long txid,Promise<Object> asyP);
	
	//boolean rollback(long txid);
}
