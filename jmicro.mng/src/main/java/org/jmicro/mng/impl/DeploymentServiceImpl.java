package org.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.choreography.ChoyConstants;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.choreography.api.Deployment;
import org.jmicro.choreography.api.IDeploymentService;
import org.jmicro.choreography.api.IResourceResponsitory;
import org.jmicro.choreography.api.PackageResource;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(namespace="mng", version="0.0.1",retryCnt=0)
public class DeploymentServiceImpl implements IDeploymentService {

	private final static Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Reference(namespace="rrs",version="*")
	private IResourceResponsitory respo;
	
	private Set<PackageResource> packageResources = new HashSet<>();
	
	@Override
	public Deployment addDeployment(Deployment dep) {
		if(StringUtils.isEmpty(dep.getJarFile())) {
			logger.error("Jar file cannot be null when do add deployment: " +dep.toString());
			return null;
		}
		if(!checkPackageResource(dep.getJarFile())) {
			logger.error("PackageResource [" + dep.getJarFile()+"] not found!");
			return null;
		}
		
		String id = idServer.getStringId(Deployment.class);
		dep.setId(id);
		
		op.createNode(ChoyConstants.DEP_DIR+"/"+id, JsonUtils.getIns().toJson(dep), false);
		
		return dep;
	}

	@Override
	public List<Deployment> getDeploymentList() {
		Set<String> children = op.getChildren(ChoyConstants.DEP_DIR, false);
		if(children == null) {
			return null;
		}
		
		List<Deployment> result = new ArrayList<>();
		
		for(String c : children) {
			String data = op.getData(ChoyConstants.DEP_DIR+"/" + c);
			if(StringUtils.isNotEmpty(data)) {
				Deployment dep = JsonUtils.getIns().fromJson(data, Deployment.class);
				if(dep != null) {
					result.add(dep);
				}
			}
		}
		return result;
	}

	@Override
	public boolean deleteDeployment(int id) {
		op.deleteNode(ChoyConstants.DEP_DIR+"/" + id);
		return true;
	}

	@Override
	public boolean updateDeployment(Deployment dep) {
		
		String data = op.getData(ChoyConstants.DEP_DIR+"/" + dep.getId());
		if(StringUtils.isEmpty(data)) {
			logger.error("Deployment not found when do update: " +dep.toString());
			return false;
		}
		
		if(StringUtils.isEmpty(dep.getJarFile())) {
			logger.error("Jar file cannot be null when do update: " +dep.toString());
			return false;
			
		}
		
		Deployment d = JsonUtils.getIns().fromJson(data, Deployment.class);
		if(!dep.getJarFile().equals(d.getJarFile())) {
			//更新了JarFile，判断更新的JAR是否存在
			if(!checkPackageResource(dep.getJarFile())) {
				logger.error("Jar file cannot not found: " +dep.getJarFile());
				return false;
			}
		}
		
		op.setData(ChoyConstants.DEP_DIR+"/"+dep.getId(), JsonUtils.getIns().toJson(dep));
		return true;
	}
	
	
	private boolean checkPackageResource(String name) {
		Iterator<PackageResource> ite = this.packageResources.iterator();
		while (ite.hasNext()) {
			if (name.equals(ite.next().getName())) {
				return true;
			}
		}

		List<PackageResource> news = respo.getResourceList(true);
		this.packageResources.addAll(news);
		Iterator<PackageResource> it = this.packageResources.iterator();
		while (it.hasNext()) {
			if (name.equals(it.next().getName())) {
				return true;
			}
		}

		return false;
	}

}
