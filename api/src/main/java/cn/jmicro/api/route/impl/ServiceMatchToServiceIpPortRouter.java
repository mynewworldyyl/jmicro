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

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.route.AbstractRouter;
import cn.jmicro.api.route.IRouter;
import cn.jmicro.api.route.RouteRuleJRso;

/**
 * 
 * @author Yulei Ye
 * @date: 2018年11月11日 下午3:56:55
 */
@Component(value=RouteRuleJRso.TYPE_FROM_SERVICE_ROUTER,lazy=false)
public class ServiceMatchToServiceIpPortRouter extends AbstractRouter implements IRouter {

	public ServiceMatchToServiceIpPortRouter() {
		super(RouteRuleJRso.TYPE_FROM_SERVICE_ROUTER);
	}

	@Override
	protected boolean accept(RouteRuleJRso r) {
		return true;
	}

}
