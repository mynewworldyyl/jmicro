package org.jmicro.example.test.locker;

import org.jmicro.api.JMicro;
import org.jmicro.api.cache.lock.ILocker;
import org.jmicro.api.cache.lock.ILockerManager;
import org.jmicro.test.JMicroBaseTestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

public class TestRedisLocker extends JMicroBaseTestCase{

    private static final Logger logger = LoggerFactory.getLogger(TestRedisLocker.class);
    
    @Test
	public void testRedisSetMethod() {
    	Jedis j = of.get(Jedis.class);
    	
    	String rst = j.set("testKey", "","NX","EX",1000);

    	System.out.println(rst);
	}
    
    @Test
   	public void testLocker() {
    	ILockerManager lm = of.get(ILockerManager.class);
       	
       ILocker l = lm.getLocker("testLock1");
       if(l.tryLock(5,10*1000)) {
    		System.out.println("Success to get locker");
    		l.unLock();
       } else {
    	   System.out.println("Fail to get locker");
       }
   	}
    
    
    int index = 0;
    
    @Test
   	public void testLockerWithMutilThread() {
    	ILockerManager lm = of.get(ILockerManager.class);
       	
    	new Thread(()->{
    		while(true) {
    			 ILocker l = lm.getLocker("testLock1");
    		       if(l.tryLock(5,10*1000)) {
    		    		index++;
    		    		System.out.println("T1__"+index);
    		    		l.unLock();
    		       }
    		       try {
    					Thread.sleep(10);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    		}
    	}).start();
    	
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	new Thread(()->{
    		while(true) {
   			   ILocker l = lm.getLocker("testLock1");
   		       if(l.tryLock(5,10*1000)) {
   		    		index++;
   		    		System.out.println("T2__"+index);
   		    		l.unLock();
   		       }
   		    try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
   		}
   	}).start();
      
    	
    	new Thread(()->{
    		while(true) {
    			 ILocker l = lm.getLocker("testLock1");
    		       if(l.tryLock(5,10*1000)) {
    		    		index++;
    		    		System.out.println("T3__"+index);
    		    		l.unLock();
    		       }
    		       try {
    					Thread.sleep(10);
    				} catch (InterruptedException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    		}
    	}).start();
    	
    	JMicro.waitForShutdown();
   	}

}
