package org.jmicro.choreography.api;

import java.util.List;

public interface IResourceResponsitory {

	List<PackageResource> getResourceList();
	
	boolean addResource(String name);
	
	boolean addResourceData(String name, byte[] data,long ofsset,int len);
	
	boolean endResource(String name);
	
	boolean deleteResource(String name);
	
}
