package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.codec.DecoderConstant;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.profile.KVJRso;
import cn.jmicro.api.profile.ProfileManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.Namespace;
import cn.jmicro.mng.api.IProfileServiceJMSrv;

@Component
@Service(version="0.0.1",namespace=Namespace.NS,retryCnt=0,external=true,debugMode=0,showFront=false,logLevel=MC.LOG_NO)
public class ProfileServiceImpl implements IProfileServiceJMSrv {

	@Inject
	private IDataOperator op;
	
	@Inject
	private ProfileManager pm;
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=256)
	public RespJRso<Set<String>> getModuleList() {
		RespJRso<Set<String>> resp = new RespJRso<>();
		ActInfoJRso ai = JMicroContext.get().getAccount();
		if(ai == null) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NotLogin");
			resp.setMsg("Account not login!");
			return resp;
		}
		
		String path = ProfileManager.profileRoot(ai.getClientId());
		path = path.substring(0, path.length()-1);
		if(op.exist(path)) {
			Set<String> modules = op.getChildren(path,false);
			resp.setData(modules);
			resp.setCode(0);
		} else {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NoData");
			resp.setMsg("No profile");
		}
		
		return resp;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=256)
	public RespJRso<Set<KVJRso>> getModuleKvs(String module) {
		RespJRso<Set<KVJRso>> resp = new RespJRso<>();
		ActInfoJRso ai = JMicroContext.get().getAccount();
		if(ai == null) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NotLogin");
			resp.setMsg("Account not login!");
			return resp;
		}
		
		String mpath = ProfileManager.profileRoot(ai.getClientId()) + module;
		if(!op.exist(mpath)) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NoData");
			resp.setMsg("No profile to list for you!");
			return resp;
		}
		
		Set<KVJRso> kvs = new HashSet<>();
		Set<String> keys = op.getChildren(mpath, false);
		
		for(String k : keys) {
			String kpath = mpath + "/" + k;
			String data = op.getData(kpath);
			if(!StringUtils.isEmpty(data)) {
				KVJRso kv = JsonUtils.getIns().fromJson(data, KVJRso.class);
				kv.setKey(k);
				kvs.add(kv);
			}
		}
		
		resp.setData(kvs);
		resp.setCode(0);
		return resp;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=256)
	public RespJRso<Map<String, Set<KVJRso>>> getKvs() {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		RespJRso<Map<String, Set<KVJRso>>> resp = new RespJRso<>();
		if(ai == null) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NotLogin");
			resp.setMsg("Account not login!");
			return resp;
		}
		
		String path = ProfileManager.profileRoot(ai.getClientId());
		Set<String> modules = op.getChildren(path, false);
		if(modules == null || modules.isEmpty()) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NoData");
			resp.setMsg("No profile to list for you!");
			return resp;
		}
		
		Map<String, Set<KVJRso>> ls = new HashMap<>();
		
		for(String m : modules) {
			String mpath = path + "/" + m;
			Set<String> keys = op.getChildren(mpath, false);
			if(keys == null || keys.isEmpty()) {
				continue;
			}
			
			Set<KVJRso> kvs = new HashSet<>();
			ls.put(mpath, kvs);
			
			for(String k : keys) {
				String kpath = mpath + "/" + k;
				String data = op.getData(kpath);
				if(StringUtils.isEmpty(data)) {
					continue;
				}
				KVJRso kv = JsonUtils.getIns().fromJson(data, KVJRso.class);
				kv.setKey(k);
				kvs.add(kv);
			}
			
		}
		
		resp.setData(ls);
		resp.setCode(0);
		return resp;
	}

	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=2048)
	public RespJRso<Boolean> updateKv(String module, KVJRso kv) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		RespJRso<Boolean> resp = new RespJRso<>();
		pm.setVal(ai.getClientId(), module, kv.getKey(), kv.getVal());
		resp.setCode(0);
		resp.setData(true);
		return resp;
	}

	@Override
	public IPromise<RespJRso<Boolean>> addKv(Integer scid, String module, String key, String valStr, Short type) {
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<Boolean>(RespJRso.CODE_SUCCESS,true);
			Object val = DecoderConstant.getValFromString(type, valStr);
			pm.setVal(scid, module, key, val);
			suc.success(r);
		});
	}
	
	@Override
	public IPromise<RespJRso<Boolean>> addModule(Integer scid, String module) {
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<Boolean>(RespJRso.CODE_FAIL,false);
			String mpath = ProfileManager.profileRoot(scid) + module;
			if(op.exist(mpath)) {
				r.setMsg(module +" exist for clientId: " + scid);
				return;
			}
			
			op.createNodeOrSetData(mpath, "", false);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
			
			suc.success(r);
		});
	}

}
