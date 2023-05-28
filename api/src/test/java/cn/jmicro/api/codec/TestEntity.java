package cn.jmicro.api.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestEntity {
	
	public byte[] data = new byte[] {1,2,3};
	
	private long v=222;
	private String str = null;
	private Object hello = "Hello World";
	
	//public Object types = new Integer[] {1,2,3};
	
	private List<Person> persons = new ArrayList<Person>();
	{
		persons.add(new Person());
	}
	
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
		return "hello=" + this.data +", value = "+ v+",list:"+list+",map: "+map;
	}
	
	
}
