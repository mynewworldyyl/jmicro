package cn.jmicro.ext.iot.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.async.ISuccess;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.ext.iot.Namespace;
import cn.jmicro.ext.iot.service.DeviceFunDefJRso;
import cn.jmicro.ext.iot.service.DeviceFunJRso;
import cn.jmicro.ext.iot.service.DeviceFunOperationJRso;
import cn.jmicro.ext.iot.service.DeviceProductJRso;
import cn.jmicro.ext.iot.service.IDeviceFunJMSrv;
import cn.jmicro.ext.iot.service.IotDeviceJRso;
import cn.jmicro.ext.iot.service.vo.DeviceFunVoJRso;
import cn.jmicro.ext.iot.service.vo.DeviceFunVobJRso;
import cn.jmicro.ext.mongodb.MongodbBaseObjectStorage;
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
	private MongoDatabase mdb;
	
	@Inject
	private DeviceProductServiceJMSrvImpl dpSrv;
	
	@Inject
	private ICache cache;
	
	private static final long expired = 1*24*60*60*1000;//1天过期
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<Map<String,Object>>> deviceFunVers(Map<String,String> devInfo) {
		ActInfoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Map<String,Object>>>((suc,fail)->{
			RespJRso<Map<String,Object>> r = RespJRso.d(RespJRso.CODE_FAIL,null);
			
			IotDeviceJRso ed = getDeviceByDeviceId(act.getDefClientId(), //getSrcActId
					act.getActName()//getDeviceId
					);
			if(ed == null) {
				r.setMsg("设备不存在");
				suc.success(r);
				return;
			}
			
			if(ed.getStatus() == IotDeviceJRso.STATUS_BUND) {
				ed.setStatus(IotDeviceJRso.STATUS_SYNC_INFO);
			}
			
			String key = "master";
			if(devInfo.containsKey(key)) {
				ed.setMaster(Boolean.parseBoolean(devInfo.get(key)));
			}
			
			key = "macAddr";
			if(devInfo.containsKey(key)) {
				ed.setMacAddr(devInfo.get(key).toString());
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
			qry.put("srcActId", act.getDefClientId()); //getSrcActId
			qry.put("deviceId", act.getActName()); //getDeviceId
			
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
	public IPromise<RespJRso<Boolean>> updateOrDelFuns(Integer productId, Set<Integer> adds, Set<Integer> dels) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			
			DeviceProductJRso p = dpSrv.getProcuctByActId(productId, act.getId());
			if(p == null) {
				r.setMsg("产品不存在");
				suc.success(r);
				return;
			}
			
			if((adds == null || adds.isEmpty()) && (dels == null || dels.isEmpty())) {
				r.setMsg("更新数据无效");
				suc.success(r);
				return;
			}
			
			if(!(adds == null || adds.isEmpty())) {
				for(Integer defId : adds) {
					DeviceFunDefJRso def = getDeviceFunDefByDefId(defId);
					if(def == null) {
						r.setMsg("功能定义不存在");
						suc.success(r);
						return;
					}
					
					DeviceFunJRso fun = this.getDeviceFunByProductId(productId, defId);
					if(fun == null) {
						DeviceFunJRso f = new DeviceFunJRso();
						f.setClientId(p.getClientId());
						f.setCreatedBy(act.getId());
						f.setCreatedTime(TimeUtils.getCurTime());
						f.setDefId(defId);
						f.setFunLabel(def.getLabelName());
						f.setId(this.idGenerator.getIntId(DeviceFunJRso.class));
						f.setProductId(p.getId());
						f.setSelfDefArg(def.getSelfDefArg());
						f.setShowFront(def.isShowFront());
						f.setUpdatedBy(act.getId());
						f.setUpdatedTime(TimeUtils.getCurTime());
						f.setVer(def.getVer());
						
						if(!os.save(DeviceFunJRso.TABLE, f, DeviceFunJRso.class,false)) {
							r.setMsg("保存失败");
							suc.success(r);
							return;
						}
					}
				}
			}
			
			if(!(dels == null || dels.isEmpty())) {
				
				List<DeviceFunOperationJRso> dfos = this.getOpByFunId(dels);
				if(dfos != null && dfos.size() > 0) {
					dfos.forEach(e->{
						dels.remove(e.getFunId());
					});
					r.setMsg("部份功能存正在使用用，无法删除");
				}
				
				Document f = new Document(IObjectStorage._ID, new Document("$in",dels));
				f.put("clientId", act.getClientId());
				f.put("productId", productId);
				int cnt = os.deleteByQuery(DeviceFunJRso.TABLE, f);
				if(cnt == 0) {
					r.setMsg("删除失败");
					suc.success(r);
					return;
				}
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			return;
		});
	}
	
	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_USER)
	public IPromise<RespJRso<Boolean>> updateOneFun(DeviceFunJRso f) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			if(f == null) {
				r.setMsg("无数据");
				suc.success(r);
				return;
			}

			if(f.getDefId() == null || f.getDefId() <= 0) {
				r.setMsg("无效更新");
				suc.success(r);
				return;
			}
			
			if(f.getId() == null || f.getId() <= 0) {
				r.setMsg("无效功能ID");
				suc.success(r);
				return;
			}
			
			DeviceProductJRso prd = this.dpSrv.getProcuctByActId(f.getProductId(), act.getId());
			
			if(prd == null) {
				r.setMsg("产品不存在");
				suc.success(r);
				return;
			}
			
			DeviceFunJRso oldFun = getDeviceFunByActId(f.getId(), prd.getId());
			if(oldFun == null) {
				r.setMsg("功能不存在"+f.getId());
				suc.success(r);
				return;
			} 
			
			Boolean showFront = false;
			Map<String,Object> qry = new HashMap<>();
			qry.put(IObjectStorage._ID, f.getDefId());
			Set<Boolean> sfs = os.getDistinctField(DeviceFunDefJRso.TABLE, qry, "showFront",Boolean.class);
			if(sfs != null && !sfs.isEmpty()) {
				showFront = sfs.iterator().next();
			}
			
			f.setClientId(prd.getClientId());//关联租户由其产品租户决定
			oldFun.setShowFront(showFront);
			oldFun.setFunLabel(f.getFunLabel());
			oldFun.setSelfDefArg(f.getSelfDefArg());
			oldFun.setType(f.getType());
			oldFun.setVer(f.getVer());
			
			if(!os.updateById(DeviceFunJRso.TABLE, oldFun, DeviceFunJRso.class, IObjectStorage._ID, false)) {
				r.setMsg("更新失败");
				suc.success(r);
				return;
			}
			
			suc.success(r);
			return;
		});
	
	}
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false)
	public IPromise<RespJRso<List<DeviceFunVobJRso>>> listFuns(QueryJRso qry) {
		
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceFunVobJRso>>>((suc,fail)->{

			RespJRso<List<DeviceFunVobJRso>> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
			
			Document f = getQryMatch(qry,act);
			
			/*if(qry.getPs().containsKey(Constants.CLIENT_ID) && P) {
				f.put("clientId", act.getClientId());
			}else {
				List<Document> ql = new ArrayList<>();
				ql.add(new Document("clientId",act.getClientId()));
				ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
				f.put("$or",ql);
			}*/
			
			int cnt =(int) os.count(DeviceFunJRso.TABLE, f);
			r.setTotal(cnt);
			
			if(cnt > 0) {
				List<DeviceFunJRso> list = this.os.query(DeviceFunJRso.TABLE, f, DeviceFunJRso.class,
						qry.getSize(), qry.getCurPage()-1, new String[] {IObjectStorage._ID,"name"}, qry.getSortName(),
						IObjectStorage.getOrderVal(qry.getOrder(), 1));
			
				List<DeviceFunVobJRso> vos = new ArrayList<>();
				for(DeviceFunJRso df : list) {
					
					DeviceFunDefJRso funDef = this.getDeviceFunDefByDefId(df.getDefId());
					
					DeviceFunVobJRso vo = new DeviceFunVobJRso();
					vo.setClientId(df.getClientId());
					vo.setDefId(df.getDefId());
					vo.setFunDesc(funDef.getFunDesc());
					vo.setFunName(funDef.getFunName());
					vo.setDefId(funDef.getId());
					vo.setFunId(df.getId());
					vo.setLabelName(funDef.getLabelName());
					vo.setProductId(df.getProductId());
					vo.setSelfDefArg(funDef.getSelfDefArg());
					vo.setShowFront(funDef.isShowFront());
					vo.setVer(funDef.getVer());
					
					vos.add(vo);
				}
				
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			
		});
	}
	
	private Document getQryMatch(QueryJRso qry, ActInfoJRso act) {
		
		Document f = new Document();
		if(!PermissionManager.isCurAdmin(Config.getClientId())){
			
			List<Document> ql = new ArrayList<>();
			ql.add(new Document("clientId",act.getClientId()));
			ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
			f.put("$or",ql);
			
			//f.put(Constants.CLIENT_ID, JMicroContext.get().getAccount().getClientId());
		}else if(qry.getPs().get(Constants.CLIENT_ID) != null && PermissionManager.isCurAdmin(act.getClientId())) {
			f.put(Constants.CLIENT_ID, Integer.parseInt(qry.getPs().get(Constants.CLIENT_ID).toString()));
		}
		
		String key = "funLabel";
		if (qry.getPs().containsKey(key)) {
			Document reg = new Document();
			reg.put(key, qry.getPs().get(key).toString());
			f.put("$regex", reg);
		}
		
		key = "defId";
		if (qry.getPs().containsKey(key)) {
			f.put(key, qry.getPs().get(key));
		}
		
		key = "showFront";
		if (qry.getPs().containsKey(key)) {
			f.put(key, qry.getPs().get(key));
		}
		
		key = "selfDefArg";
		if (qry.getPs().containsKey(key)) {
			f.put(key, qry.getPs().get(key));
		}
		return f;
	}
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false)
	public IPromise<RespJRso<List<DeviceFunVobJRso>>> listProductFuns(QueryJRso qry0) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceFunVobJRso>>>((suc,fail)->{
			Integer st = 0;
			Object sto = qry0.getPs().get("selectType");
			if(sto != null) {
				st = Integer.parseInt(sto.toString());
			}
			
			if(st == 0) {
				//查询全部
				listProductFunsAll(act,qry0,suc);
			}else if(st == 1) {
				//查询已经关联产品的功能
				listProductFunsAsso(act,qry0,suc);
			} else {
				//查询未关联产品的功能
				listProductFunsNotAsso(act,qry0,suc);
			}
		});
	}
	
	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false, cacheType=Constants.CACHE_TYPE_PAYLOAD)
	public IPromise<RespJRso<List<DeviceFunVobJRso>>> listProductFunKV(QueryJRso qry) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<List<DeviceFunVobJRso>>>((suc,fail)->{
			if(qry == null || qry.getPs() == null || qry.getPs().isEmpty() || !qry.getPs().containsKey("productId")) {
				RespJRso<List<DeviceFunVobJRso>> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
				r.setMsg("缺少必要参数");
				suc.success(r);
				return;
			}
			//查询已经关联产品的功能
			listProductFunsAsso(act,qry,suc);
		});
	}
	
	private void listProductFunsAsso(ActInfoJRso act, QueryJRso qry0, ISuccess<RespJRso<List<DeviceFunVobJRso>>> suc) {

		RespJRso<List<DeviceFunVobJRso>> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
		
		Document qryMatch = getQryMatch(qry0, act);
		
		Document smMatch = new Document();
		
		String key = "productId";
		Integer pid = Integer.parseInt(qry0.getPs().get(key).toString());
		
		smMatch.put("fun.productId", pid);

		Document lookup = new Document();
		lookup.put("from", DeviceFunJRso.TABLE);
		lookup.put("localField", IObjectStorage._ID);
		lookup.put("foreignField", "defId");
		lookup.put("as", "fun");
		
		Document cntGrp = new Document();
		cntGrp.put("_id", null);
		cntGrp.put("total", new Document("$sum",1));
		
		Document match = new Document("$match", qryMatch);
		Document joinsm = new Document("$lookup", lookup);
		
		Document unwind = new Document("$unwind", "$fun");
		
		Document sort = new Document("$sort", new Document("updatedTime", 1));
		Document skip = new Document("$skip", (qry0.getCurPage()-1)*qry0.getSize());
		Document limit = new Document("$limit", qry0.getSize());
		
		List<DeviceFunVobJRso> rl = new ArrayList<>();
		r.setData(rl);
		
		r.setPageSize(qry0.getSize());
		
		MongoCollection<Document> rpcLogColl = mdb.getCollection(DeviceFunDefJRso.TABLE,Document.class);
		
		List<Document> cntList = new ArrayList<Document>();
		cntList.add(match);
		cntList.add(joinsm);
		cntList.add(unwind);
		if(!smMatch.isEmpty()) {
			cntList.add(new Document("$match", smMatch));
		}
		cntList.add(new Document("$group",cntGrp));
		
		AggregateIterable<Document> countRst = rpcLogColl.aggregate(cntList,Document.class);
		MongoCursor<Document> countCurson = countRst.iterator();
		
		int cnt = 0;
		if(countCurson.hasNext()) {
			Document cd =  countCurson.next();
			cnt = cd.getInteger("total", 0);
			log.info(cd.toJson());
		}
	
		r.setTotal(cnt);
		
		List<Document> aggregateList = new ArrayList<Document>();
		aggregateList.add(match);
		aggregateList.add(joinsm);
		aggregateList.add(unwind);
		
		if(!smMatch.isEmpty()) {
			aggregateList.add(new Document("$match", smMatch));
		}
		aggregateList.add(sort);
		aggregateList.add(skip);
		aggregateList.add(limit);
		AggregateIterable<Document> resultset = rpcLogColl.aggregate(aggregateList,Document.class);
		MongoCursor<Document> cursor = resultset.iterator();
		
		try {
			while(cursor.hasNext()) {
				Document doc =  cursor.next();
				String jo = doc.toJson(MongodbBaseObjectStorage.settings);
				DeviceFunVobJRso mi = JsonUtils.getIns().fromJson(jo, DeviceFunVobJRso.class);
				mi.setDefId(doc.getInteger(IObjectStorage._ID));//defId
				
				rl.add(mi);
				
				/*List smList = doc.get("fun", ArrayList.class);
				if(smList == null || smList.isEmpty()) continue;
				
				Document sm = (Document) smList.get(0);*/
				
				Document sm = doc.get("fun", Document.class);
				
				mi.setClientId(sm.getInteger("clientId"));
				mi.setFunId(sm.getInteger(IObjectStorage._ID));//funId
				mi.setProductId(sm.getInteger("productId"));
				
			}
		} finally {
			cursor.close();
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		suc.success(r);
	}
	
	private void listProductFunsNotAsso(ActInfoJRso act, QueryJRso qry, ISuccess<RespJRso<List<DeviceFunVobJRso>>> suc) {

		RespJRso<List<DeviceFunVobJRso>> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
		
		Document f = new Document();
		List<Document> ql = new ArrayList<>();
		ql.add(new Document("clientId",act.getClientId()));
		ql.add(new Document("clientId",Constants.NO_CLIENT_ID));
		f.put("$or",ql);
		
		List<DeviceFunDefJRso> list = this.os.query(DeviceFunDefJRso.TABLE, f, DeviceFunDefJRso.class,
				Integer.MAX_VALUE, 0, new String[] {IObjectStorage._ID}, null,0);
		
		Integer pid = Integer.parseInt(qry.getPs().get("productId").toString());
		
		List<Integer> defids = new ArrayList<>();
		list.forEach(e->{
			if(!existsFunForProduct(pid, e.getId())) {
				defids.add(e.getId());
			}
		});
		
		f.clear();
		f.put(IObjectStorage._ID, new Document("$in",defids));
		
		List<DeviceFunVobJRso> vos = this.os.query(DeviceFunDefJRso.TABLE, f, DeviceFunVobJRso.class);
		
		vos.forEach(e->e.setDefId(e.getId()));
		
		r.setPageSize(vos.size());
		r.setTotal(vos.size());
		r.setData(vos);
		
		r.setCode(RespJRso.CODE_SUCCESS);
		suc.success(r);

	}
	
	private void listProductFunsAll(ActInfoJRso act, QueryJRso qry, ISuccess<RespJRso<List<DeviceFunVobJRso>>> suc) {

		RespJRso<List<DeviceFunVobJRso>> r = new RespJRso<>(RespJRso.CODE_FAIL,null);
		
		Document f = getQryMatch(qry,act);
		
		int cnt =(int) os.count(DeviceFunDefJRso.TABLE, f);
		r.setTotal(cnt);
		
		Integer pid = Integer.parseInt(qry.getPs().get("productId").toString());
		
		if(cnt > 0) {
			List<DeviceFunVobJRso> list = this.os.query(DeviceFunDefJRso.TABLE, f, DeviceFunVobJRso.class,
					qry.getSize(), qry.getCurPage()-1, null, qry.getSortName(),
					IObjectStorage.getOrderVal(qry.getOrder(), 1));
		
			for(DeviceFunVobJRso df : list) {
				df.setDefId(df.getId());
				DeviceFunJRso fun = this.getDeviceFunByProductId(pid, df.getId());
				if(fun != null) {
					df.setFunId(fun.getId());
					df.setProductId(pid);
				}
			}
			r.setData(list);
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		suc.success(r);
	
	}

	@Override
	@SMethod(maxSpeed=1,upSsl=false,encType=0,downSsl=false,needLogin=true,perType=false)
	public IPromise<RespJRso<Boolean>> addOneFun(DeviceFunJRso f) {
		ActInfoJRso act = JMicroContext.get().getAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			if(f == null) {
				r.setMsg("无数据");
				suc.success(r);
				return;
			}

			if(f.getDefId() == null || f.getDefId() <= 0) {
				r.setMsg("无效更新");
				suc.success(r);
				return;
			}
			
			if(f.getId() == null || f.getId() <= 0) {
				r.setMsg("无效功能ID");
				suc.success(r);
				return;
			}
			
			DeviceProductJRso prd = this.dpSrv.getProcuctByActId(f.getProductId(), act.getId());
			
			if(prd == null) {
				r.setMsg("产品不存在");
				suc.success(r);
				return;
			}
			
			DeviceFunJRso oldFun = getDeviceFunByProductId(f.getProductId(), f.getDefId());
			if(oldFun != null) {
				r.setMsg("产品已经关联引功能"+f.getId());
				suc.success(r);
				return;
			}
			
			f.setClientId(prd.getClientId());//关联租户由其产品租户决定
			
			f.setCreatedBy(act.getId());
			f.setCreatedTime(TimeUtils.getCurTime());
			f.setDel(false);
			f.setId(this.idGenerator.getIntId(DeviceFunJRso.class));
			f.setUpdatedBy(act.getId());
			f.setUpdatedTime(TimeUtils.getCurTime());
			//f.setVer(ver);
			
			if(!os.save(DeviceFunJRso.TABLE, f, DeviceFunJRso.class,false)) {
				r.setMsg("保存失败");
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
			return;
		});
	
	}

	@Override
	@SMethod(maxSpeed=1, needLogin=true, forType=Constants.FOR_TYPE_DEV_USER)
	public IPromise<RespJRso<Boolean>> delFun(Integer funDefId) {
		ActInfoJRso act = JMicroContext.get().getDevAccount();
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = RespJRso.d(RespJRso.CODE_FAIL,false);
			DeviceFunJRso oldFun = getDeviceFunByName(funDefId, act.getDefClientId(), //getSrcActId
					act.getActName());//getDeviceId
			if(oldFun != null) {
				//r = this.doUpdateDeviceFun(null, oldFun, true);
			}
			suc.success(r);
			return;
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
					DeviceFunDefJRso dfd = getDeviceFunDefByDefId(df.getDefId());
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
					DeviceFunDefJRso dfd = getDeviceFunDefByDefId(df.getDefId());
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
	
	private DeviceFunJRso getDeviceFunById(Integer funId, Integer clientId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, funId);
		qry.put("clientId", clientId);
		return os.getOne(DeviceFunJRso.TABLE, qry, DeviceFunJRso.class);
	}
	
	private DeviceFunJRso getDeviceFunByActId(Integer funId, Integer actId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, funId);
		qry.put("createdBy", actId);
		return os.getOne(DeviceFunJRso.TABLE, qry, DeviceFunJRso.class);
	}
	
	private DeviceFunJRso getDeviceFunByProductId(Integer productId,Integer defId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("productId", productId);
		//qry.put("clientId", clientId);
		qry.put("defId", defId);
		return os.getOne(DeviceFunJRso.TABLE, qry, DeviceFunJRso.class);
	}
	
	private boolean existsFunForProduct(Integer productId,Integer defId) {
		Map<String,Object> qry = new HashMap<>();
		qry.put("productId", productId);
		//qry.put("clientId", clientId);
		qry.put("defId", defId);
		return os.count(DeviceFunJRso.TABLE, qry) > 0;
	}

	private DeviceFunDefJRso getDeviceFunDefByDefId(Integer id) {
		Map<String,Object> qry = new HashMap<>();
		qry.put(IObjectStorage._ID, id);
		return os.getOne(DeviceFunDefJRso.TABLE, qry, DeviceFunDefJRso.class);
	}
	
	private List<DeviceFunOperationJRso> getOpByFunId(Set<Integer> funIds) {
		Document f = new Document("funId",new Document("$in",funIds));
		return os.query(DeviceFunOperationJRso.TABLE, f, DeviceFunOperationJRso.class,Integer.MAX_VALUE, 1,
				new String[] {IObjectStorage._ID,"funId"},null, 1);
	}
}
