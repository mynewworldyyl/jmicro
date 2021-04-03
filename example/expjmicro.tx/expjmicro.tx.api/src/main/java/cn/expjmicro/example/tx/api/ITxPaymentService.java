package cn.expjmicro.example.tx.api;

import cn.expjmicro.example.tx.api.entities.Payment;
import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ITxPaymentService {

	Resp<Boolean> pay(Payment p);
	
}
