package cn.jmicro.mng.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.mng.Namespace;
import cn.jmicro.mng.api.IDeploymentDemoServiceJMSrv;

@Component
@Service(external=true,showFront=true,version="0.0.1",logLevel=MC.LOG_NO,namespace=Namespace.NS)
public class DeploymentDemoServiceImpl implements IDeploymentDemoServiceJMSrv {

	private final static Logger logger = LoggerFactory.getLogger(DeploymentDemoServiceImpl.class);
	
	@Override
	public RespJRso<Boolean> redeploy(String code) {
		
		return null;
	}

}
