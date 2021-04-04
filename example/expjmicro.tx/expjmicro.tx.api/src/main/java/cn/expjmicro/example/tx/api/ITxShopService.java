package cn.expjmicro.example.tx.api;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface ITxShopService {

	 Resp<Boolean> buy(int goodId,int num);
	
	 Resp<Boolean> updateLocalData(int goodId,int num);
}
