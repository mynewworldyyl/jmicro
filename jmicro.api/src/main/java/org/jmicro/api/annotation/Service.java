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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:58:31
 */
@Target({TYPE})
@Retention(RUNTIME)
public @interface Service {

	public String value() default "";
	
	public String registry() default Constants.DEFAULT_REGISTRY;
	
	//public int retryCount() default 3;
	
	public String server() default Constants.DEFAULT_SERVER;
	
	public Class<?>[] interfaces() default {};
	
	public String namespace() default Constants.DEFAULT_NAMESPACE;
	
	public String version() default Constants.DEFAULT_VERSION;
	
	public int monitorEnable() default -1;
	
	public int retryInterval() default 500;
	
	//method must can be retry, or 1
	public int retryCnt() default 3;
	
	public int timeout() default 2000;
	
	public int maxFailBeforeDegrade() default 100;
	
	public int maxFailBeforeFusing() default 500;
	
	public String testingArgs() default "";
	
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
