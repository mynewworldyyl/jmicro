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
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.iot.IotDeviceVoJRso;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.Base64Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.DeviceFunJRso;
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
	
	@Inject
	private ICache cache;
	
	private static final long expired = 1*24*60*60*1000;//1天过期
	
	@Override
	@SMethod(maxSpeed=1, upSsl=false, encType=0, downSsl=false, needLogin=false, perType=false, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Map<String,Object>>> deviceLogin(Integer actId, String deviceId) {

		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Map<String,Object>>>((suc,fail)->{
			RespJRso<Map<String,Object>> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			Map<String,Object> ps = new HashMap<>();
			if(Utils.isEmpty(deviceId)) {
				r.setMsg("Invalid deviceId");
				suc.success(r);
				return;
			}
			
			IotDeviceJRso dev = this.getDeviceByDeviceId(actId, deviceId);
			if(dev == null) {
				r.setMsg("Invalid device");
				suc.success(r);
				return;
			}
			
			String akey = AccountManager.deviceKey(actId, deviceId);
			String logink = null;
			if(cache.exist(akey)) {
				//账号已经登录，用已经登录的KEY
				logink = cache.get(akey, String.class);
			}
			
			if(Utils.isEmpty(logink)) {
				int seed = HashUtils.FNVHash1(TimeUtils.getCurTime() + "_" + this.idGenerator.getStringId(ActInfoJRso.class));
				if(seed < 0) {
					seed = -seed;
				}
				
				IotDeviceVoJRso vo = new IotDeviceVoJRso();
				vo.setDeviceId(dev.getDeviceId());
				vo.setId(dev.getId());
				vo.setSrcActId(dev.getSrcActId());
				vo.setSrcClientId(dev.getSrcClientId());
				
				logink = AccountManager.deviceKey(seed,deviceId);
				cache.put(logink,vo, expired);
				cache.expire(akey, expired);
			}
			
			ps.put("loginKey", logink);
			ps.put("clientId", dev.getSrcClientId());
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(ps);
			
			//使用这些可用字段存储信息，减小数据传输大小
			//r.setMsg(logink);
			//r.setCurPage(dev.getSrcClientId());
			//r.setPageSize(dev.getSrcActId());
			suc.success(r);
			return;
		});

	}
	
	@Override
	@SMethod(maxSpeed=1, upSsl=true, encType=0, downSsl=true, needLogin=true, perType=false)
	public IPromise<RespJRso<IotDeviceJRso>> bindDevice(IotDeviceJRso dev) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<IotDeviceJRso>>((suc,fail)->{
			RespJRso<IotDeviceJRso> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(dev.getName())) {
				r.setMsg("名称不能为空");
				suc.success(r);
				return;
			}
			
			if(count(act.getId(), dev.getName()) > 0) {
				r.setMsg("设备名称重复");
				suc.success(r);
				return;
			}
			
			//dev.setId(idGenerator.getLongId(IotDeviceJRso.class));
			//dev.setDeviceId(toDeviceId(dev.getId()+TimeUtils.getCurTime()));
			
			dev.setSrcActId(act.getId());
			dev.setSrcClientId(act.getClientId());
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setCreatedTime(TimeUtils.getCurTime());
			dev.setCreatedBy(act.getId());
			dev.setUpdatedBy(act.getId());
			dev.setStatus(STATUS_ENABLE);
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(dev);
			
			suc.success(r);
			return;
		});
		
	}
	
	@Override
	@SMethod(maxSpeed=1, upSsl=true, encType=0, downSsl=true, needLogin=true, perType=false)
	public IPromise<RespJRso<IotDeviceJRso>> addDevice(IotDeviceJRso dev) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<IotDeviceJRso>>((suc,fail)->{
			RespJRso<IotDeviceJRso> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(dev.getName())) {
				r.setMsg("名称不能为空");
				suc.success(r);
				return;
			}
			
			if(count(act.getId(), dev.getName()) > 0) {
				r.setMsg("设备名称重复");
				suc.success(r);
				return;
			}
			
			dev.setId(idGenerator.getIntId(IotDeviceJRso.class));
			dev.setDeviceId(toDeviceId(dev.getId()));
			
			dev.setSrcActId(act.getId());
			dev.setSrcClientId(act.getClientId());
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setCreatedTime(TimeUtils.getCurTime());
			dev.setCreatedBy(act.getId());
			dev.setUpdatedBy(act.getId());
			dev.setStatus(STATUS_ENABLE);
			
			if(Utils.isEmpty(dev.getGrpName())) {
				dev.setGrpName("Default");
			}
			
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
	public IPromise<RespJRso<Boolean>> unbindDevice(String deviceId) {
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
			ed.setType(dev.getType());
			ed.setGrpName(dev.getGrpName());
			
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
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Map<String, Object>>> deviceFunVers() {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Map<String, Object>>>((suc,fail)->{
			RespJRso<Map<String, Object>> r = RespJRso.d(RespJRso.CODE_FAIL,null);
			
			Map<String,Object> qry = new HashMap<>();
			qry.put("srcActId", act.getSrcActId());
			qry.put("deviceId", act.getDeviceId());
			
			List<Map<String,Object>> l = os.getFields(TABLE_FUN, qry, "ver","funName");
			
			Map<String,Object> f2v = new HashMap<>();
			l.forEach(m->{
				f2v.put(m.get("funName").toString(), m.get("ver"));
			});
			
			r.setData(f2v);
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			return;
		});
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Boolean>> updateFun(DeviceFunJRso funs) {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			if(Utils.isEmpty(funs.getFunName())) {
				r.setMsg("方法名称不能为空");
				suc.success(r);
				return;
			}
			
			DeviceFunJRso oldFun = getDeviceFunByName(funs.getFunName(), act.getSrcActId(), act.getDeviceId());
			if(oldFun == null) {
				r = doAddDeviceFun(funs,act);
			} else {
				r = doUpdateDeviceFun(funs,oldFun,false);
			}
			
			suc.success(r);
			return;
		});
	
	}

	private RespJRso<Boolean> doUpdateDeviceFun(DeviceFunJRso fun, DeviceFunJRso oldFun, boolean doDel) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		
		if(doDel) {
			oldFun.setDel(true);
		}else {
			oldFun.setArgs(fun.getArgs());
			oldFun.setFunDesc(fun.getFunDesc());
			oldFun.setDel(false);
			oldFun.setVer(fun.getVer());
			oldFun.setArgs(fun.getArgs());
		}
		
		if(!os.updateById(TABLE_FUN, oldFun, DeviceFunJRso.class, "id", false)) {
			r.setMsg("更新数据失败");
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		
		return r;
	}

	private RespJRso<Boolean> doAddDeviceFun(DeviceFunJRso fun, IotDeviceVoJRso act) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		if(count(fun.getFunName(), act.getSrcActId(), act.getDeviceId()) > 0) {
			r.setMsg("功能名称重复: "+fun.getFunName());
			return r;
		}
		
		fun.setId(idGenerator.getIntId(DeviceFunJRso.class));
		fun.setDeviceId(act.getDeviceId());
		fun.setSrcActId(act.getSrcActId());
		fun.setDel(false);
		
		fun.setUpdatedTime(TimeUtils.getCurTime());
		fun.setUpdatedBy(act.getId());
		fun.setCreatedBy(act.getSrcActId());
		fun.setUpdatedBy(act.getSrcActId());
		
		if(!os.save(TABLE_FUN, fun, DeviceFunJRso.class, false)) {
			r.setMsg("租户创建失败");
			return r;
		}
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Boolean>> delFun(String funName) {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			DeviceFunJRso oldFun = getDeviceFunByName(funName, act.getSrcActId(), act.getDeviceId());
			if(oldFun != null) {
				r = this.doUpdateDeviceFun(null, oldFun, true);
			}
			suc.success(r);
			return;
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

	private DeviceFunJRso getDeviceFunByName(String funName, Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("funName", funName);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.getOne(TABLE_FUN, qry, DeviceFunJRso.class);
	}

	private int count(String funName, Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("funName", funName);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.count(TABLE_FUN, qry);
	}
	
}
