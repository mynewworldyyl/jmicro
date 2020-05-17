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
package cn.jmicro.api.route;

import java.util.Set;

import cn.jmicro.api.registry.ServiceItem;

/**
 * 1. 指定源IP请求全部转发到特定IP的特定端口（如果指定端口）服务上；
 * 2. 指定源IP请求全部转发到特定服务上（服务名称，服务名称空间，服务版本）；
 * 3. 客户端请求时指定路由ID，则使用指定路由
 * 4. 客户端上下文参数匹配路由参数
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月11日 上午10:02:52
 */
public interface IRouter {
	
	public static final String TYPE_IP_TO_IP = "sourceIpPortMatch2IpPort";
	
	public static final String TYPE_CONTEXT_PARAMS_MATCH = "contextParamMatch2IpPort";
	
	public static final String TYPE_CLIENT_SERVICE_MATCH = "clientServiceMatch2IpPort";
	
	public static final String TYPE_NONE = "noRouter";
	
	RouteRule getRouteRule();
	
	Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> service,String srvName,String method,Class<?>[] args
			,String namespace,String version,String transport);
}
