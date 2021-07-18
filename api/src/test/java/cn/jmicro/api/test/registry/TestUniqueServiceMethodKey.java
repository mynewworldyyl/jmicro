package cn.jmicro.api.test.registry;

import java.lang.reflect.Method;

import org.junit.Test;

import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;

import cn.jmicro.api.registry.UniqueServiceMethodKeyJRso;

public class TestUniqueServiceMethodKey {
	
	public void args(String test,String tet3, String[] args) {
		
	}

	@Test
	public void testParamsStr() {
		Object[] objs = new Object[1];
		objs[0] = new String[] {"test","3"};
		
		String ps = UniqueServiceMethodKeyJRso.paramsStr(objs);
		
		System.out.println(ps);
	}
	
	@Test
	public void testMethodDesc() throws NoSuchMethodException, SecurityException {
		Method m = TestUniqueServiceMethodKey.class.getMethod("args", 
				new Class[] {String.class,String.class,String[].class});
		String desc = ReflectUtils.getDesc(m);
		System.out.println(desc);
	}
}
