package org.jmicro.choreography.api;

import java.util.List;

public interface IResourceResponsitory {

	List<PackageResource> getResourceList(boolean onlyFinish);
	
	int addResource(String name, int totalSize);
	
	boolean addResourceData(String name, byte[] data, int blockNum);
	
	boolean deleteResource(String name);
	
	byte[] downResourceData(int downloadId, int blockNum);
	
	int initDownloadResource(String name);
	
	
}
