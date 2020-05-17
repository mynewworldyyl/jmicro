package cn.jmicro.server;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component
public class DefaultServerListener implements IServerListener {

	@Inject
	private Config config ;
	
	
	@Override
	public void serverStared(String ip, int port, String transport) {
		IDataOperator dop = config.getDataOperator();
		//String path = Config.getRaftBaseDir();
		String path = Config.InstanceDir +"/"+Config.getInstanceName()+"_ipPort";
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
		
		/*String insPath = Config.InstanceDir + "/" + Config.getInstanceName();
		if(!dop.exist(insPath)) {
			dop.createNode(insPath, d, false);
		}else {
			dop.setData(insPath, d);
		}*/
		
	}

}
