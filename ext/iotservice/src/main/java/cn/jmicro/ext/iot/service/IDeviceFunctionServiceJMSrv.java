package cn.jmicro.ext.iot.service;

import java.util.List;
import java.util.Set;

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
public interface IDeviceFunctionServiceJMSrv {

	//操作一个参数
	IPromise<RespJRso<Boolean>> delFunArg(Integer funId,Byte opmodel, DeviceFunDefArgsJRso arg);
	
	IPromise<RespJRso<Set<String>>> groupList();
	
	IPromise<RespJRso<DeviceFunDefJRso>> addDeviceDef(DeviceFunDefJRso dev);
	 
	IPromise<RespJRso<Boolean>> updateFunDef(DeviceFunDefJRso funs);
	
	IPromise<RespJRso<Boolean>> delFunDef(Integer funName);
	
	IPromise<RespJRso<List<DeviceFunDefJRso>>>  deviceFunDefs(QueryJRso qry);
	
	IPromise<RespJRso<DeviceFunDefJRso>> getDeviceDef(Integer defId);
	
	//取和当前全部显示于前端的接口KEY和Label的列表，用于下拉选择框
	IPromise<RespJRso<List<DeviceFunDefJRso>>> getDefKeyValMap();
	
	//productId未实现的接口列表
	IPromise<RespJRso<List<DeviceFunDefJRso>>> getDefKeyValMapExcludeProduct(Integer productId);
	
}
