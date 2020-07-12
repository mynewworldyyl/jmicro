package cn.jmicro.example.api.rpc;

import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IGenelizedType {

   /* List<Person> getResourceList(boolean onlyFinish);
	
	int addResource(String name, int totalSize);
	
	boolean addResourceData(String name, byte[] data, int blockNum);
	
	boolean deleteResource(String name);*/
	
	byte[] downResourceData(int downloadId, int blockNum);
	
	/*int initDownloadResource(String name);*/
}
