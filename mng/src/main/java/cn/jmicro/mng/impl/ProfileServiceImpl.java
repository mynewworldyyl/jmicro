package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.profile.KV;
import cn.jmicro.api.profile.ProfileManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.api.IProfileService;

@Component
@Service(namespace="mng", version="0.0.1",retryCnt=0,external=true,debugMode=0,showFront=false)
public class ProfileServiceImpl implements IProfileService {

	@Inject
	private IDataOperator op;
	
	@Inject
	private ProfileManager pm;
	
	@Override
	@SMethod(needLogin=true,maxSpeed=5,maxPacketSize=256)
	public Resp<Set<String>> getModuleList() {
		Resp<Set<String>> resp = new Resp<>();
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai == null) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NotLogin");
			resp.setMsg("Account not login!");
			return resp;
		}
		
		String path = ProfileManager.ROOT + "/" + ai.getId();
		if(op.exist(path)) {
			Set<String> modules = op.getChildren(path, false);
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
	public Resp<Set<KV>> getModuleKvs(String module) {

		Resp<Set<KV>> resp = new Resp<>();
		ActInfo ai = JMicroContext.get().getAccount();
		if(ai == null) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NotLogin");
			resp.setMsg("Account not login!");
			return resp;
		}
		
		String mpath = ProfileManager.ROOT + "/" + ai.getId()+"/"+module;
		if(!op.exist(mpath)) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NoData");
			resp.setMsg("No profile to list for you!");
			return resp;
		}
		
		Set<KV> kvs = new HashSet<>();
		Set<String> keys = op.getChildren(mpath, false);
		
		for(String k : keys) {
			String kpath = mpath + "/" + k;
			String data = op.getData(kpath);
			if(!StringUtils.isEmpty(data)) {
				KV kv = JsonUtils.getIns().fromJson(data, KV.class);
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
	public Resp<Map<String, Set<KV>>> getKvs() {
		ActInfo ai = JMicroContext.get().getAccount();
		Resp<Map<String, Set<KV>>> resp = new Resp<>();
		if(ai == null) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NotLogin");
			resp.setMsg("Account not login!");
			return resp;
		}
		
		String path = ProfileManager.ROOT + "/" + ai.getId();
		Set<String> modules = op.getChildren(path, false);
		if(modules == null || modules.isEmpty()) {
			resp.setData(null);
			resp.setCode(1);
			resp.setKey("NoData");
			resp.setMsg("No profile to list for you!");
			return resp;
		}
		
		Map<String, Set<KV>> ls = new HashMap<>();
		
		for(String m : modules) {
			String mpath = path + "/" + m;
			Set<String> keys = op.getChildren(mpath, false);
			if(keys == null || keys.isEmpty()) {
				continue;
			}
			
			Set<KV> kvs = new HashSet<>();
			ls.put(mpath, kvs);
			
			for(String k : keys) {
				String kpath = mpath + "/" + k;
				String data = op.getData(kpath);
				if(StringUtils.isEmpty(data)) {
					continue;
				}
				KV kv = JsonUtils.getIns().fromJson(data, KV.class);
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
	public Resp<Boolean> updateKv(String module,KV kv) {
		ActInfo ai = JMicroContext.get().getAccount();
		Resp<Boolean> resp = new Resp<>();
		pm.setVal(ai.getId(), module, kv.getKey(), kv.getVal());
		resp.setCode(0);
		resp.setData(true);
		return resp;
	}

}
