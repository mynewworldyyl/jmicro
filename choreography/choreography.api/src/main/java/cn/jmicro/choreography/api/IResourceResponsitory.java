package cn.jmicro.choreography.api;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IResourceResponsitory {

	List<PackageResource> getResourceList(boolean onlyFinish);
	
	Resp<Integer> addResource(String name, int totalSize);
	
	Resp<Boolean> addResourceData(String name, byte[] data, int blockNum);
	
	Resp<Boolean> deleteResource(String name);
	
	byte[] downResourceData(int downloadId, int blockNum);
	
	Resp<Integer> initDownloadResource(String name);
	
	
}
