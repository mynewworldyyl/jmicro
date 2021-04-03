package cn.jmicro.api.tx;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ITransactionResource {

	/**
	 * 通知客户端提交事务
	 * @return 成功失败
	 */
	Resp<Boolean> finish(long txid,boolean commit);
	
	/**
	 * 回滚事务
	 * @param txid
	 * @return
	 */
	//Resp<Boolean> rollback(long txid);
	
}
