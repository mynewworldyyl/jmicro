package cn.jmicro.ext.iot.service;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 
 *
 * @author Yulei Ye
 * @date 2023年5月9日 下午9:48:34
 */
@AsyncClientProxy
public interface IDeviceServiceJMSrv {

	public static final String TABLE = "t_device";
	
	public static final String TABLE_FUN = "t_device_fun";
	
	public static final byte STATUS_ENABLE = 0;//正常可用
	
	public static final byte STATUS_FREEZONE = 1;//冻结
	
	IPromise<RespJRso<IotDeviceJRso>> addDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<IotDeviceJRso>>  bindDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<Boolean>>  unbindDevice(String deviceId);
	
	IPromise<RespJRso<Boolean>>  updateDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<List<IotDeviceJRso>>>  myDevices(QueryJRso qry);
	
	/**
	 * 设备关联账号
	 * @param actId
	 * @param deviceId
	 * @return
	 */
	IPromise<RespJRso<Map<String,Object>>> deviceLogin(Integer actId, String deviceId);
	
	//当前设备功能版本列表
	IPromise<RespJRso<Map<String, Object>>> deviceFunVers();
	
	//更新设备功能列表，如果功能不存在，则新增
	IPromise<RespJRso<Boolean>> updateFun(DeviceFunJRso funs);
	
	IPromise<RespJRso<Boolean>> delFun(String funName);
	
}
