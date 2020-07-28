package cn.jmicro.mng.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.mng.api.IDeploymentDemoService;

@Component
@Service(external=true,showFront=true,namespace="mng",version="0.0.1")
public class DeploymentDemoServiceImpl implements IDeploymentDemoService {

	private final static Logger logger = LoggerFactory.getLogger(DeploymentDemoServiceImpl.class);
	
	@Override
	public Resp<Boolean> redeploy(String code) {
		
		return null;
	}

}
