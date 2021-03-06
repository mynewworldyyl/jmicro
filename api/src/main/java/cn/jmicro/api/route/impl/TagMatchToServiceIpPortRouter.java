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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.route.AbstractRouter;
import cn.jmicro.api.route.IRouter;
import cn.jmicro.api.route.RouteRuleJRso;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date: 2018年11月11日 下午3:56:55
 */
@Component(value=RouteRuleJRso.TYPE_FROM_TAG_ROUTER,lazy=false)
public class TagMatchToServiceIpPortRouter extends AbstractRouter  implements IRouter {

	public TagMatchToServiceIpPortRouter() {
		super(RouteRuleJRso.TYPE_FROM_TAG_ROUTER);
	}

	@Override
	protected boolean accept(RouteRuleJRso r) {
		String ctxVal = JMicroContext.get().getString(r.getFrom().getTagKey(),null);
		if(StringUtils.isNotEmpty(ctxVal) && ctxVal.equals(r.getFrom().getVal())) {
			return true;
		}
		return false;
	}

}
