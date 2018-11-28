/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
/**
 * service method
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:59:13
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SMethod {

	//public String value() default "";
	
	//-1： depend on service config
	//1:enable
	//0: disable
	public int monitorEnable() default -1;
	
	public int retryInterval() default 500;
	//method must can be retry, or 1
	public int retryCnt() default 3;
	public int timeout() default 2000;
	
	/**
	 * 失败时的默认返回值，包括服务熔断失败，降级失败等
	 * 通过Gson能反序列化为方法的返回参数,如果失败，抛出异常，业务通过捕获异常处理失败
	 */
	public String failResponse() default "";
	
	/**
	 * true: 可以熔断，熔断时走快速失败逻辑
	 * @return
	 */
	public boolean breakable() default false;
	
	//continue fail will downgrade service
	public int maxFailBeforeDegrade() default 5;
	
	//continue fail will hung up service, service request will fast fail
	public int maxFailBeforeBreaking() default 10;
	
	//after hung up, will test the service with this arguments
	public String testingArgs() default "";
	
	//0: need response, 1:no need response
	public boolean needResponse() default true;
	
	// StringUtils.isEmpty()=true: not stream, false: stream, one request will got more response
	// if this value is not NULL, the async is always true without check the real value
	// value is the callback component in IOC container created in client
	public boolean stream() default false;
	
	//async return result, server return two time: 
	//first return to confirm receive the request, 
	//second return the result
	//public boolean async() default false;
	
	//limit qps
	//public int speedLimit() default -1;
	
	/**
	 * max qps
	 */
	public String maxSpeed() default "";
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	public int avgResponseTime() default -1;
	
}
