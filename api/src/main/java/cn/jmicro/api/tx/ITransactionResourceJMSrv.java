package cn.jmicro.api.tx;

import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ITransactionResourceJMSrv {

	public static final String STR_TAG = "tx";
	
	/**
	 * 通知客户端提交事务
	 * @return 成功失败
	 */
	RespJRso<Boolean> finish(long txid,boolean commit);
	
	/**
	 * 3PC的二阶段，判断参与者是否可以提交，只有全部参与者确认可提交后，协调者才能提交事务
	 * @param txid
	 * @return
	 */
	RespJRso<Boolean> canCommit(long txid);
	
}
