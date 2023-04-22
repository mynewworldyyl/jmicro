package cn.jmicro.ext.iot.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.Base64Utils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.IDeviceServiceJMSrv;
import cn.jmicro.ext.iot.service.IotDeviceJRso;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Service(version="0.0.1",namespace=Namespace.NS,external=true,showFront=false)
public class DeviceServiceJMSrvImpl implements IDeviceServiceJMSrv {

	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectStorage os;
	
	/*
	@Inject
	private AccountServiceImpl as;
	*/
	
	@Override
	@SMethod(maxSpeed=1, upSsl=true, encType=0, downSsl=true, needLogin=true, perType=false)
	public IPromise<RespJRso<IotDeviceJRso>> registDevice(IotDeviceJRso dev) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<IotDeviceJRso>>((suc,fail)->{
			RespJRso<IotDeviceJRso> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(dev.getName())) {
				r.setMsg("名称不能为空");
				suc.success(r);
				return;
			}
			
			if(count(act.getId(),dev.getName()) > 0) {
				r.setMsg("设备名称重复");
				suc.success(r);
				return;
			}
			
			dev.setId(idGenerator.getLongId(IotDeviceJRso.class));
			dev.setDeviceId(toDeviceId(dev.getId()+TimeUtils.getCurTime()));
			
			/*RespJRso<ActInfoJRso>  arr = as.registDevice(dev.getDeviceId(), act.getClientId());
			if(arr.getCode() != 0) {
				//注册关联设备账号失败
				r.setCode(arr.getCode());
				r.setMsg(arr.getMsg());
				suc.success(r);
				return;
			}*/
			
			dev.setSrcActId(act.getId());
			dev.setSrcClientId(act.getClientId());
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setCreatedTime(TimeUtils.getCurTime());
			dev.setCreatedBy(act.getId());
			dev.setUpdatedBy(act.getId());
			dev.setStatus(STATUS_ENABLE);
			
			if(!os.save(TABLE, dev, IotDeviceJRso.class, false)) {
				r.setMsg("租户创建失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(dev);
			
			suc.success(r);
			return;
		});
		
	}
	
	private String toDeviceId(long sid) {
		try {
			
			byte[] bytes = new byte[8];
			bytes[0] = (byte)((sid >> 56) & 0xFF);
			bytes[1] = (byte)((sid >> 48) & 0xFF);
			bytes[2] = (byte)((sid >> 40) & 0xFF);
			bytes[3] = (byte)((sid >> 32) & 0xFF);
			
			bytes[4] = (byte)((sid >> 24) & 0xFF);
			bytes[5] = (byte)((sid >> 16) & 0xFF);
			bytes[6] = (byte)((sid >> 8) & 0xFF);
			bytes[7] = (byte)((sid >> 0) & 0xFF);
			
			/*byte temp;
			Random rv = new Random(TimeUtils.getCurTime());
			
			for(int i = 0 ; i < 3; i++) {
				int rvi = rv.nextInt(4);
				temp = bytes[rvi];
				bytes[rvi] = bytes[rvi+4];
				bytes[rvi+4] = temp;
			}*/
			
			return Base64Utils.encodeToStr(bytes);
		} catch (Exception e) {
			log.error("toDeviceId:"+sid,e);
			return null;
		}
		
	}

	@Override
	@SMethod(maxSpeed=1, upSsl=true, encType=0, downSsl=true, needLogin=true, perType=false)
	public IPromise<RespJRso<Boolean>> delDevice(String deviceId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(deviceId)) {
				r.setMsg("删除设备不存在");
				suc.success(r);
				return;
			}
			
			Map<String,Object> qry = new HashMap<>();
			qry.put("deviceId", deviceId);
			qry.put("srcActId", act.getId());
			
			if(os.deleteByQuery(TABLE, qry) <= 0) {
				r.setMsg("设备删除失败");
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
	@SMethod(maxSpeed=1, upSsl=true, encType=0, downSsl=true, needLogin=true, perType=false)
	public IPromise<RespJRso<Boolean>> updateDevice(IotDeviceJRso dev) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(dev.getName())) {
				r.setMsg("名称不能为空");
				suc.success(r);
				return;
			}
			
			IotDeviceJRso ed = getDeviceByDeviceId(act.getId(), dev.getDeviceId());
			if(ed == null) {
				r.setMsg("设备不存在");
				suc.success(r);
				return;
			}
			
			if(!dev.getName().equals(ed.getName())) {
				if(count(act.getId(), dev.getName()) > 0) {
					r.setMsg("设备名称重复");
					suc.success(r);
					return;
				}
			}
			
			ed.setUpdatedTime(TimeUtils.getCurTime());
			ed.setUpdatedBy(act.getId());
			ed.setName(dev.getName());
			ed.setDesc(dev.getDesc());
			
			if(!os.updateById(TABLE, ed, IotDeviceJRso.class, "id", false)) {
				r.setMsg("设置更新失败");
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
	public IPromise<RespJRso<List<IotDeviceJRso>>> myDevices(QueryJRso qry) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<IotDeviceJRso>>>((suc,fail)->{
			RespJRso<List<IotDeviceJRso>> r = new RespJRso<>();
			
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
				List<IotDeviceJRso> list = this.os.query(TABLE, filter, IotDeviceJRso.class,
						qry.getSize(), qry.getCurPage()-1,null,qry.getSortName(),
						IObjectStorage.getOrderVal(qry.getOrder(), 1));
				r.setData(list);
			}
			
			suc.success(r);
		});
	}
	
	private int count(Integer actId, String name) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("name", name);
		qry.put("srcActId", actId);
		return os.count(TABLE, qry);
	}

	private IotDeviceJRso getDeviceByName(Integer actId, String name) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("name", name);
		qry.put("srcActId", actId);
		return os.getOne(TABLE, qry, IotDeviceJRso.class);
	}
	
	private IotDeviceJRso getDeviceByDeviceId(Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("deviceId", deviceId);
		qry.put("srcActId", actId);
		return os.getOne(TABLE, qry, IotDeviceJRso.class);
	}

}
