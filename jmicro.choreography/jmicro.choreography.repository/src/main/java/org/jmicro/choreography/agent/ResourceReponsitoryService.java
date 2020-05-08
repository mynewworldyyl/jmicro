package org.jmicro.choreography.agent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.choreography.api.IResourceResponsitory;
import org.jmicro.choreography.api.PackageResource;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(namespace="rrs",version="0.0.1",timeout=5000,retryCnt=0)
public class ResourceReponsitoryService implements IResourceResponsitory{

	private static final Logger LOG = LoggerFactory.getLogger(ResourceReponsitoryService.class);
	
	@Cfg(value="/ResourceReponsitoryService/dataDir", defGlobal=true)
	private String resDir = System.getProperty("user.dir") + "/resDataDir";
	
	private String zkDir = Config.BASE_DIR + "/resDataDir";
	
	private File dir = null;
	
	private Map<String,FileOutputStream> outs = new HashMap<>();
	
	@Inject
	private IDataOperator op;
	
	public void ready() {
		dir = new File(resDir);
	}
	
	@Override
	public List<PackageResource> getResourceList() {
		List<PackageResource> l = new ArrayList<>();
		File[] fs = dir.listFiles();
		
		for(File f : fs) {
			if(f.isDirectory()) {
				continue;
			}
			PackageResource rr = new PackageResource();
			rr.setName(f.getName());
			rr.setSize(f.length());
			
			String zkFilePath = zkDir +"/"+ f.getName();
			if(op.exist(zkFilePath)) {
				PackageResource zkrr = JsonUtils.getIns().fromJson(op.getData(zkFilePath),
						PackageResource.class);
				if(zkrr != null) {
					rr.setFinish(false);
					rr.setSize(zkrr.getSize());
					rr.setStatus(zkrr.getStatus());
					rr.setOffset(zkrr.getOffset());
				}
			}
			
			l.add(rr);
		}
		
		return l;
	}
	
	@Override
	public boolean deleteResource(String name) {
		File res = new File(resDir+"/"+name);
		if(res.exists()) {
			res.delete();
		}
		
		String zkFilePath = zkDir +"/"+ name;
		if(op.exist(zkFilePath)) {
			op.deleteNode(zkFilePath);
		}
		
		return true;
	}

	@Override
	public boolean addResource(String name) {
		File res = new File(resDir+"/"+name);
		if(res.exists()) {
			return false;
		}
		
		PackageResource rr = new PackageResource();
		rr.setName(name);
		rr.setSize(0);
		rr.setFinish(false);
		rr.setOffset(0);
		//rr.setStatus("init");
		
		String zkFilePath = zkDir +"/"+ name;
		if(op.exist(zkFilePath)) {
			op.deleteNode(zkFilePath);
		}
		op.createNode(zkFilePath, JsonUtils.getIns().toJson(rr), false);
		
		try {
			FileOutputStream os = new FileOutputStream(res);
			this.outs.put(name, os);
		} catch (FileNotFoundException e) {
			LOG.error("addResouce "+name, e);
			op.deleteNode(zkFilePath);
			return false;
		}
		
		return true;
	}

	@Override
	public boolean addResourceData(String name, byte[] data, long ofset, int len) {
		
		FileOutputStream os = this.outs.get(name);
		if(os != null) {
			try {
				os.write(data);
			} catch (IOException e) {
				LOG.error("addResourceData "+name, e);
				return false;
			}
		}
		
		String zkFilePath = zkDir +"/"+ name;
		PackageResource zkrr = JsonUtils.getIns().fromJson(op.getData(zkFilePath),
				PackageResource.class);
		zkrr.setOffset(ofset + len+1);
		//zkrr.setStatus("write");
		op.setData(zkFilePath, JsonUtils.getIns().toJson(zkrr));
		
		return false;
	}

	@Override
	public boolean endResource(String name) {
		FileOutputStream os = this.outs.get(name);
		if(os != null) {
			try {
				os.close();
				this.outs.remove(name);
			} catch (IOException e) {
				LOG.error("addResourceData "+name, e);
				return false;
			}
		}
		
		String zkFilePath = zkDir +"/"+ name;
		op.deleteNode(zkFilePath);
		
		return true;
	}

}
