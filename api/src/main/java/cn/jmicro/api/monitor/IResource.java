package cn.jmicro.api.monitor;

import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;

public interface IResource {
	
	//可用数量占百分比
	public static final String KEY_LIMIT_AVAI_PERCENT = "limitAvaiPercent";
	
	//可用大小
	public static final String KEY_LIMIT_AVAI_SIZE = "limitAvaiSize";

	ResourceData getResource(Map<String,Object> params);
	
	boolean isEnable();
	
	void setEnable(boolean en);
	
	Set<CfgMetadata> metaData();
}
