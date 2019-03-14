package org.jmicro.example.provider;

import java.io.File;

import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.service.IServiceInstanceNameGenerator;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;

public class ExampleServiceInstanceNameProvider implements IServiceInstanceNameGenerator {

	private static final String TAG = "JMicroInstance";
	
	@Override
	public String getInstanceName(IDataOperator dataOperator,Config config) {
		
		String dataDir = config.getString(Constants.LOCAL_DATA_DIR, null);
		
		if(StringUtils.isEmpty(dataDir)) {
			dataDir = System.getProperty("user.dir");
		}
		
		String insName = null;
		
		File ud = null;
		for(int i = 0; i < Integer.MAX_VALUE ; i++) {
			String name = TAG + i;
			ud = new File(dataDir + File.pathSeparator + name);
			if(ud.exists()) {
				String path = Config.InstanceDir + "/" + name;
				if(!dataOperator.exist(path)) {
					dataOperator.createNode(path, "0", true);
					insName = name;
				}
			}
		}
		
		if(StringUtils.isEmpty(insName)) {
			throw new CommonException("Fail to get instance name");
		}
		
		return insName;
	}

}
