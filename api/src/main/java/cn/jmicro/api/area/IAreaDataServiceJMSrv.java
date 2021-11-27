package cn.jmicro.api.area;

import java.util.List;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.data.AreaJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IAreaDataServiceJMSrv {

	IPromise<RespJRso<List<AreaJRso>>> getSubAreas(String parentCode);
	
	IPromise<RespJRso<List<AreaJRso>>> querySelective(QueryJRso qry);
	
	IPromise<RespJRso<AreaJRso>> getProvince(String areaCode);
	
	IPromise<RespJRso<AreaJRso>> getCity(String areaCode);
	
	IPromise<RespJRso<AreaJRso>> getArea(String areaCode);
	
	IPromise<RespJRso<AreaJRso>> getTown(String areaCode);
	
	IPromise<RespJRso<AreaJRso>> getVillage(String areaCode);
	
	IPromise<RespJRso<String>> getFullPath(String areaCode);
	
}
