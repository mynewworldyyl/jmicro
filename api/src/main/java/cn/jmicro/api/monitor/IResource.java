package cn.jmicro.api.monitor;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadataJRso;

public interface IResource {
	
	public static final String EXP_STR = "expStr";
	
	//可用数量占百分比
	public static final String KEY_LIMIT_AVAI_PERCENT = "limitAvaiPercent";
	
	//可用大小
	public static final String KEY_LIMIT_AVAI_SIZE = "limitAvaiSize";

	ResourceDataJRso getResource(Map<String,Object> params,String expStr);
	
	boolean isEnable();
	
	void setEnable(boolean en);
	
	Map<String,Set<CfgMetadataJRso>> metaData();
	
	String getResourceName();
}
