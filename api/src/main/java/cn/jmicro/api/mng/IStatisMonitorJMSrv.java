package cn.jmicro.api.mng;

import java.util.Map;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IStatisMonitorJMSrv {

	/**
	 * 
	 * @param mkey RPC方法键
	 * @param t 取样周期，单位是秒，即多少秒取样一次，并把结果发送客户端
	 * @return 订阅成功，返回订阅ID，取消订阅时，需要此ID标识取消那个订阅
	 */
	boolean startStatis(String mkey,Integer t);
	
	boolean stopStatis(String mkey,Integer t);
	
	Map<String,Object> index2Label();
	
	
}
