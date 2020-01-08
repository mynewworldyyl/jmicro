package org.jmicro.example.test.cache;

import org.jmicro.api.JMicro;
import org.jmicro.api.cache.ICache;
import org.jmicro.example.test.vo.ResponseVo;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRedisCache extends JMicroBaseTestCase{

    private static final Logger logger = LoggerFactory.getLogger(TestRedisCache.class);
    
    @Test
	public void testSetGetObject() {
    	ICache c = of.get(ICache.class);
    	
    	ResponseVo val = new ResponseVo();
    	val.setId(1111l);
    	val.setResult("test vo");
    	
    	String key = "aa111";
    	c.put(key, val);
    	
    	ResponseVo vala = c.get(key);
		System.out.println(vala);
		
		c.del(key);

	}
    
    @Test
	public void testRefreshCache() {
    	ICache c = of.get(ICache.class);
    	
    	ResponseVo val = new ResponseVo();
    	val.setId(1111l);
    	val.setResult("test vo");
    	
    	String key = "aa111";
    	
    	c.setReflesher(key, (k)->{
    		System.out.println("Value from source: " + key);
    		return val;
    	});
    	
    	//c.put(key, val);
    	
    	ResponseVo vala = c.get(key);
    	
		System.out.println(vala);
		
		c.del(key);
	}
    
    @Test
	public void testConfigRefreshCache() {
    	ICache c = of.get(ICache.class);
    	
    	ResponseVo val = new ResponseVo();
    	val.setId(1111l);
    	val.setResult("test vo");
    	
    	String key = "aa111";
    	
    	c.setReflesher(key, (k)->{
    		System.out.println("Value from source: " + key);
    		return val;
    	});
    	
    	c.configRefresh(key, 2000, -1, -1);
    	
    	ResponseVo vala = c.get(key);
    	
		System.out.println(vala);
		
		//c.del(key);
		
		JMicro.waitForShutdown();
	}

}
