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
	IPromise<RespJRso<AreaJRso>> getProvinceByName(String proName);
	
	IPromise<RespJRso<AreaJRso>> getCity(String cityCode);
	IPromise<RespJRso<AreaJRso>> getCityByName(String cityName);
	
	IPromise<RespJRso<AreaJRso>> getArea(String areaCode);
	//相同名称的区可能有多个
	IPromise<RespJRso<List<AreaJRso>>> getAreasByName(String name);
	
	IPromise<RespJRso<AreaJRso>> getTown(String areaCode);
	
	IPromise<RespJRso<AreaJRso>> getVillage(String areaCode);
	
	IPromise<RespJRso<String>> getFullPath(String areaCode);
	
}
