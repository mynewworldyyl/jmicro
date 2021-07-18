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
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.route.AbstractRouter;
import cn.jmicro.api.route.IRouter;
import cn.jmicro.api.route.RouteRuleJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.Constants;

/**
 * @author Yulei Ye
 * @date: 2018年11月11日 下午3:56:55
 */
@Component(value=RouteRuleJRso.TYPE_FROM_ACCOUNT_ROUTER,lazy=false)
public class AccountRouterImpl extends AbstractRouter implements IRouter {

	public AccountRouterImpl() {
		super(RouteRuleJRso.TYPE_FROM_ACCOUNT_ROUTER);
	}
	
	@Override
	protected boolean accept(RouteRuleJRso r) {
		ActInfoJRso ai = null;
		ServiceMethodJRso sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm.getForType() == Constants.FOR_TYPE_SYS) {
			ai = JMicroContext.get().getSysAccount();
		}else {
			ai = JMicroContext.get().getAccount();
		}
		
		if(ai == null) {
			return false;
		}
		
		if(r.getFrom().getVal().equals(ai.getActName())||
				r.getFrom().getVal().equals(ai.getClientId()+"")) {
			return true;
		}
		
		return false;
	}

	
}
