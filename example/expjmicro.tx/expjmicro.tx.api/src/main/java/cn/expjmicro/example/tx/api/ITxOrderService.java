package cn.expjmicro.example.tx.api;

import cn.expjmicro.example.tx.api.entities.Good;
import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ITxOrderService {

	Resp<Boolean> takeOrder(int goodId,int num);
	
	IPromise<Resp<Boolean>> takeOrderAsy(Good good,int num);
	
	
}
