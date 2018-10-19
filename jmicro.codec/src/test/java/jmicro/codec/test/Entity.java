package jmicro.codec.test;

public class Entity {

	private long v=222;
	private String str = null;
	private Object hello = "Hello World";
	
	/*private List<String> list = new ArrayList<>();
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
	}*/
	
	@Override
	public String toString() {
		return "hello=" + this.hello +", value = "/*+ v+",list:"+list+",map: "+map*/;
	}
	
	
}
