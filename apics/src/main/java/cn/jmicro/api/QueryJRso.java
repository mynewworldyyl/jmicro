package cn.jmicro.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.common.Utils;
import lombok.Data;

@Data
public class QueryJRso {
	
	private int size;
	private int curPage;
	
	private String sortName;
	private String order;//1:增序  2：降序

	private List<QryDefJRso> qryPs = new ArrayList<>();
	
	private Map<String,Object> ps = new HashMap<>();
	
	public static final void appendParam(QueryJRso qry,String key,Map<String,Object> filter) {
		Object v = qry.getPs().get(key);
		if (v != null && !Utils.isEmpty(v.toString().trim())) {
			filter.put(key,v);
		}
	}
}
