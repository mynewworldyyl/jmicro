package cn.jmicro.ext.iot.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.gateway.IGatewayMessageCallbackJMSrv;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.DeviceDataJRso;
import cn.jmicro.ext.iot.service.IDeviceStorageJMSrv;
import cn.jmicro.ext.iot.service.IotDeviceJRso;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Service(version="0.0.1", namespace=Namespace.NS, external=true, showFront=false)
public class DeviceStorageJMSrvImpl implements IDeviceStorageJMSrv {

	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectStorage os;

	/**
	 * 直接下发消息到Api网关
	 */
	@Reference(namespace="apigateway",required=false)
	private IGatewayMessageCallbackJMSrv gmcSrv;
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<Boolean>> add(String name, String val, String desc,Byte type) {

		ActInfoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			if(Utils.isEmpty(name)) {
				r.setMsg("参数名称不能为空");
				suc.success(r);
				return;
			}
			
			/*if(Utils.isEmpty(val)) {
				r.setMsg("值不能为空");
				suc.success(r);
				return;
			}*/
			
			if(Utils.isEmpty(desc)) {
				r.setMsg("描述不能为空");
				suc.success(r);
				return;
			}
			
			/*IotDeviceJRso dide = getDeviceByDeviceId(act.getSrcActId(),act.getDeviceId());
			if(dide == null) {
				r.setMsg("设备不存在deviceId："+act.getDeviceId());
				suc.success(r);
				return;
			}*/
			
			if(count(act.getDefClientId(),act.getActName(), name) > 0) {
				r.setMsg("参数名称重复");
				suc.success(r);
				return;
			}
			
			DeviceDataJRso dev = new DeviceDataJRso();
			dev.setName(name);
			dev.setVal(val);
			dev.setType(type);
			dev.setDesc(desc);
			dev.setDeviceId(act.getActName());
			dev.setSrcActId(act.getDefClientId());
			dev.setSrcClientId(act.getClientId());
			dev.setId(idGenerator.getIntId(DeviceDataJRso.class));
			
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setCreatedTime(TimeUtils.getCurTime());
			dev.setCreatedBy(act.getDefClientId());
			dev.setUpdatedBy(act.getDefClientId());
			dev.setStatus(STATUS_ENABLE);
			
			if(!os.save(TABLE, dev, DeviceDataJRso.class, false)) {
				r.setMsg("保存数据失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
			
			suc.success(r);
			return;
		});
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<Boolean>> delete(String name) {
		ActInfoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			Map<String,Object> qry = new HashMap<>();
			qry.put("deviceId", act.getActName());//getDeviceId
			qry.put("srcActId", act.getDefClientId());//getDeviceId
			qry.put("name",name);
			
			if(os.deleteByQuery(TABLE, qry) <= 0) {
				r.setMsg("删除数据失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
			
			suc.success(r);
			return;
		});
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<Boolean>> update(String name, String val, String desc) {
		
		ActInfoJRso act = JMicroContext.get().getDevAccount();
		
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			DeviceDataJRso data = getDeviceByName(name, act.getActName(), act.getDefClientId());
			if(data == null) {
				r.setMsg("数据不存在");
				suc.success(r);
				return;
			}
			
			//data.setName(name);
			data.setDesc(desc);
			data.setVal(val);
			
			data.setUpdatedTime(TimeUtils.getCurTime());
			data.setUpdatedBy(act.getDefClientId());
			
			if(!os.updateById(TABLE, data, DeviceDataJRso.class, "id", false)) {
				r.setMsg("更新数据失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
			
			suc.success(r);
			return;
		});
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<Map<String,Object>>> getOne(String name) {
		ActInfoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Map<String,Object>>>((suc,fail)->{
			RespJRso<Map<String,Object>> r = RespJRso.d(RespJRso.CODE_SUCCESS,null);
			Map<String,Object> ps = new HashMap<>();
			DeviceDataJRso data = getDeviceByName(name, act.getActName(), act.getDefClientId());
			if(data != null) {
				ps.put("type", data.getType());
				ps.put("val", data.getVal());
				r.setCode(RespJRso.CODE_SUCCESS);
				r.setData(ps);
			}else {
				r.setCode(RespJRso.CODE_FAIL);
				r.setMsg("Device data not found name: " + name);
			}
			suc.success(r);
			return;
		});
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<List<DeviceDataJRso>>> query(QueryJRso qry) {

		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceDataJRso>>>((suc,fail)->{
			RespJRso<List<DeviceDataJRso>> r = new RespJRso<>();
			
			Map<String,Object> filter = new HashMap<>();
			filter.put("srcActId", act.getId());
			
			String key = "name";
			if (qry.getPs().containsKey(key)) {
				Map<String,Object> typeN = new HashMap<>();
				typeN.put("$regex", qry.getPs().get(key));
				filter.put("name", typeN);
			}
			
			key = "desc";
			if (qry.getPs().containsKey(key)) {
				Map<String,Object> typeN = new HashMap<>();
				typeN.put("$regex", qry.getPs().get(key));
				filter.put("desc", typeN);
			}
			
			key = "deviceId";
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
			
			int cnt =(int) os.count(TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<DeviceDataJRso> list = this.os.query(TABLE, filter, DeviceDataJRso.class,
						qry.getSize(), qry.getCurPage()-1,null,qry.getSortName(),
						IObjectStorage.getOrderVal(qry.getOrder(), 1));
				r.setData(list);
			}
			
			suc.success(r);
		});
	
	}
	
	private int count(Integer actId,String deviceId, String name) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("name", name);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.count(TABLE, qry);
	}

	private DeviceDataJRso getDeviceByDataId(Integer dataId, String deviceId, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("id", dataId);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.getOne(TABLE, qry, DeviceDataJRso.class);
	}
	
	private DeviceDataJRso getDeviceByName(String name, String deviceId, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("name", name);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.getOne(TABLE, qry, DeviceDataJRso.class);
	}
	
	private IotDeviceJRso getDeviceByDeviceId(Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("deviceId", deviceId);
		qry.put("srcActId", actId);
		return os.getOne(TABLE, qry, IotDeviceJRso.class);
	}
	
}
