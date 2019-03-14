package org.jmicro.api.service;

import java.io.File;
import java.util.List;

import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.common.util.StringUtils;

public class DefaultServiceInstanceNameProvider implements IServiceInstanceNameGenerator {

	private static final String TAG = "expInstanceName";
	private static final String LOCK_FILE = "lock.tmp";
	
	@Override
	public String getInstanceName(IDataOperator dataOperator,Config config) {
		
		String dataDir = config.getString(Constants.LOCAL_DATA_DIR, null);
		
		if(StringUtils.isEmpty(dataDir)) {
			dataDir = System.getProperty("user.dir");
		}
		
		dataDir = dataDir + File.separatorChar + "data";
		
		String insName = null;
		
		File ud = null;
		File dir = new File(dataDir);
		
		if(!dir.exists()) {
			dir.mkdirs();
		}
		
		//优先在本地目录中寻找
		for(File f : dir.listFiles()) {
			if(!f.isDirectory()) {
				continue;
			}
			
			/*File lockFile = new File(f.getAbsolutePath(),LOCK_FILE);
			if(lockFile.exists()) {
				//被同一台机上的不同实例使用
				continue;
			}*/
			
			String path = Config.InstanceDir + "/" + f.getName();
			if(!dataOperator.exist(path)) {
				doTag(dataOperator,f,path);
				insName = f.getName();
				break;
			}
		}
		
		String tag = config.getString(TAG,TAG);
		
		if(insName == null) {
			for(int i = 0; i < Integer.MAX_VALUE ; i++) {
				String name = tag + i;
				ud = new File(dir,name);
				String path = Config.InstanceDir + "/" + name;
				if(!ud.exists() && !dataOperator.exist(path)) {
					doTag(dataOperator,ud.getAbsoluteFile(),path);
					insName = name;
					break;
				}
			}
		}
		
		if(StringUtils.isEmpty(insName)) {
			throw new CommonException("Fail to get instance name");
		}
		
		return insName;
	}
	
	private void doTag(IDataOperator dataOperator,File dir,String path) {
		if(!dir.exists()) {
			dir.mkdirs();
		}
		/*File lf = new File(dir,LOCK_FILE);
		try {
			lf.createNewFile();
		} catch (IOException e) {
			throw new CommonException(lf.getAbsolutePath(),e);
		}
		lf.deleteOnExit();*/
		//本地存在，ZK中不存在,也就是没有虽的机器在使用此目录
		List<String> ips = Utils.getIns().getLocalIPList();
		String data = "0";
		if(!ips.isEmpty()) {
			data = ips.get(0);
		}
		dataOperator.createNode(path, data, true);
	}

}
