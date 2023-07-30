package cn.jmicro.ext.iot.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.ext.iot.service.vo.DeviceFunVoJRso;
import cn.jmicro.ext.iot.service.vo.DeviceFunVobJRso;

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
	//IPromise<RespJRso<Boolean>> updateFun(List<DeviceFunJRso> funs);
	
	IPromise<RespJRso<Boolean>> delFun(Integer funDefId);
	
	IPromise<RespJRso<List<DeviceFunJRso>>>  deviceFuns(String deviceId);
	
	IPromise<RespJRso<List<DeviceFunVoJRso>>> deviceFrontFunDetail(String deviceId);
	
	public IPromise<RespJRso<Boolean>> updateOrDelFuns(Integer productId,Set<Integer> adds, Set<Integer> dels);
	IPromise<RespJRso<Boolean>> updateOneFun(DeviceFunJRso f);
	IPromise<RespJRso<Boolean>> addOneFun(DeviceFunJRso f);
	IPromise<RespJRso<List<DeviceFunVobJRso>>> listFuns(QueryJRso qry);
	IPromise<RespJRso<List<DeviceFunVobJRso>>> listProductFuns(QueryJRso qry0);
	
	//查询指定产品的功能列表，只返回必要字段
	IPromise<RespJRso<List<DeviceFunVobJRso>>> listProductFunKV(QueryJRso qry0);
	
	
}
