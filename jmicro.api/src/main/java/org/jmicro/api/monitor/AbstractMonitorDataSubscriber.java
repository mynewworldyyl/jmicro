package org.jmicro.api.monitor;

import org.jmicro.common.CommonException;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	public static final Integer[] YTPES = new Integer[]{
			//服务器发生错误,返回ServerError异常
			MonitorConstant.CLIENT_REQ_EXCEPTION_ERR,
			//业务错误,success=false,此时接口调用正常
			MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,
			//请求超时
			MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL,
			//请求开始
			MonitorConstant.CLIENT_REQ_BEGIN,
			//异步请求成功确认包
			MonitorConstant.CLIENT_REQ_ASYNC1_SUCCESS,
			//同步请求成功
			MonitorConstant.CLIENT_REQ_OK,
			//超时次数
			MonitorConstant.CLIENT_REQ_TIMEOUT
		};
	
	@Override
	public Double getData(String srvKey,Integer type) {
		throw new CommonException("getData not support for type: "+type);
	}

}
