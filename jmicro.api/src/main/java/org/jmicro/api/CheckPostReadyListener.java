package org.jmicro.api;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IPostFactoryReady;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月20日-下午9:36:38
 */
@Component(active=true,value="systemConditionChecker")
public class CheckPostReadyListener implements IPostFactoryReady{

	private final static Logger logger = LoggerFactory.getLogger(CheckPostReadyListener.class);
	
	@Override
	public void ready(IObjectFactory of) {
		IDataOperator ddop = of.getByParent(IDataOperator.class).get(0);
		if(ddop.exist(Config.getRaftBaseDir())) {
			if(ddop.exist(Config.getRaftBaseDir()+"/active")){
				logger.info("InstanceName: "+Config.getInstanceName() + " is in using,sleep 10s to recheck");
				try {
					Thread.sleep(10*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(ddop.exist(Config.ServiceCofigDir+"/active")){
					throw new CommonException("InstanceName :"+Config.getInstanceName()+" is using by other server");
				}
			}
			ddop.createNode(Config.ServiceCofigDir+"/active", "", true);
		}
		
	}

}
