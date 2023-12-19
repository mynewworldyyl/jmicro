package cn.jmicro.ext.iot.service;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * @author Yulei Ye
 * @date 2023年5月9日 下午9:48:34
 */
@AsyncClientProxy
public interface IDeviceServiceJMSrv {
	
	IPromise<RespJRso<IotDeviceJRso>> addDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<IotDeviceJRso>>  bindDevice(String macAddr,  String deviceId);
	
	IPromise<RespJRso<Boolean>>  unbindDevice(String deviceId);
	
	IPromise<RespJRso<Boolean>>  updateDevice(IotDeviceJRso dev);
	
	IPromise<RespJRso<List<IotDeviceJRso>>>  myDevices(QueryJRso qry);
	
	//取得指令设备信息，只能取得自己的设备
	IPromise<RespJRso<IotDeviceJRso>> getDevices(String deviceId);
	
	/**
	 * 设备关联账号
	 * @param actId
	 * @param deviceId
	 * @return
	 */
	IPromise<RespJRso<Map<String,Object>>> deviceLogin(Integer actId, String deviceId);
	
	//我的主设备列表，供前端下拉选择框使用
	IPromise<RespJRso<Map<String,String>>> myMasterDevices(Byte master);
	
	//删除未初始化设备，已经初始化的设备不能删除
	IPromise<RespJRso<Boolean>> deleteDevice(Integer did);
	
	//重置设备恢复初始状态
	IPromise<RespJRso<IotDeviceJRso>> resetDevice(String deviceId);
	 
}
