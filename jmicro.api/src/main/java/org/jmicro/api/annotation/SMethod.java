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

import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
/**
 * service method
 * @author Yulei Ye
 * @date 2018年10月4日-上午11:59:13
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface SMethod {

	//public String value() default "";
	
	/**
	 * 开启Debug模式，-1表示未定义，由别的地方定义，如系统环境变量，启动时指定等，0表示不开启，1表示开启
	 * Message包增加额外高试字段，如linkid,msgid,instanceName,method
	 * 开启debug后，其他标志才志作用 {@link Message}
	 */
	public int debugMode() default -1;
	
	//-1： depend on service config
	//1: enable
	//0: disable
	public int monitorEnable() default -1;
	
	//dump 下行流，用于下行数问题排查
	public boolean dumpDownStream() default false;
	//dump 上行流，用于上行数问题排查
	public boolean dumpUpStream() default false;
	
	//服务方法级别的日志记录标识，参考monitorEnable说明
	public int loggable() default -1;
	
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
	 * 主要是ServiceCounter使用
	 * 统计数据的基本时间窗口，小于0表示由Service注解确定，大于0表示启用
	 * @return
	 */
	public long timeWindow() default -1;
	
	
	/**
	 * 采样统计数据周期，单位由baseTimeUnit确定
	 * 小于0表示由Service注解确定，大于0表示启用
	 * @return
	 */
	public long checkInterval() default -1;
	
	public int slotSize() default -1;
	
	/**
	 * 空值表示由Service注解确定
	 * @return
	 */
	public String baseTimeUnit() default Constants.TIME_MILLISECONDS;
	
	public SBreakingRule breakingRule() default @SBreakingRule(enable=false,breakTimeInterval=1000,percent=50,checkInterval=80);
	
	/**
	 * 是否可异步调用，实际上此属性不应该在此设置，因为任何RPC方法都可以异步调用，只应该由客户端决定是否做异步调用。
	 * 设置此属性的唯一目的是告诉ServiceLoader要把方法的KEY设置到topic中,以使PubsubServer注册此RPC方法为消息订阅方法 ，这样客户端
	 * 就可以通过  PubsubServer异步调用此方法
	 * 实际上可以在服务运行过程中设置topic属性达到同样的效果，而不管asyncable是什么值，asyncable只算是服务第一次运行过程中的默认行为
	 * @return
	 */
	public boolean asyncable() default false;
	
	//after breaking, will test the service with this arguments
	public String testingArgs() default "";
	
	/**
	 * 时间单位参考：@link org.jmicro.api.registry.ServiceItem
	 * 1分钟内超时数超过总请求数的5%, 则将QPS限速降低10%
	 * 
	 * 值为空时，不启用
	 */
	public String degradeRule() default "1M [7FFFFEF4,7FFFFEF2] 10%";
	
	//0: need response, 1:no need response
	//不需要响应并不等于不需要返回值，但是不需要响应肯定没有返回值，有返回值肯定需要响应
	//不需要响应说明RPC接口调用不需要确保一定成功，允许在极端情况下失败，比如日志提交，消息订阅发送等场景，以提升系统吞吐量
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
	public int maxSpeed() default 0;//无限速
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	public int avgResponseTime() default -1;
	
	
	
}
