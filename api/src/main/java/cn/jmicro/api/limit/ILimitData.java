package cn.jmicro.api.limit;

import cn.jmicro.api.monitor.StatisData;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 限速服务器通过此服务接口通知服务器限速数据
 * @author yeyulei
 *
 */
@AsyncClientProxy
public interface ILimitData {
	
	public static final int LIMIT_SOURCE_IP = 1;
	
	public static final int LIMIT_SOURCE_ACCOUNT = 2;
	

	void onData(StatisData sc);
	
}
