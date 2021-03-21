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
package cn.jmicro.api.route.impl;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.route.AbstractRouter;
import cn.jmicro.api.route.IRouter;
import cn.jmicro.api.route.RouteRule;
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
@Component(value=RouteRule.TYPE_FROM_IP_ROUTER,lazy=false)
public class IpMatchToServiceIpPortRouter extends AbstractRouter implements IRouter {

	public IpMatchToServiceIpPortRouter() {
		super(RouteRule.TYPE_FROM_IP_ROUTER);
	}

	@Override
	protected boolean accept(RouteRule r) {
		String clientIp = null;
		if(JMicroContext.isCallSideService()) {
			//在服务端做路由，取远程IP作为客户端IP,比如API网关
			clientIp =  JMicroContext.get().getString(JMicroContext.REMOTE_HOST, null);
		}else {
			clientIp = JMicroContext.get().getString(JMicroContext.LOCAL_HOST, null);
		}
		
		if(StringUtils.isEmpty(clientIp)) {
			return false;
		}
		
		if(r.getFrom().getVal().startsWith(clientIp)) {
			return true;
		}
		
		return false;
	}
}
