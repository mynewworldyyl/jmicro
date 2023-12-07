package cn.jmicro.ext.iot.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.codec.DecoderConstant;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.DeviceFunDefArgsJRso;
import cn.jmicro.ext.iot.service.DeviceFunDefJRso;
import cn.jmicro.ext.iot.service.IDeviceFunctionServiceJMSrv;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Service(version="0.0.1",namespace=Namespace.NS,external=true,showFront=false)
public class DeviceFunctionDefJMSrvImpl implements IDeviceFunctionServiceJMSrv {

	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectStorage os;
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false,
		cacheType=Constants.CACHE_TYPE_PAYLOAD)
	public IPromise<RespJRso<DeviceFunDefJRso>> getDeviceDef(Integer defId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<DeviceFunDefJRso>>((suc,fail)->{
			RespJRso<DeviceFunDefJRso> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(defId <= 0) {
				r.setMsg("无效接口定义ID");
				suc.success(r);
				return;
			}
			
			DeviceFunDefJRso oldFun = getDeviceFunByFunId(defId);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(oldFun);
			
			suc.success(r);
			return;
		});	
	}
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false)
	public IPromise<RespJRso<List<DeviceFunDefJRso>>> getDefKeyValMap() {
		ActInfoJRso act = JMicroContext.get().getAccount();
		
		return new Promise<RespJRso<List<DeviceFunDefJRso>>>((suc,fail)->{
			RespJRso<List<DeviceFunDefJRso>> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			Map<String,Object> filter = new HashMap<>();
			//filter.put("actId", act.getId());
			
			filter.put("showFront", true);
			//filter.put("selfDefArg", true);
			
			int cnt =(int) os.count(DeviceFunDefJRso.TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<DeviceFunDefJRso> list = this.os.query(DeviceFunDefJRso.TABLE, filter, DeviceFunDefJRso.class,
						1000, 0,new String[] {IObjectStorage._ID, "labelName","selfDefArg","showFront"},null,null);
				r.setData(list);
			}
			
			r.setCode(0);
			suc.success(r);
			return;
		});
		
	}
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false)
	public IPromise<RespJRso<List<DeviceFunDefJRso>>> getDefKeyValMapExcludeProduct(Integer productId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		
		return new Promise<RespJRso<List<DeviceFunDefJRso>>>((suc,fail)->{
			RespJRso<List<DeviceFunDefJRso>> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			Map<String,Object> filter = new HashMap<>();
			
			List<Document> ql = new ArrayList<>();
			ql.add(new Document("clientId",act.getClientId()));
			ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
			filter.put("$or",ql);
			
			int cnt =(int) os.count(DeviceFunDefJRso.TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<DeviceFunDefJRso> list = this.os.query(DeviceFunDefJRso.TABLE, filter, DeviceFunDefJRso.class,
						1000, 0,new String[] {IObjectStorage._ID, "labelName","selfDefArg","showFront"},null,null);
				r.setData(list);
			}
			
			r.setCode(0);
			suc.success(r);
			return;
		});
		
	}
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false)
	public IPromise<RespJRso<DeviceFunDefJRso>> addDeviceDef(DeviceFunDefJRso dev) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<DeviceFunDefJRso>>((suc,fail)->{
			RespJRso<DeviceFunDefJRso> r = RespJRso.r(RespJRso.CODE_FAIL,"");
			
			if(Utils.isEmpty(dev.getFunName())) {
				r.setMsg("功能名称不能为空");
				suc.success(r);
				return;
			}
			
			if(count(act.getId(), dev.getFunName()) > 0) {
				r.setMsg("功能名称重复");
				suc.success(r);
				return;
			}
			
			if(PermissionManager.isCurAdmin(act.getClientId())) {
				if(dev.getClientId() == 0) {
					dev.setClientId(act.getClientId());
				}
			} else {
				dev.setClientId(act.getClientId());
			}
			
			dev.setId(idGenerator.getIntId(DeviceFunDefJRso.class));
			dev.setActId(act.getId());
		
			dev.setUpdatedTime(TimeUtils.getCurTime());
			dev.setCreatedTime(TimeUtils.getCurTime());
			dev.setCreatedBy(act.getId());
			dev.setUpdatedBy(act.getId());
			
			if(Utils.isEmpty(dev.getGrp())) {
				dev.setGrp("Default");
			}
			
			if(!os.save(DeviceFunDefJRso.TABLE, dev, DeviceFunDefJRso.class, false)) {
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

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Boolean>> updateFunDef(DeviceFunDefJRso funs) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			if(Utils.isEmpty(funs.getFunName())) {
				r.setMsg("方法名称不能为空");
				suc.success(r);
				return;
			}
			
			DeviceFunDefJRso oldFun = getDeviceFunByFunId(funs.getId(), act.getId());
			
			if(oldFun == null) {
				
				if(PermissionManager.isCurAdmin(act.getClientId())) {
					if(funs.getClientId() == 0) {
						funs.setClientId(act.getClientId());
					}
				} else {
					funs.setClientId(act.getClientId());
				}
				
				r = doAddDeviceFun(funs,act);
			} else {
				
				if(PermissionManager.isCurAdmin(act.getClientId())) {
					if(oldFun.getClientId() == 0) {
						oldFun.setClientId(act.getClientId());
					}
				} else {
					oldFun.setClientId(act.getClientId());
				}
				
				oldFun.setUpdatedBy(act.getId());
				oldFun.setCreatedBy(act.getId());
				r = doUpdateDeviceFunDef(funs,oldFun,false);
			}
			r.setData(true);
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			return;
		});
	
	}

	private RespJRso<Boolean> doUpdateDeviceFunDef(DeviceFunDefJRso fun, DeviceFunDefJRso oldFun, boolean doDel) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		
		oldFun.setUpdatedTime(TimeUtils.getCurTime());
		
		if(doDel) {
			oldFun.setDel(true);
		} else {
			oldFun.setArgs(fun.getArgs());
			oldFun.setFunDesc(fun.getFunDesc());
			oldFun.setFunName(fun.getFunName());
			oldFun.setLabelName(fun.getLabelName());
			oldFun.setFunType(fun.getFunType());
			//oldFun.setDel(false);
			oldFun.setVer(fun.getVer());
			oldFun.setArgs(fun.getArgs());
			oldFun.setGrp(fun.getGrp());
			oldFun.setShowFront(fun.isShowFront());
			oldFun.setSelfDefArg(fun.getSelfDefArg());
		}
		
		if(!os.updateById(DeviceFunDefJRso.TABLE, oldFun, DeviceFunDefJRso.class, "id", false)) {
			r.setMsg("更新数据失败");
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		
		return r;
	}

	private RespJRso<Boolean> doAddDeviceFun(DeviceFunDefJRso fun, ActInfoJRso act) {
		RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
		if(count(fun.getFunName(), act.getId()) > 0) {
			r.setMsg("功能名称重复: "+fun.getFunName());
			return r;
		}
		
		fun.setId(idGenerator.getIntId(DeviceFunDefJRso.class));
		fun.setActId(act.getId());
		fun.setDel(false);
		
		fun.setUpdatedTime(TimeUtils.getCurTime());
		fun.setUpdatedBy(act.getId());
		fun.setCreatedBy(act.getId());
		
		if(!os.save(DeviceFunDefJRso.TABLE, fun, DeviceFunDefJRso.class, false)) {
			r.setMsg("租户创建失败");
			return r;
		}
		r.setCode(RespJRso.CODE_SUCCESS);
		return r;
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Boolean>> delFunDef(Integer defId) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			DeviceFunDefJRso oldFun = getDeviceByActId(defId, act.getId());
			if(oldFun != null) {
				r = this.doUpdateDeviceFunDef(null, oldFun, true);
			}
			suc.success(r);
			return;
		});
	}
	
	/**
	 * 更新删除修改参数
	 */
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Boolean>> delFunArg(Integer funId, Byte opmodel, DeviceFunDefArgsJRso arg) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			if(opmodel == 1 || opmodel == 2) {
				if(Utils.isEmpty(arg.getName())) {
					r.setMsg("参数名称不能为空");
					suc.success(r);
					return;
				}
				
				if(arg.getType() == 0) {
					arg.setType(DecoderConstant.PREFIX_TYPE_STRINGG);
				}
			}
			
			DeviceFunDefJRso oldFun = getDeviceFunByFunId(funId, act.getId());
			if(oldFun == null) {
				r.setMsg("功能实例不存");
				suc.success(r);
				return;
			}
			
			DeviceFunDefArgsJRso oar = null;
			
			int idx = -1;
			if(oldFun.getArgs() != null && oldFun.getArgs().size() > 0) {
				for(DeviceFunDefArgsJRso a : oldFun.getArgs()) {
					idx++;
					if(a.getName().equals(arg.getName())) {
						oar = a;
						break;
					}
				}
			}
			
			if(opmodel == 1) {
				//更新
				if(oar == null) {
					r.setMsg("参数不存在");
					suc.success(r);
					return;
				}
				oar.setDefVal(arg.getDefVal());
				oar.setDesc(arg.getDesc());
				oar.setIsRequired(arg.getIsRequired());
				oar.setMaxLen(arg.getMaxLen());
				oar.setName(arg.getName());
				oar.setType(arg.getType());
				oar.setLabel(arg.getLabel());
				
			}else if(opmodel == 4) {
				//删除
				if(oar == null) {
					r.setMsg("参数不存在");
					suc.success(r);
					return;
				}
				oldFun.getArgs().remove(idx);
			}else if(opmodel == 2) {
				//新增
				if(oar != null) {
					r.setMsg("参数名称不能重复");
					suc.success(r);
					return;
				}
				oldFun.getArgs().add(arg);
			}
			
			this.updateFunDef(oldFun);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
			suc.success(r);
			return;
		});
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER, cacheType=Constants.CACHE_TYPE_PAYLOAD,cacheExpireTime=3)
	public IPromise<RespJRso<List<DeviceFunDefJRso>>> deviceFunDefs(QueryJRso qry) {

		//ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceFunDefJRso>>>((suc,fail)->{

			RespJRso<List<DeviceFunDefJRso>> r = new RespJRso<>();
			
			Map<String,Object> filter = new HashMap<>();
			//filter.put("actId", act.getId());
			
			String key = "actId"; 
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
			
			key = "clientId"; 
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
			
			key = "funName";
			if (qry.getPs().containsKey(key)) {
				Map<String,Object> typeN = new HashMap<>();
				typeN.put("$regex", qry.getPs().get(key));
				filter.put("funName", typeN);
			}
			
			key = "funDesc";
			if (qry.getPs().containsKey(key)) {
				Map<String,Object> typeN = new HashMap<>();
				typeN.put("$regex", qry.getPs().get(key));
				filter.put("funDesc", typeN);
			}
			
			key = "labelName";
			if (qry.getPs().containsKey(key)) {
				Map<String,Object> typeN = new HashMap<>();
				typeN.put("$regex", qry.getPs().get(key));
				filter.put("labelName", typeN);
			}
			
			key = "grp";
			if (qry.getPs().containsKey(key)) {
				Map<String,Object> typeN = new HashMap<>();
				typeN.put("$regex", qry.getPs().get(key));
				filter.put("grp", typeN);
			}
			
			key = "funType";
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
			
			key = "ver";
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
			
			key = "showFront";
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
			
			filter.put("del", false);
			
			int cnt =(int) os.count(DeviceFunDefJRso.TABLE, filter);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<DeviceFunDefJRso> list = this.os.query(DeviceFunDefJRso.TABLE, filter,
					DeviceFunDefJRso.class,qry.getSize(), qry.getCurPage()-1,null, 
					qry.getSortName(),IObjectStorage.getOrderVal(qry.getOrder(), 1));
				r.setData(list);
			}
			
			suc.success(r);
		
		});
	
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, cacheType=Constants.CACHE_TYPE_ACCOUNT)
	public IPromise<RespJRso<Set<String>>> groupList() {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Set<String>>>((suc,fail)->{
			RespJRso<Set<String>> r = RespJRso.d(RespJRso.CODE_FAIL,null);
			
			Map<String,Object> qry = new HashMap<>();
			qry.put("actId", act.getId());
			qry.put("del", false);
			
			Set<String> f2v = os.getDistinctField(DeviceFunDefJRso.TABLE, qry, "grp",String.class);
			if(f2v == null || f2v.isEmpty()) {
				f2v = new HashSet<>();
				f2v.add(DeviceFunDefJRso.DEF_GROUP);
			}

			r.setData(f2v);
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			return;
		});
	}

	private int count(Integer actId, String name) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("funName", name);
		qry.put("actId", actId);
		qry.put("del", false);
		return os.count(DeviceFunDefJRso.TABLE, qry);
	}

	private DeviceFunDefJRso getDeviceByActId(Integer defId, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, defId);
		qry.put("actId", actId);
		return os.getOne(DeviceFunDefJRso.TABLE, qry, DeviceFunDefJRso.class);
	}
	
	private DeviceFunDefJRso getDeviceFunByFunId(Integer id, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, id);
		qry.put("actId", actId);
		return os.getOne(DeviceFunDefJRso.TABLE, qry, DeviceFunDefJRso.class);
	}
	
	private DeviceFunDefJRso getDeviceFunByFunId(Integer id) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, id);
		return os.getOne(DeviceFunDefJRso.TABLE, qry, DeviceFunDefJRso.class);
	}

	private int count(String funName, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("funName", funName);
		qry.put("actId", actId);
		qry.put("del", false);
		return os.count(DeviceFunDefJRso.TABLE, qry);
	}
	
}
