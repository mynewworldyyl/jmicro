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

	void onData(StatisData sc);
	
}
