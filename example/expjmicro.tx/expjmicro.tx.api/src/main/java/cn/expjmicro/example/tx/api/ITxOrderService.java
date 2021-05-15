package cn.expjmicro.example.tx.api;

import cn.expjmicro.example.tx.api.entities.Good;
import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 订单服务，分同步和异步两种实现
 *
 * @author Yulei Ye
 * @date 2021年4月18日 上午10:40:21
 */
@AsyncClientProxy
public interface ITxOrderService {

	Resp<Boolean> takeOrder(Good good,int num);
	
	IPromise<Resp<Boolean>> takeOrderAsy(Good good,int num);
	
	
}
