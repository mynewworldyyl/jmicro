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

import cn.jmicro.api.net.Message;
import cn.jmicro.common.Constants;
@Target({TYPE})
@Retention(RUNTIME)
public @interface Service {

	//public String value() default "";
	
	/**
	 * 服务使用的注册表，如将来同时使用实现ZK或Etcd，此属性目前感觉没必要，一个就足够了，
	 * 真想不明白同时用两种类型注册表有什么用
	 */
	//public String registry() default Constants.DEFAULT_REGISTRY;
	
	/**
	 * 是否可通过api网关使用
	 */
	public boolean external() default false;
	
	/**
	 * 服务配置显示于管理后台前端
	 * @return
	 */
	public boolean showFront() default true;
	
	/**
	 * 服务接口，如果类只实现一个接口，则此值可不填
	 * 一个实现只能存在一个接口作为服务，如果类同时实现了多个接口，则需要在此属性说明那个接口是服务接口
	 */
	public Class<?> infs() default Void.class;
	
	/**
	 * 服务命名空间，服务之间以命名空间作为区别，如出库单服务，入库单服务可以用不同的命名空间相区别，利于服务管理
	 * 客户端使用服务时，可以指定命名空间
	 */
	//public String namespace() default "";

	/**
	 * 服务版本，每个服务接口可以有多个版本，版本格式为 DD.DD.DD,6个数字用英方步点号隔开
	 * 客户端使用服务时，可以指定版本或版本范围
	 */
	public String version() default "";
	
	/**
	 * 开启Debug模式，-1表示未定义，由别的地方定义，如系统环境变量，启动时指定等，0表示不开启，1表示开启
	 * Message包增加额外高试字段，如linkid,msgid,instanceName,method
	 * 开启debug后，其他标志才志作用 {@link Message}
	 */
	public int debugMode() default -1;
	
	/**
	 * 服务是否可监控，-1表示未定义，由别的地方定义，如系统环境变量，启动时指定等，0表示不可监控，1表示可以被监控
	 * 可以被监控的意思是：系统启用埋点日志上报，服务请求开始，服务请求得到OK响应，服务超时，服务异常等埋点
	 */
	public int monitorEnable() default 0;
	
	/**
	 * 服务级的日志启用标识
	 * @return
	 */
	public int logLevel() default 5;
	
	/**
	 * 如果超时了，要间隔多久才重试
	 * @return
	 */
	public int retryInterval() default 500;
	/**
	 * 重试次数
	 */
	//method must can be retry, or 1
	public int retryCnt() default 0;
	
	/**
	 * 请求超时，单位是毫秒
	 */
	public int timeout() default 10000;
	
	/**
	 * 系统检测自动带上的参数 
	 */
	//public String testingArgs() default "";
	
	/**
	 * 服务降级前最大失败次数，如降底QPS，提高响应时间等策略
	 * @return
	 */
	public int maxFailBeforeDegrade() default 100;
	
	/**
	 * 可以接受的最大平均响应时间，如果监控检测到超过此时间，系统可能会被降级或熔断
	 */
	public int avgResponseTime() default -1;
	
	/**
	 * 服务熔断前最大失败次数
	 * @return
	 */
	public int maxFailBeforeFusing() default 500;
	
	/**
	 * 每秒最大支持的最高QPS
	 */
	public int maxSpeed() default 10000; //0无限制,大于0，限速
	
	//统计服务数据基本时长，单位同baseTimeUnit确定  @link SMethod
	public long timeWindow() default 1000*60;
	
	public int slotInterval() default 1000;
	
	/**
	 * 采样统计数据周期，单位由baseTimeUnit确定
	 *   小于0表示由Service注解确定，大于0表示启用
	 * @return
	 */
	public long checkInterval() default -1;
	
	// @link SMethod
	public String baseTimeUnit() default Constants.TIME_MILLISECONDS;
	
	/**
	 * 用于代理处理类，有特殊需求的定制代码才需要设置，如ID请求处理器，使用的RpcRequest及Message不需要ID等特殊实现
	 * @return
	 */
	public String handler() default Constants.DEFAULT_INVOCATION_HANDLER;
	
	/**
	 * 直接从接口获取代理时，拿取的是客户端实例还是服务端实例
	 * 通过Reference注解的肯定是客户端实例
	 * @return
	 */
	public String side() default Constants.SIDE_ANY;
	
	/**
	 * 指定包下的类才可使用
	 * @return
	 */
	public String[] limit2Packages() default {};
	
	public int clientId() default Constants.USE_SYSTEM_CLIENT_ID;
	
	//public int clazzVersion() default 0;
}
