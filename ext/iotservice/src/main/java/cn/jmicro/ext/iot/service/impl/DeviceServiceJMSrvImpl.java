package cn.jmicro.ext.iot.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

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
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.Base64Utils;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.DeviceActiveSourceJRso;
import cn.jmicro.ext.iot.service.DeviceFunDefJRso;
import cn.jmicro.ext.iot.service.DeviceFunOperationJRso;
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

		//ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Map<String,Object>>>((suc,fail)->{
			RespJRso<Map<String,Object>> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			Map<String,Object> ps = new HashMap<>();
			if(Utils.isEmpty(deviceId)) {
				r.setMsg("Invalid deviceId");
				suc.success(r);
				return;
			}
			
			IotDeviceJRso dev = this.getDeviceByActId(actId, deviceId);
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
				
				ActInfoJRso vo = new ActInfoJRso();
				vo.setId(dev.getId());
				vo.setActName(dev.getDeviceId());
				vo.setClientId(dev.getSrcClientId());
				vo.setDefClientId(dev.getSrcActId());
				vo.setTokenType((byte)(dev.getMaster()?1:0));
				vo.setDev(true);
				//vo.setMaster(dev.getMaster());
				
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
	public IPromise<RespJRso<Boolean>> deleteDevice(Integer did) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.r(RespJRso.CODE_FAIL);
			Map<String,Object> qry = new HashMap<>();
			qry.put(IObjectStorage._ID, did);
			qry.put("createdBy", act.getId());
			qry.put("status", IotDeviceJRso.STATUS_INIT);
			
			if(os.deleteByQuery(IotDeviceJRso.TABLE, qry)==0) {
				r.setMsg("删除失败");
			} else {
				r.setCode(RespJRso.CODE_SUCCESS);
			}
			
			suc.success(r);
		});
		
	}
	
	@Override
	@SMethod(maxSpeed=1, upSsl=true, encType=0, downSsl=true, needLogin=true, perType=false)
	public IPromise<RespJRso<IotDeviceJRso>> bindDevice(String macAddr,  String deviceId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<IotDeviceJRso>>((suc,fail)->{
			RespJRso<IotDeviceJRso> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(macAddr)) {
				r.setMsg("绑定设备信息为空");
				suc.success(r);
				return;
			}
			
			if(countByMacAddr(macAddr) > 0) {
				r.setMsg("设备已经被绑定");
				suc.success(r);
				return;
			}
			
			IotDeviceJRso dev = this.getDeviceByDeviceId(act.getId(), deviceId);
			if(dev == null) {
				r.setMsg("设备不存在");
				suc.success(r);
				return;
			}
			
			if(dev.getStatus() == IotDeviceJRso.STATUS_BUND 
				|| dev.getStatus() == IotDeviceJRso.STATUS_FREEZONE) {
				r.setMsg("设备已经被绑定");
				suc.success(r);
				return;
			}
			
			dev.setMacAddr(macAddr);
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setUpdatedBy(act.getId());
			dev.setStatus(IotDeviceJRso.STATUS_BUND);
			
			if(!os.updateById(IotDeviceJRso.TABLE, dev, IotDeviceJRso.class, "id", false)) {
				r.setMsg("绑定设备失败");
				suc.success(r);
				return;
			}
			
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
			
			if(countDevice(act.getId(), dev.getName()) > 0) {
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
			dev.setStatus(IotDeviceJRso.STATUS_INIT);
			
			if(Utils.isEmpty(dev.getGrpName())) {
				dev.setGrpName("Default");
			}
			
			if(!os.save(IotDeviceJRso.TABLE, dev, IotDeviceJRso.class, false)) {
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
			IotDeviceJRso dev = os.getOne(IotDeviceJRso.TABLE, qry, IotDeviceJRso.class);
			
			if(dev == null) {
				r.setMsg("设备不存在");
				suc.success(r);
				return;
			}
			
			if(!(dev.getStatus() == IotDeviceJRso.STATUS_BUND || dev.getStatus() == IotDeviceJRso.STATUS_SYNC_INFO)) {
				r.setMsg("设备非绑定状态");
				suc.success(r);
				return;
			}
			
			clearDeviceRefData(act,deviceId);
	
			dev.setStatus(IotDeviceJRso.STATUS_UNBUND);
			dev.getDevInfo().clear();
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setMacAddr("");
			
			if(!os.updateById(IotDeviceJRso.TABLE, dev, IotDeviceJRso.class, "id", false)) {
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

	private void clearDeviceRefData(ActInfoJRso act, String deviceId) {
		//删除设备定义的操作
		Map<String,Object> opFilter = new HashMap<>();
		opFilter.put("deviceId", deviceId);
		opFilter.put("by", DeviceFunOperationJRso.SRC_DEVICE);
		opFilter.put("createdBy", act.getId());
		os.deleteByQuery(DeviceFunOperationJRso.TABLE, opFilter);
		
		opFilter.clear();
		//opFilter.put("deviceId", deviceId);
		//opFilter.put("by", DeviceFunOperationJRso.SRC_DEVICE);
		
		List<Document> ql = new ArrayList<>();
		ql.add(new Document("masterDeviceId",deviceId));
		ql.add(new Document("slaveDeviceId",deviceId));
		opFilter.put("$or",ql);
		
		opFilter.put("createdBy", act.getId());
		os.deleteByQuery(DeviceActiveSourceJRso.TABLE, opFilter);
		
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
			
			if(dev.getProductId() == null || dev.getProductId()<=0) {
				r.setMsg("无效产品码");
				suc.success(r);
				return;
			}
			
			//IotDeviceJRso ed = getDeviceByDeviceId(act.getClientId(), dev.getDeviceId());
			
			Map<String,Object> qry = new HashMap<>();
			qry.put("deviceId", dev.getDeviceId());
			
			if(!PermissionManager.isCurAdmin(act.getClientId())) {
				qry.put("srcClientId", act.getClientId());
			}
			
			IotDeviceJRso ed = os.getOne(IotDeviceJRso.TABLE, qry, IotDeviceJRso.class);
			
			if(ed == null) {
				r.setMsg("设备不存在");
				suc.success(r);
				return;
			}
			
			if(!dev.getName().equals(ed.getName())) {
				if(countDevice(act.getClientId(), dev.getName()) > 0) {
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
			ed.setProductId(dev.getProductId());
			ed.setMaster(dev.getMaster());
			
			if(!os.updateById(IotDeviceJRso.TABLE, ed, IotDeviceJRso.class, "id", false)) {
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

	//新增设备要过30秒才能在页面查询到
	@Override
	@SMethod(maxSpeed=1,needLogin=true,perType=false,cacheType=Constants.CACHE_TYPE_PAYLOAD_AND_ACT,cacheExpireTime=30)
	public IPromise<RespJRso<List<IotDeviceJRso>>> myDevices(QueryJRso qry) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<IotDeviceJRso>>>((suc,fail)->{
			RespJRso<List<IotDeviceJRso>> r = new RespJRso<>();
			
			Map<String,Object> filter = new HashMap<>();
			//filter.put("srcActId", act.getId());
			
			if(qry.getPs().containsKey(Constants.CLIENT_ID)) {
				filter.put("clientId", act.getClientId());
			} else {
				if(!PermissionManager.isCurAdmin(act.getClientId())) {
					List<Document> ql = new ArrayList<>();
					ql.add(new Document("clientId",act.getClientId()));
					ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
					filter.put("$or",ql);
				}
			}
			
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
			
			int cnt =(int) os.count(IotDeviceJRso.TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<IotDeviceJRso> list = this.os.query(IotDeviceJRso.TABLE, filter, IotDeviceJRso.class,
						qry.getSize(), qry.getCurPage()-1,null,qry.getSortName(),
						IObjectStorage.getOrderVal(qry.getOrder(), 1));
				r.setData(list);
			}
			
			suc.success(r);
		});
	}
	
	@Override
	public IPromise<RespJRso<Map<String,String>>> myMasterDevices(Boolean master) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Map<String,String>>>((suc,fail)->{
			RespJRso<Map<String,String>> r = new RespJRso<>(RespJRso.CODE_SUCCESS);
			Map<String,String> ps = new HashMap<>();
			r.setData(ps);
			
			Map<String,Object> filter = new HashMap<>();
			filter.put("master", master);
			filter.put("createdBy", act.getId());
			
			List<IotDeviceJRso> list = this.os.query(IotDeviceJRso.TABLE, filter, IotDeviceJRso.class,
					Integer.MAX_VALUE, 0, new String[]{"deviceId","name","master"} ,null, 0);
			
			if(list != null && !list.isEmpty()) {
				for(IotDeviceJRso v : list) {
					ps.put(v.getDeviceId(), v.getName());
				}
			}
			
			suc.success(r);
		});
	}
	
	@Override
	public IPromise<RespJRso<IotDeviceJRso>> getDevices(String deviceId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<IotDeviceJRso>>((suc,fail)->{
			RespJRso<IotDeviceJRso> r = new RespJRso<>(RespJRso.CODE_SUCCESS);
			IotDeviceJRso d = getDeviceByActId(act.getId(),deviceId);
			if(d == null) {
				r.setCode(RespJRso.CODE_FAIL);
				r.setMsg("设备不存在");
			}else {
				r.setData(d);
			}
			suc.success(r);
		});
	}
	
	private int countByMacAddr(String macAddr) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("macAddr", macAddr);
		//qry.put("srcActId", actId);
		return os.count(IotDeviceJRso.TABLE, qry);
	}
	
	private int countDevice(Integer clientId, String deviceName) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("name", deviceName);
		qry.put("srcClientId", clientId);
		return os.count(IotDeviceJRso.TABLE, qry);
	}
	
	private IotDeviceJRso getDeviceByDeviceId(Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("deviceId", deviceId);
		qry.put("createdBy", actId);
		return os.getOne(IotDeviceJRso.TABLE, qry, IotDeviceJRso.class);
	}
	
	private IotDeviceJRso getDeviceByActId(Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("deviceId", deviceId);
		qry.put("createdBy", actId);
		return os.getOne(IotDeviceJRso.TABLE, qry, IotDeviceJRso.class);
	}

	private DeviceFunDefJRso getDeviceFunDefByFunId(Integer id) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, id);
		return os.getOne(DeviceFunDefJRso.TABLE, qry, DeviceFunDefJRso.class);
	}
	
}
