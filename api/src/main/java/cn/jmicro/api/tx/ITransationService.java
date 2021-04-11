package cn.jmicro.api.tx;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 事务服务器
 * @author yeyulei
 *
 */
@AsyncClientProxy
public interface ITransationService {

	/**
	 * 开始一个全局事务
	 * @return 全局事务ID
	 */
	Resp<Long> start(TxConfig cfg);
	
	/**
	 * 结束事务
	 * @param txId
	 * @return
	 */
	//public IPromise<Resp<Boolean>> end(long txId);
	
	/**
	 * 
	 * @param notifySmKey 事务本地资源管理器客户端，接收事务成功或失败通知
	 * @param txid
	 * @return
	 */
	Resp<Boolean> takePartIn(int pid,long txid);
	
	/**
	 * 投票
	 * @param pid ProcessInfo ID
	 * @param txid 全局事务ID
	 * @param commit true 同意提交
	 * @return
	 */
	Resp<Boolean> vote(int pid,long txid,boolean commit);
}
