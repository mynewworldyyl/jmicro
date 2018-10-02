package org.jmicro.api.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Entity {

	private String hello="Hello World";
	private Long v = 222L;
	
	private List<String> list = new ArrayList<>();
	{
		list.add("1");
		list.add("2");
		list.add("3");
	}
	
	//private String[] arrs = {"56","2","67"};
	
	private Map<String,Long> map = new HashMap<>();
	{
		map.put("1",222L);
		map.put("2",333L);
		map.put("3",555L);
	}
	
	@Override
	public String toString() {
		return "hello=" + this.hello +", value = "+ v+",list:"+list+",map: "+map;
	}
	
	
}
