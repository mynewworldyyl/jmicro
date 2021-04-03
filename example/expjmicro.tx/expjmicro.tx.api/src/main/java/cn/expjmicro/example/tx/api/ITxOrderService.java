package cn.expjmicro.example.tx.api;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ITxOrderService {

	Resp<Boolean> takeOrder(int goodId,int num);
	
}
