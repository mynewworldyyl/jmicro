package cn.jmicro.ext.iot.service;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.ext.iot.service.vo.DeviceFunVoJRso;

/**
 * 
 *
 * @author Yulei Ye
 * @date 2023年5月9日 下午9:48:34
 */
@AsyncClientProxy
public interface IDeviceFunJMSrv {
	
	//当前设备功能版本列表
	IPromise<RespJRso<Map<String,Object>>> deviceFunVers(Map<String,String> devInfo);
	
	//更新设备功能列表，如果功能不存在，则新增
	IPromise<RespJRso<Boolean>> updateFun(List<DeviceFunJRso> funs);
	
	IPromise<RespJRso<Boolean>> delFun(Integer funDefId);
	
	IPromise<RespJRso<List<DeviceFunJRso>>>  deviceFuns(String deviceId);
	
	IPromise<RespJRso<List<DeviceFunVoJRso>>> deviceFrontFunDetail(String deviceId);
	
	//给设备增加操作
	IPromise<RespJRso<Integer>> addOrUpdateFunOperation(Integer funId, DeviceFunOperationJRso op);
	
	//删除设备指令
	IPromise<RespJRso<Boolean>> delFunOperation(Integer funId, Integer opId);
	
	//更新指令资源ID
	IPromise<RespJRso<Boolean>> updateFunOperationResId(Integer funId, Integer opId, String opName, Integer  resId);
	
	//根据资源ID取得指令ID
	IPromise<RespJRso<String>> deviceCmdByResId(Integer resId);
}
