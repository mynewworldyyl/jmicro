package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.config.DictManager;
import cn.jmicro.api.i18n.I18NManager;
import cn.jmicro.api.mng.ICommonManagerJMSrv;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.Utils;

@Component
@Service(version="0.0.1",external=true,debugMode=1,showFront=false,logLevel=MC.LOG_NO)
public class CommonManagerImpl implements ICommonManagerJMSrv {

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
	public RespJRso<Map<String,Object>> getDicts(String[] keys,String qry) {
		Map<String,Object> dicts = new HashMap<>();
		ActInfoJRso ai = JMicroContext.get().getAccount();
		for(String k : keys) {
			if(Utils.isEmpty(k)) {
				continue;
			}

			switch(k) {
			case SERVICE_METHODS:
				dicts.put(k, dictManager.serviceMethods(qry,ai.getClientId()));
				break;
			case SERVICE_NAMESPACES:
				dicts.put(k, dictManager.serviceNamespaces(qry,ai.getClientId()));
				break;
			case SERVICE_NAMES:
				dicts.put(k, dictManager.serviceNames(qry,ai.getClientId()));
				break;
			case SERVICE_VERSIONS:
				dicts.put(k, dictManager.serviceVersions(qry,ai.getClientId()));
				break;
			case INSTANCES:
				dicts.put(k, dictManager.serviceInstances(qry,ai.getClientId()));
				break;
			case NAMED_TYPES:
				dicts.put(k, getNamedTypeNameList());
				break;
			case ALL_INSTANCES:
				dicts.put(k, dictManager.serviceInstances(null,ai.getClientId()));
				break;
			case MONITOR_RESOURCE_NAMES:
				dicts.put(k, dictManager.resourceNames());
				break;
			default:
				dicts.put(k, this.dictManager.getDict(k));
			}
		
		}
		
		RespJRso<Map<String,Object>> resp = new RespJRso<>();
		resp.setData(dicts);
		resp.setCode(RespJRso.CODE_SUCCESS);
		
		return resp;
	}

	private Set<String> getNamedTypeNameList() {
		return op.getChildren( Config.getRaftBasePath(Config.NamedTypesDir), false);
	}
		
}
