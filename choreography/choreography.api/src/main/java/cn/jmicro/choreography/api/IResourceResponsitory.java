package cn.jmicro.choreography.api;

import java.util.List;
import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IResourceResponsitory {

	Resp<List<PackageResource>> getResourceList(Map<String,Object> qry,int pageSize,int curPage);
	
	Resp<PackageResource> addResource(PackageResource pr);
	
	Resp<PackageResource> getResource(int resId);
	
	Resp<PackageResource> updateResource(PackageResource pr,boolean updateFile);
	
	Resp<Boolean> addResourceData(int id, byte[] data, int blockNum);
	
	Resp<Boolean> deleteResource(int id);
	
	byte[] downResourceData(int downloadId, int blockNum);
	
	Resp<Integer> initDownloadResource(int actId,int resId);
	
	Resp<Map<String,Object>> queryDict();
	
	Resp<List<Map<String,Object>>> waitingResList(int resId);
	
	Resp<List<Map<String,Object>>> dependencyList(int resId);
	
	Resp<List<Map<String,Object>>> getResourceListForDeployment(Map<String,Object> qry);
	
	Resp<PackageResource> getAgentPackage(String version);
	
	Resp<Boolean> clearInvalidDbFile();
	
	Resp<Boolean> clearInvalidResourceFile();
	
	
}
