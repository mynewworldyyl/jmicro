package cn.jmicro.mng.api;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
@Service(external=true,showFront=true)
public interface IDeploymentDemoService {

	public Resp<Boolean> redeploy(String code);
	
}
