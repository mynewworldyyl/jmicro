package cn.jmicro.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadataJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.ResourceDataJRso;
import cn.jmicro.api.monitor.ResourceMonitorConfigJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMngResourceServiceJMSrv {

	RespJRso<List<ResourceMonitorConfigJRso>> query();
	
	RespJRso<Boolean> enable(Integer id);
	
	RespJRso<Boolean> update(ResourceMonitorConfigJRso cfg);
	
	RespJRso<Boolean> delete(int id);
	
	RespJRso<ResourceMonitorConfigJRso> add(ResourceMonitorConfigJRso cfg);
	
	RespJRso<Set<CfgMetadataJRso>> getResourceMetadata(String resName);
	
	RespJRso<Map<String,Map<String,Set<CfgMetadataJRso>>>> getInstanceResourceList();
	
	IPromise<RespJRso<Map<String,List<ResourceDataJRso>>>> getInstanceResourceData(ResourceDataReqJRso req);
	
}
