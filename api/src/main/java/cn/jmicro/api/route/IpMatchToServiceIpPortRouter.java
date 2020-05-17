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

import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

/**
 * Any client with specify IP will route to specify server listening on target IP
 * 
 * rule example1: IP to IP
 * 
     {
       "id":"1",
        "type":"ipRouter",
        "enable":true,
        "priority":1,
         "from":{
               "ipPort":"192.168.1.23"
          },
         "to":{
				"ipPort":"192.168.1.25"
  
          }
     }
 *    
 *    
 *    rule example2: IP and PORT to target IP and PORT
 *    
 *     *    {
 *      "id":"2",
 *       "type":"ipRouter",
 *       "enable":true,
 *       "priority":1,
 *        "from":{
 *              "ipPort":'192.168.1.23:9999'
 *         },
 *        "to":{
"				ipPort":'192.168.1.25:8888'
 * 
 *         }
 *    }
 *    
 *        rule example3: IP to target IP and PORT
 *    
 *         {
 *      "id":"3",
 *       "type":"ipRouter",
 *       "enable":true,
 *       "priority":1,
 *        "from":{
 *              "ipPort":'192.168.1.23'
 *         },
 *        "to":{
"				ipPort":'192.168.1.25:8888'
 * 
 *         }
 *    }
 *    
 *    rule example4: IP and PORT to target IP
 *         {
 *       "id":"4",
 *       "type":"ipRouter",
 *       "enable":true,
 *       "priority":1,
 *        "from":{
 *              "ipPort":'192.168.1.23:9999'
 *         },
 *        "to":{
"				ipPort":'192.168.1.25'
 * 
 *         }
 *    }
 * 
 * @author Yulei Ye
 * @date: 2018年11月11日 下午3:56:55
 */
@Component(value="ipRouter",lazy=false)
public class IpMatchToServiceIpPortRouter extends AbstractRouter implements IRouter {

	private final static Logger logger = LoggerFactory.getLogger(IpMatchToServiceIpPortRouter.class);
	
	@Inject
	private RuleManager ruleManager;
	
	@Override
	public RouteRule getRouteRule() {
		Set<RouteRule> rules = ruleManager.getRouteRulesByType(IRouter.TYPE_IP_TO_IP);
		if(rules == null || rules.isEmpty()) {
			return null;
		}
		
		String clientIp;
		if(JMicroContext.callSideProdiver()) {
			//在服务端做路由，取远程IP作为客户端IP,比如API网关
			clientIp =  JMicroContext.get().getString(JMicroContext.REMOTE_HOST, null);
		}else {
			clientIp = JMicroContext.get().getString(JMicroContext.LOCAL_HOST, null);
		}
		
		if(StringUtils.isEmpty(clientIp)) {
			return null;
		}
		
		Iterator<RouteRule> ite = rules.iterator();
		while(ite.hasNext()) {
			RouteRule r = ite.next();
			if(StringUtils.isEmpty(r.getFrom().getIpPort())) {
				//规则定义的源IP和端口是NULL，无效
				logger.error("Invalid rule: {}",JsonUtils.getIns().toJson(r));
				ite.remove();
				if(rules.isEmpty()) {
					return null;
				}
				continue;
			}
			if(!r.getFrom().getIpPort().startsWith(clientIp)) {
				//规则定义的源IP和端口不匹配当前请求客户端
				ite.remove();
				continue;
			}
		}
		return RouteUtils.maxPriorityRule(rules);
	}

	@Override
	public Set<ServiceItem> doRoute(RouteRule rule,Set<ServiceItem> services, String srvName, String method, Class<?>[] args,
			String namespace, String version, String transport) {
		return filterServicesByTargetIpPort(rule,services,transport);
	}

}
