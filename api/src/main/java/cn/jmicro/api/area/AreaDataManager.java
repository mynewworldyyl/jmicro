package cn.jmicro.api.area;

import java.util.List;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.data.AreaJRso;
import cn.jmicro.common.Utils;

@Component
public class AreaDataManager implements IAreaDataServiceJMSrv {

	@Reference
	private IAreaDataServiceJMSrv ads;
	
	@Override
	public IPromise<RespJRso<AreaJRso>> getProvinceByName(String proName) {
		check();
		return ads.getProvinceByName(proName);
	}

	@Override
	public IPromise<RespJRso<AreaJRso>> getCityByName(String cityName) {
		check();
		return ads.getCityByName(cityName);
	}

	@Override
	public IPromise<RespJRso<List<AreaJRso>>> getAreasByName(String name) {
		check();
		return ads.getAreasByName(name);
	}

	@Override
	public IPromise<RespJRso<List<AreaJRso>>> querySelective(QueryJRso qry) {
		check();
		return ads.querySelective(qry);
	}
	
	public RespJRso<List<AreaJRso>> querySelectiveSync(QueryJRso qry) {
		check();
		return ads.querySelective(qry).getResult();
	}

	@Override
	public IPromise<RespJRso<List<AreaJRso>>> getSubAreas(String parentCode) {
		check();
		return ads.getSubAreas(parentCode);
	}

	@Override
	public IPromise<RespJRso<AreaJRso>> getProvince(String areaCode) {
		check();
		return ads.getProvince(areaCode);
	}

	@Override
	public IPromise<RespJRso<AreaJRso>> getCity(String areaCode) {
		check();
		return ads.getCity(areaCode);
	}

	@Override
	public IPromise<RespJRso<AreaJRso>> getArea(String areaCode) {
		check();
		return ads.getArea(areaCode);
	}

	@Override
	public IPromise<RespJRso<AreaJRso>> getTown(String areaCode) {
		check();
		return ads.getTown(areaCode);
	}

	@Override
	public IPromise<RespJRso<AreaJRso>> getVillage(String areaCode) {
		check();
		return ads.getVillage(areaCode);
	}

	@Override
	public IPromise<RespJRso<String>> getFullPath(String areaCode) {
		check();
		return ads.getFullPath(areaCode);
	}
	
	public RespJRso<List<AreaJRso>> getSubAreasSync(String parentCode) {
		check();
		return ads.getSubAreas(parentCode).getResult();
	}

	public RespJRso<AreaJRso> getProvinceSync(String areaCode) {
		check();
		return ads.getProvince(areaCode).getResult();
	}

	public RespJRso<AreaJRso> getCitySync(String areaCode) {
		check();
		return ads.getCity(areaCode).getResult();
	}

	public RespJRso<AreaJRso> getAreaSync(String areaCode) {
		check();
		return ads.getArea(areaCode).getResult();
	}

	public RespJRso<AreaJRso> getTownSync(String areaCode) {
		check();
		return ads.getTown(areaCode).getResult();
	}

	public RespJRso<AreaJRso> getVillageSync(String areaCode) {
		check();
		return ads.getVillage(areaCode).getResult();
	}

	public RespJRso<String> getFullPathSync(String areaCode) {
		check();
		IPromise<RespJRso<String>> pr =  ads.getFullPath(areaCode);
		return pr.getResult();
	}
	
	public boolean isValidProvince(String areaCode) {
		if(Utils.isEmpty(areaCode)) return false;
		if(areaCode.length() < 2) return false;
		if(AreaJRso.V00.equals(areaCode.subSequence(0, 2))) return false;
		return true;
	}
	
	public boolean isValidCity(String areaCode) {
		if(!isValidProvince(areaCode)) return false;
		if(areaCode.length() < 4) return false;
		if(AreaJRso.V00.equals(areaCode.subSequence(2, 4))) return false;
		return true;
	}
	
	public boolean isValidArea(String areaCode) {
		if(!isValidCity(areaCode)) return false;
		if(areaCode.length() < 6) return false;
		if(AreaJRso.V00.equals(areaCode.subSequence(4, 6))) return false;
		return true;
	}
	
	public boolean isValidTown(String areaCode) {
		if(!isValidArea(areaCode)) return false;
		if(areaCode.length() < 9)  return false;
		if(AreaJRso.V000.equals(areaCode.subSequence(6, 9))) return false;
		return true;
	}
	
	public boolean isValidVillage(String areaCode) {
		if(!isValidTown(areaCode)) return false;
		if(areaCode.length() < 12) return false;
		if(AreaJRso.V000.equals(areaCode.subSequence(9, 12))) return false;
		return true;
	}
	
	private void check() {
		if(ads == null) throw new NullPointerException("IAreaDataServiceJMSrv not found");
	}
	
}
