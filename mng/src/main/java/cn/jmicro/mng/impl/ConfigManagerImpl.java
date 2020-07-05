package cn.jmicro.mng.impl;

import java.util.Set;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ConfigNode;
import cn.jmicro.api.mng.IConfigManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.util.StringUtils;

@Component
@Service(namespace="mng", version="0.0.1",retryCnt=0,external=true,timeout=10000,debugMode=1)
public class ConfigManagerImpl implements IConfigManager {

	@Inject
	private IDataOperator op;
	
	@Override
	public ConfigNode[] getChildren(String path,Boolean getAll) {
		Set<String> clist = op.getChildren(path, false);
		if(clist == null || clist.isEmpty()) {
			return null;
		}
		ConfigNode[] children = new ConfigNode[clist.size()];
		int i = 0;
		for(String p : clist) {
			
			String fp ="/".equals(path) ? "/"+p : path+"/"+p;
			
			String val = op.getData(fp);
			if(val == null) {
				val = "";
			}
			
			ConfigNode cn = new ConfigNode(fp,val,p);
			
			if(getAll) {
				cn.setChildren(this.getChildren(fp,getAll));
			}
			
			children[i++]=cn;
			
		}
		return children;
	}

	@Override
	public boolean update(String path, String val) {
		try {
			op.setData(path, val);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}

	@Override
	public boolean delete(String path) {
		try {
			op.deleteNode(path);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean add(String path, String val,Boolean isDir) {
		try {
			if(isDir && val == null) {
				val = "";
			}else if(StringUtils.isEmpty(val)) {
				return false;
			}
			op.createNodeOrSetData(path, val, false);
			if(isDir) {
				//有子结点才是目录结点，否则作为叶子结点造成不能再往里增加子结点
				op.createNodeOrSetData(path+"/ip", Config.getHost(), false);
			}
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	
}
