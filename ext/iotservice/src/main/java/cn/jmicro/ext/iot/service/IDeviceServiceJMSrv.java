package cn.jmicro.ext.iot.service;

import java.util.List;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IDeviceServiceJMSrv {

	public static final String TABLE = "t_device";
	
	public static final byte STATUS_ENABLE = 0;//正常可用
	
	public static final byte STATUS_FREEZONE = 1;//冻结
	
	IPromise<RespJRso<IotDeviceJRso>>  registDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<Boolean>>  delDevice(String deviceId);
	
	IPromise<RespJRso<Boolean>>  updateDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<List<IotDeviceJRso>>>  myDevices(QueryJRso qry);
	
}
