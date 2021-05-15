package cn.expjmicro.example.tx.api;

import cn.expjmicro.example.tx.api.entities.Payment;
import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 支付服务，分别用同步和异步实现
 *
 * @author Yulei Ye
 * @date 2021年4月18日 上午10:39:27
 */
@AsyncClientProxy
public interface ITxPaymentService {

	Resp<Boolean> pay(Payment p);
	
	IPromise<Resp<Boolean>> payAsy(Payment p);
}
