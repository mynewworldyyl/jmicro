package cn.expjmicro.example.tx.api;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 
 * 模拟商店购买接口
 * 完成整个流程分为商店下单服务，生成订单服务，支付服务
 * 分为同步和异步两种接口实现方式
 * @author Yulei Ye
 * @date 2021年4月18日 上午10:28:11
 */
@AsyncClientProxy
public interface ITxShopService {

	 /**
	  * 同步下单
	  * @param goodId 商品ID
	  * @param num 购买数量
	  * @return
	  */
	 Resp<Boolean> buy(int goodId,int num);
	 
	 /**
	  * 异步下单
	  * @param goodId 商品ID
	  * @param num 购买数量
	  * @return
	  */
	 IPromise<Resp<Boolean>> buyAsy(int goodId,int num);
	 
	 Resp<Boolean> updateLocalData(int goodId,int num);
	 
	 void resetGoodCache(int goodId);
}
