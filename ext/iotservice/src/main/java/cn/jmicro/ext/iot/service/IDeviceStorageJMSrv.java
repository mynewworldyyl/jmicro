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
 * @date 2023年5月27日 下午1:40:47
 */
@AsyncClientProxy
public interface IDeviceStorageJMSrv {

	public static final String TABLE = "t_device_storage";
	
	public static final byte STATUS_ENABLE = 0;//正常可用
	
	public static final byte STATUS_FREEZONE = 1;//冻结
	
	IPromise<RespJRso<Boolean>> add(String name, String val, String desc,Byte type);
	
	IPromise<RespJRso<Boolean>>  delete(String name);
	
	IPromise<RespJRso<Boolean>>  update(String name, String val, String desc);
	
	IPromise<RespJRso<List<DeviceDataJRso>>>  query(QueryJRso qry);
	
	IPromise<RespJRso<Map<String,Object>>> getOne(String name);
}
