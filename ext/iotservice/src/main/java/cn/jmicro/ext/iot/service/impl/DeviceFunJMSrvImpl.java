package cn.jmicro.ext.iot.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
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
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.DeviceFunDefJRso;
import cn.jmicro.ext.iot.service.DeviceFunJRso;
import cn.jmicro.ext.iot.service.DeviceFunOperationJRso;
import cn.jmicro.ext.iot.service.IDeviceFunJMSrv;
import cn.jmicro.ext.iot.service.IotDeviceJRso;
import cn.jmicro.ext.iot.service.OperationArgJRso;
import cn.jmicro.ext.iot.service.vo.DeviceFunVoJRso;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Service(version="0.0.1",namespace=Namespace.NS,external=true,showFront=false)
public class DeviceFunJMSrvImpl implements IDeviceFunJMSrv {

	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectStorage os;
	
	@Inject
	private ICache cache;
	
	private static final long expired = 1*24*60*60*1000;//1天过期
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Map<String,Object>>> deviceFunVers(Map<String,String> devInfo) {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Map<String,Object>>>((suc,fail)->{
			RespJRso<Map<String,Object>> r = RespJRso.d(RespJRso.CODE_FAIL,null);
			
			IotDeviceJRso ed = getDeviceByDeviceId(act.getSrcActId(), act.getDeviceId());
			if(ed == null) {
				r.setMsg("设备不存在");
				suc.success(r);
				return;
			}
			
			if(ed.getStatus() == IotDeviceJRso.STATUS_BUND) {
				ed.setStatus(IotDeviceJRso.STATUS_SYNC_INFO);
			}
			
			ed.setDevInfo(devInfo);
			
			//设备绑定的物理地址不能变
			/*if(devInfo.get("macAddr") != null) {
				ed.setMacAddr(devInfo.get("macAddr"));
			}*/
			
			ed.setUpdatedTime(TimeUtils.getCurTime());
			if(!os.updateById(IotDeviceJRso.TABLE, ed, IotDeviceJRso.class, "id", false)) {
				r.setMsg("设置更新失败");
				suc.success(r);
				return;
			}
			
			Map<String,Object> qry = new HashMap<>();
			qry.put("srcActId", act.getSrcActId());
			qry.put("deviceId", act.getDeviceId());
			
			List<Map<String,Object>> l = os.getFields(DeviceFunJRso.TABLE, qry, "ver","defId");
			
			Map<String,Object> f2v = new HashMap<>();
			l.forEach(m->{
				f2v.put(m.get("defId").toString(), m.get("ver"));
			});
			
			r.setData(f2v);
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			return;
		});
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Boolean>> updateFunOperationResId(Integer funId, Integer opId, String opName, Integer  resId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			if((opId == null || opId <= 0) && StringUtils.isEmpty(opName)) {
				r.setMsg("操作参数无效");
				suc.success(r);
				return;
			}
			
			DeviceFunJRso oldFun = getDeviceFunById(funId, act.getId());
			if(oldFun == null) {
				r.setMsg("设备不支持此接口");
				suc.success(r);
				return;
			}
			
			if(oldFun.getCtrlOps() == null  || oldFun.getCtrlOps().isEmpty()) {
				r.setMsg("此功能无操作指令");
				suc.success(r);
				return;
			}
			
			DeviceFunOperationJRso oldOp = null;
			
			for(DeviceFunOperationJRso o : oldFun.getCtrlOps()) {
				if(opId != null && opId > 0) {
					if(opId.equals(o.getIdv())) {
						oldOp = o;
						break;
					}
				}else if(!StringUtils.isEmpty(o.getName()) && o.getName().equals(opName)) {
					oldOp = o;
					break;
				}
			}
			
			if(oldOp == null) {
				r.setMsg("更新指令不存在");
				suc.success(r);
				return;
			}
			
			if(oldOp.getResId() == resId) {
				r.setMsg("数值不变，无需更新");
				suc.success(r);
				return;
			}
			
			oldOp.setResId(resId);
			
			if(!os.updateById(DeviceFunJRso.TABLE, oldFun, DeviceFunJRso.class, IObjectStorage.ID, false)) {
				r.setMsg("更新失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
			suc.success(r);
			
		});
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Integer>> addOrUpdateFunOperation(Integer funId, DeviceFunOperationJRso op) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Integer>>((suc,fail)->{
			RespJRso<Integer> r = RespJRso.d(RespJRso.CODE_FAIL,0);
			
			if(op == null) {
				r.setMsg("无效指令");
				suc.success(r);
				return;
			}
			
			if(op.isFromDevice()) {
				r.setMsg("设备预置指令不能修改");
				suc.success(r);
				return;
			}
			
			if( StringUtils.isEmpty(op.getName())) {
				r.setMsg("指令名称不能空");
				suc.success(r);
				return;
			}
			
			DeviceFunJRso oldFun = getDeviceFunById(funId, act.getId());
			if(oldFun == null) {
				r.setMsg("设备不支持此接口");
				suc.success(r);
				return;
			}
			
			if(oldFun.getCtrlOps() != null && !oldFun.getCtrlOps().isEmpty()) {
				for(DeviceFunOperationJRso o : oldFun.getCtrlOps()) {
					if(!o.isFromDevice()) {
						//排除设备预置的指令
						if((op.getIdv() == null || op.getIdv() <= 0)) {
							if(o.getName().equals(op.getName())) {
								//新增指令重名
								r.setMsg("指令重名");
								suc.success(r);
								return;
							}
						}else if(!o.getIdv().equals(op.getIdv()) && o.getName().equals(op.getName())){
							//更新指令重名
							r.setMsg("指令重名");
							suc.success(r);
							return;
						}
					
					}else if(o.getName().equals(op.getName())){
						//更新指令重名
						r.setMsg("跟设备预置指令重名");
						suc.success(r);
						return;
					}
				}
			}
			
			DeviceFunOperationJRso oldOp = null;
			
			if(op.getIdv() != null && op.getIdv() > 0) {
				for(DeviceFunOperationJRso o : oldFun.getCtrlOps()) {
					if(op.getIdv().equals(o.getIdv())) {
						oldOp = o;
						break;
					}
				}
				if(oldOp == null) {
					r.setMsg("更新指令不存在");
					suc.success(r);
					return;
				}
			}
			
			if(oldOp != null) {
				//更新
				oldOp.setArgLen(op.getArgLen());
				oldOp.setArgs(op.getArgs());
				oldOp.setDesc(op.getDesc());
				oldOp.setName(op.getName());
				oldOp.setFromDevice(false);
				r.setData(oldOp.getIdv());
			} else {
				op.setIdv(this.idGenerator.getIntId(DeviceFunOperationJRso.class));
				if(oldFun.getCtrlOps() == null) {
					oldFun.setCtrlOps( new HashSet<>());
				}
				op.setFromDevice(false);
				oldFun.getCtrlOps().add(op);
				r.setData(op.getIdv());
			}
			
			if(!os.updateById(DeviceFunJRso.TABLE, oldFun, DeviceFunJRso.class, IObjectStorage.ID, false)) {
				r.setMsg("更新数据失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			
			return;
		});
	
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Boolean>> delFunOperation(Integer funId, Integer opId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			if(opId <= 0) {
				r.setMsg("操作ID无效");
				suc.success(r);
				return;
			}
			
			DeviceFunJRso oldFun = getDeviceFunById(funId, act.getId());
			if(oldFun == null) {
				r.setMsg("设备不支持此接口");
				suc.success(r);
				return;
			}
			
			if(oldFun.getCtrlOps() == null || oldFun.getCtrlOps().isEmpty()) {
				r.setMsg("设备功能指令列表为空");
				suc.success(r);
				return;
			}
			
			DeviceFunOperationJRso op = null;
			
			for(DeviceFunOperationJRso o : oldFun.getCtrlOps()) {
				if(o.getIdv().equals(opId)) {
					op = o;
					break;
				}
			}
			
			if(op == null) {
				r.setMsg("设备功能指令不存在");
				suc.success(r);
				return;
			}
			
			if(op.isFromDevice()) {
				r.setMsg("不能更新预置指令");
				suc.success(r);
				return;
			}
			
			oldFun.getCtrlOps().remove(op);
			
			if(!os.updateById(DeviceFunJRso.TABLE, oldFun, DeviceFunJRso.class, IObjectStorage.ID, false)) {
				r.setMsg("更新数据失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			
			return;
		});
	
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Boolean>> updateFun(List<DeviceFunJRso> funs) {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			if(funs == null || funs.isEmpty()) {
				r.setMsg("无数据");
				r.setData(false);
				suc.success(r);
				return;
			}
			
			StringBuffer sb = new StringBuffer();
			for(DeviceFunJRso f : funs) {
				if(f.getDefId() == null || f.getDefId() <= 0) {
					sb.append("Invalid defId, id=").append(f.getId())
					.append(", ver=").append(f.getVer())
					.append(", deviceId=").append(f.getDeviceId())
					.append("actId: ").append(f.getSrcActId())
					.append(",ctrlOps=").append(f.getCtrlOps()==null? "":f.getCtrlOps().toString())
					.append("\n");
					log.error(sb.toString());
					continue;
				}
				
				Boolean showFront = false;
				Map<String,Object> qry = new HashMap<>();
				qry.put(IObjectStorage._ID, f.getDefId());
				Set<Boolean> sfs = os.getDistinctField(DeviceFunDefJRso.TABLE, qry, "showFront",Boolean.class);
				if(sfs != null && !sfs.isEmpty()) {
					showFront = sfs.iterator().next();
				}
				
				DeviceFunJRso oldFun = getDeviceFunByName(f.getDefId(), act.getSrcActId(), act.getDeviceId());
				if(oldFun == null) {
					f.setShowFront(showFront);
					r = doAddDevicePreFun(f,act);
				} else {
					oldFun.setShowFront(showFront);
					f.setShowFront(showFront);
					r = doUpdateDevicePreFun(f,oldFun,false);
				}
			}
			
			suc.success(r);
			return;
		});
	
	}

	private RespJRso<Boolean> doUpdateDeviceFun(DeviceFunJRso fun, DeviceFunJRso oldFun, boolean doDel) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		
		if(doDel) {
			oldFun.setDel(true);
		} else {
			final Set<DeviceFunOperationJRso> ctrlOps;
			if(fun.getCtrlOps() == null) {
				ctrlOps = new HashSet<>();
			} else {
				ctrlOps = fun.getCtrlOps();
			}
			
			if(oldFun.getCtrlOps() != null && oldFun.getCtrlOps().size() > 0) {
				oldFun.getCtrlOps().forEach(e->{
					if(e.getIdv() > 0 || e.isFromDevice()) {
						//用户增加的操作
						ctrlOps.add(e);
					}
				});
			}
			
			oldFun.setCtrlOps(ctrlOps);
			oldFun.setSelfDefArg(fun.getSelfDefArg());
			oldFun.setType(fun.getType());
			oldFun.setVer(fun.getVer());
			//oldFun.setDel(false);
		}
		
		if(!os.updateById(DeviceFunJRso.TABLE, oldFun, DeviceFunJRso.class, IObjectStorage.ID, false)) {
			r.setMsg("更新数据失败");
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		
		return r;
	}
	
	//更新设备预置的操作
	private RespJRso<Boolean> doUpdateDevicePreFun(DeviceFunJRso newFun, DeviceFunJRso oldFun, boolean doDel) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		
		if(doDel) {
			oldFun.setDel(true);
		} else {
			final Set<DeviceFunOperationJRso> ctrlOps;
			if(newFun.getCtrlOps() == null) {
				ctrlOps = new HashSet<>();
			} else {
				ctrlOps = newFun.getCtrlOps();
			}
			
			ctrlOps.forEach(e->{
				e.setFromDevice(true);
				e.setIdv(0);
			});
			
			if(oldFun.getCtrlOps() != null && oldFun.getCtrlOps().size() > 0) {
				oldFun.getCtrlOps().forEach(e->{
					if(e.isFromDevice()) {
						ctrlOps.forEach(ne->{
							if(StringUtils.isNotBlank(ne.getName()) && ne.getName().equals(e.getName())) {
								//资源标识由用户设置，不能更新
								ne.setResId(e.getResId());
							}
						});
					} else {
						//用户增加的操作
						ctrlOps.add(e);
					}
				});
			}
			
			oldFun.setCtrlOps(ctrlOps);
			oldFun.setSelfDefArg(newFun.getSelfDefArg());
			oldFun.setType(newFun.getType());
			oldFun.setVer(newFun.getVer());
			//oldFun.setDel(false);
		}
		
		if(!os.updateById(DeviceFunJRso.TABLE, oldFun, DeviceFunJRso.class, IObjectStorage.ID, false)) {
			r.setMsg("更新数据失败");
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		
		return r;
	}

	private RespJRso<Boolean> doAddDeviceFun(DeviceFunJRso fun, IotDeviceVoJRso act) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		if(this.countDeviceImplFun(fun.getDefId(), act.getSrcActId(), act.getDeviceId()) > 0) {
			r.setMsg("功能名称重复: "+fun.getDefId());
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
		
		if(fun.getCtrlOps() != null && fun.getCtrlOps().size() > 0) {
			fun.getCtrlOps().forEach(e->{
				e.setFromDevice(true);
				e.setIdv(0);
			});
		}
		
		if(!os.save(DeviceFunJRso.TABLE, fun, DeviceFunJRso.class, false)) {
			r.setMsg("租户创建失败");
			return r;
		}
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	}
	
	private RespJRso<Boolean> doAddDevicePreFun(DeviceFunJRso fun, IotDeviceVoJRso act) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		if(this.countDeviceImplFun(fun.getDefId(), act.getSrcActId(), act.getDeviceId()) > 0) {
			r.setMsg("功能名称重复: "+fun.getDefId());
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
		
		if(fun.getCtrlOps() != null && fun.getCtrlOps().size() > 0) {
			fun.getCtrlOps().forEach(e->{
				e.setFromDevice(true);
				e.setIdv(0);
			});
		}
		
		if(!os.save(DeviceFunJRso.TABLE, fun, DeviceFunJRso.class, false)) {
			r.setMsg("租户创建失败");
			return r;
		}
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV)
	public IPromise<RespJRso<Boolean>> delFun(Integer funDefId) {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			DeviceFunJRso oldFun = getDeviceFunByName(funDefId, act.getSrcActId(), act.getDeviceId());
			if(oldFun != null) {
				r = this.doUpdateDeviceFun(null, oldFun, true);
			}
			suc.success(r);
			return;
		});
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV, cacheType=Constants.CACHE_TYPE_PAYLOAD)
	public IPromise<RespJRso<String>> deviceCmdByResId(Integer resId) {
		IotDeviceVoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<String>>((suc,fail)->{
			RespJRso<String> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
			
			Map<String,Object> filter = new HashMap<>();
			filter.put("ctrlOps.resId", resId);
			filter.put("srcActId", act.getSrcActId());
			
			DeviceFunJRso f = os.getOne(DeviceFunJRso.TABLE, filter, DeviceFunJRso.class);
			if(f == null) {
				r.setMsg("Fun resid: "+ resId + " not found!");
				suc.success(r);
				return;
			}
			
			DeviceFunOperationJRso op = null;
			for(DeviceFunOperationJRso fop : f.getCtrlOps()) {
				if(fop.getResId() != null && fop.getResId().equals(resId)) {
					op = fop;
					break;
				}
			}
			
			if(op == null) {
				r.setMsg("resid: "+ resId + " not found!");
				suc.success(r);
				return;
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("deviceId=").append(f.getDeviceId())
			.append("&resId=").append(resId);
			
			
			if(op.getArgs() != null || op.getArgs().size() > 0) {
				for(OperationArgJRso a : op.getArgs()) {
					sb.append("&").append(a.getName()).append("=").append(a.getVal());
				}
			}
			
			r.setData(sb.toString());
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
		});
	}

	/**
	 * 前端配置设备接口时，查找当前已经配置的接口列表
	 */
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER, cacheType=Constants.CACHE_TYPE_PAYLOAD)
	public IPromise<RespJRso<List<DeviceFunJRso>>> deviceFuns(String deviceId) {

		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceFunJRso>>>((suc,fail)->{
			RespJRso<List<DeviceFunJRso>> r = new RespJRso<>();
			
			Map<String,Object> filter = new HashMap<>();
			filter.put("srcActId", act.getId());
			filter.put("deviceId", deviceId);
			filter.put("showFront", true);
			
			int cnt =(int) os.count(DeviceFunJRso.TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<DeviceFunJRso> list = this.os.query(DeviceFunJRso.TABLE, filter, DeviceFunJRso.class);
				r.setData(list);
				for(DeviceFunJRso df : list) {
					DeviceFunDefJRso dfd = getDeviceFunDefByFunId(df.getDefId());
					df.setFunLabel(dfd.getLabelName());
				}
			}
			suc.success(r);
		});
	
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER, cacheType=Constants.CACHE_TYPE_PAYLOAD)
	public IPromise<RespJRso<List<DeviceFunVoJRso>>> deviceFrontFunDetail(String deviceId) {

		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceFunVoJRso>>>((suc,fail)->{
			RespJRso<List<DeviceFunVoJRso>> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
			
			if(Utils.isEmpty(deviceId)) {
				r.setMsg("设备ID不能为空");
				suc.success(r);
				return;
			}
			
			Map<String,Object> filter = new HashMap<>();
			filter.put("srcActId", act.getId());
			filter.put("deviceId", deviceId);
			filter.put("showFront", true);
			
			int cnt =(int) os.count(DeviceFunJRso.TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				
				List<DeviceFunVoJRso> rlist = new ArrayList<>();
				List<DeviceFunJRso> list = this.os.query(DeviceFunJRso.TABLE, filter, DeviceFunJRso.class);
				
				for(DeviceFunJRso df : list) {
					DeviceFunVoJRso vo = new DeviceFunVoJRso();
					vo.setDf(df);
					DeviceFunDefJRso dfd = getDeviceFunDefByFunId(df.getDefId());
					vo.setDfd(dfd);
					rlist.add(vo);
				}
				
				r.setData(rlist);
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
		});
	
	}
	
	private IotDeviceJRso getDeviceByDeviceId(Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("deviceId", deviceId);
		qry.put("srcActId", actId);
		return os.getOne(IotDeviceJRso.TABLE, qry, IotDeviceJRso.class);
	}

	private DeviceFunJRso getDeviceFunByName(Integer defId, Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("defId", defId);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.getOne(DeviceFunJRso.TABLE, qry, DeviceFunJRso.class);
	}
	
	private DeviceFunJRso getDeviceFunById(Integer funId, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, funId);
		qry.put("srcActId", actId);
		return os.getOne(DeviceFunJRso.TABLE, qry, DeviceFunJRso.class);
	}

	private int countDeviceImplFun(Integer defId, Integer actId, String deviceId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("defId", defId);
		qry.put("srcActId", actId);
		qry.put("deviceId", deviceId);
		return os.count(DeviceFunJRso.TABLE, qry);
	}
	
	private DeviceFunDefJRso getDeviceFunDefByFunId(Integer id) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, id);
		return os.getOne(DeviceFunDefJRso.TABLE, qry, DeviceFunDefJRso.class);
	}
	
}
