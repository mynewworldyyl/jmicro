package cn.jmicro.ext.mybatis;

import org.apache.ibatis.session.SqlSession;

import cn.jmicro.api.registry.ServiceMethod;

public interface ILocalTransactionResource {

	boolean begin(ServiceMethod sm);
	
	boolean end(long txid);
	
	boolean takePartIn(long txid,SqlSession s);
	
	boolean vote(long txid,boolean commit);
	
	boolean rollback(long txid);
}
