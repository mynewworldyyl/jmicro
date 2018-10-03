package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(METHOD)
@Retention(RUNTIME)
public @interface SMethod {

	public String value() default "";
	
	public int retryInterval() default 500;
	
	//method must can be retry, or 1
	public int retryCnt() default 3;
	
	public int timeout() default 2000;
	
	public int maxFailBeforeDowngrade() default 100;
	
	public int maxFailBeforeCutdown() default 500;
	
	public String testingArgs() default "";
	
	public int speedLimit() default -1;
	
	/**
	 * max qps
	 */
	public int maxSpeed() default -1;
	
	/**
	 * min qps
	 * real qps less this value will downgrade service
	 */
	public int minSpeed() default -1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	public int avgResponseTime() default -1;
	
}
