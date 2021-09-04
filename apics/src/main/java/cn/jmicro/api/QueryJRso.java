package cn.jmicro.api;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class QueryJRso {
	
	private int pageSize;
	private int curPage;
	
	private String sortName;
	private int order;//1:增序  2：降序

	private List<QryDefJRso> ps = new ArrayList<>();
	
}
