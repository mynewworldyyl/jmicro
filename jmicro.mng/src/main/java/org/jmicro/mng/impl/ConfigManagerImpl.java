package org.jmicro.mng.impl;

import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.mng.ConfigNode;
import org.jmicro.api.mng.IConfigManager;
import org.jmicro.api.raft.IDataOperator;

@Component
@Service(namespace="configManager", version="0.0.1")
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
	public boolean add(String path, String val) {
		try {
			op.createNode(path, val, false);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	
}
