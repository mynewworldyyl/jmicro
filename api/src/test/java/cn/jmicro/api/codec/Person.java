package cn.jmicro.api.codec;

import java.util.HashMap;
import java.util.Map;

public class Person {

	private String name="张三";
	private String pwd="sb lh ";
	private Map<String,Long> map = new HashMap<>();
	{
		map.put("1",222L);
		map.put("2",333L);
		map.put("3",555L);
	}
}
