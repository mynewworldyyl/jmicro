package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.config.DictManager;
import cn.jmicro.api.i18n.I18NManager;
import cn.jmicro.api.mng.ICommonManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Utils;

@Component
@Service(namespace="mng", version="0.0.1",external=true,debugMode=1,showFront=false)
public class CommonManagerImpl implements ICommonManager {

	@Cfg("/notLonginClientId")
	private int notLonginClientId = 10;
	
	@Inject
	private I18NManager i18nManager;
	
	@Inject
	private DictManager dictManager;
	
	@Inject
	private IDataOperator op;
	
	@Override
	public Map<String, String> getI18NValues(String lang) {
		return i18nManager.values(lang);
	}

	@Override
	public boolean hasPermission(int per) {
		if(JMicroContext.get().hasPermission(per)) {
			return true;
		} else {
			return notLoginPermission(per);
		}
	}
	
	@Override
	public boolean notLoginPermission(int per) {
		return per >= this.notLonginClientId;
	}

	@Override
	public Resp<Map<String,Object>> getDicts(String[] keys,String qry) {
		Map<String,Object> dicts = new HashMap<>();
		for(String k : keys) {
			if(Utils.isEmpty(k)) {
				continue;
			}

			switch(k) {
			case SERVICE_METHODS:
				dicts.put(k, dictManager.serviceMethods(qry));
				break;
			case SERVICE_NAMESPACES:
				dicts.put(k, dictManager.serviceNamespaces(qry));
				break;
			case SERVICE_NAMES:
				dicts.put(k, dictManager.serviceNames(qry));
				break;
			case SERVICE_VERSIONS:
				dicts.put(k, dictManager.serviceVersions(qry));
				break;
			case INSTANCES:
				dicts.put(k, dictManager.serviceInstances(qry));
				break;
			case NAMED_TYPES:
				dicts.put(k, getNamedTypeNameList());
				break;
			case ALL_INSTANCES:
				dicts.put(k, dictManager.serviceInstances(null));
				break;
			default:
				dicts.put(k, this.dictManager.getDict(k));
			}
		
		}
		
		Resp<Map<String,Object>> resp = new Resp<>();
		resp.setData(dicts);
		resp.setCode(Resp.CODE_SUCCESS);
		
		return resp;
	}

	private Set<String> getNamedTypeNameList() {
		return op.getChildren( Config.NamedTypesDir, false);
	}
		
}
