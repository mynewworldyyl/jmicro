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
package cn.jmicro.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import cn.jmicro.api.registry.AsyncConfigJRso;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:57:14
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Async {
	
	//对那个RPC方法的配置
	public String forMethod() default "";
	public boolean enable() default false;
	
	//异步条件timeout:超时转异步， speedlimit:限速转异步
	//留给应用场影决定什么条件下做异步处理
	public String condition() default AsyncConfigJRso.ASYNC_DISABLE;
	
	//结果回调用服务名称
	public String serviceName() default "";
	
	//结果回调用服务名称空间
	public String namespace() default ""; 
	
	//结果回调用服务版本
	public String version() default "";
	
	//结果回调用服务方法名称,参数即是目标方法的返回值
	public String method() default "";
	
	public String paramStr() default "";
	
}
