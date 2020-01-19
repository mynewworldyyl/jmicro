package org.jmicro.server;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;

@Component
public class DefaultServerListener implements IServerListener {

	@Inject
	private Config config ;
	
	
	@Override
	public void serverStared(String ip, int port, String transport) {
		IDataOperator dop = config.getDataOperator();
		//String path = Config.getRaftBaseDir();
		String path = Constants.CFG_ROOT +"/"+Config.getInstanceName()+"_ipPort";
		String d = transport+":"+ip+":"+port;
		if(!dop.exist(path)) {
			dop.createNode(path, d, false);
		} else {
			String dd = dop.getData(path);
			if(StringUtils.isNotEmpty(dd)) {
				d = dd+","+d;
			}
			dop.setData(path, d);
		}
		
		String insPath = Config.InstanceDir + "/" + Config.getInstanceName();
		if(!dop.exist(insPath)) {
			dop.createNode(insPath, d, false);
		}else {
			dop.setData(insPath, d);
		}
		
	}

}
