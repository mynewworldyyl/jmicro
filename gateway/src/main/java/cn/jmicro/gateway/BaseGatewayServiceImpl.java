package cn.jmicro.gateway;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.gateway.IBaseGatewayServiceJMSrv;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.profile.KVJRso;
import cn.jmicro.api.profile.ProfileManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.IRegistry;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Service(external=true,version="0.0.1",logLevel=MC.LOG_NO,showFront=false,namespace=Namespace.NS)
@Component
public class BaseGatewayServiceImpl implements IBaseGatewayServiceJMSrv {

	private Logger logger = LoggerFactory.getLogger(BaseGatewayServiceImpl.class);
	 
	@Inject
	private IDataOperator op;
		
	@Inject
	private ApiGatewayHostManager hostManager;
	
	@Inject
	private IRegistry reg;
	
	/*public void ready() {
		reg.addServiceNameListener(IBaseGatewayServiceJMSrv.class.getName(), (type,siKey,item)->{
			if(type == IListener.ADD) {
				ServiceMethodJRso sm = item.getMethod("fnvHash1a");
				logger.info("fnvHash1a key: "+sm.getKey().fullStringKey());
				logger.info("fnvHash1a code: "+sm.getKey().getSnvHash());
			}
		});
	}*/
	
	@Override
	@SMethod(needLogin=false,maxSpeed=1)
	public List<String> getHosts(String protocal) {
		return hostManager.getHosts(protocal);
	}

	@Override
	@SMethod(needLogin=false,maxSpeed=1)
	public String bestHost(String protocal) {
		return hostManager.bestHost(protocal);
	}

	@Override
	@SMethod(needLogin=false,maxSpeed=5)
	public int fnvHash1a(String methodKey) {
		//int code = HashUtils.FNVHash1(methodKey);
		//logger.info("fnvHash1a: " + code + " => " + methodKey);
		return HashUtils.FNVHash1(methodKey);
	}
	
	@Override
	@SMethod(needLogin=false,maxSpeed=1,maxPacketSize=256,cacheType=Constants.CACHE_TYPE_PAYLOAD,cacheExpireTime=5*60)
	public IPromise<RespJRso<Set<KVJRso>>> getSCidModuleKvs(Integer scid, String module) {
		return new Promise<RespJRso<Set<KVJRso>>>((suc,fail)->{
			RespJRso<Set<KVJRso>> resp = new RespJRso<>(RespJRso.CODE_SUCCESS);
			
			String mpath = ProfileManager.profileRoot(scid) + module;
			if(!op.exist(mpath)) {
				resp.setKey("NoData");
				resp.setMsg("No profile to list for you!");
				suc.success(resp);
				return;
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
			suc.success(resp);
			return;
		});
	}
}
