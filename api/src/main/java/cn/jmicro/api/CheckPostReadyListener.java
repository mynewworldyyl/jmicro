package cn.jmicro.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.objectfactory.IPostFactoryListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.CommonException;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月20日-下午9:36:38
 */
@Component(active=false,value="systemConditionChecker")
public class CheckPostReadyListener implements IPostFactoryListener{

	private final static Logger logger = LoggerFactory.getLogger(CheckPostReadyListener.class);
	
	@Override
	public void afterInit(IObjectFactory of) {
		IDataOperator ddop = of.getByParent(IDataOperator.class).iterator().next();

		if(ddop.exist(Config.ServiceConfigDir+"/active")){
			logger.info("InstanceName: "+Config.getInstanceName() + " is in using,sleep 10s to recheck");
			try {
				//服务关闭后，需要过一定时间后，结点才会删除
				Thread.sleep(10*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(ddop.exist(Config.ServiceConfigDir+"/active")){
				throw new CommonException("InstanceName :"+Config.getInstanceName()+" is using by other server");
			}
		}
		ddop.createNode(Config.ServiceConfigDir+"/active", "", true);
	
		
	}
	
	@Override
	public int runLevel() {
		return 1001;
	}
	
	@Override
	public void preInit(IObjectFactory of) {
	}
	
	
}
