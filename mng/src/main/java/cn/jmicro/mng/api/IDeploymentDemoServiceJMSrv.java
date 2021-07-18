package cn.jmicro.mng.api;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
@Service(external=true,showFront=true)
public interface IDeploymentDemoServiceJMSrv {

	public RespJRso<Boolean> redeploy(String code);
	
}
