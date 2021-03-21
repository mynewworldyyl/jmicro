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
package cn.jmicro.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.EnterMain;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.limitspeed.ILimiter;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:30
 */
@Component(value=Constants.FIRST_INTERCEPTOR,lazy=false,side=Constants.SIDE_PROVIDER)
@Interceptor(order = 0)
public class FirstInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(FirstInterceptor.class);
	
	//@Cfg(value ="/defaultLimiterName", required=false, changeListener="limiterName")
	private String defaultLimiterName="limiterName";
	
	@Cfg("/respBufferSize")
	private int respBufferSize = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	private ILimiter limiter = null;
	
	public FirstInterceptor() {}
	
	public void init() {
		limiterName(defaultLimiterName);
	}
	
	public void limiterName(String fieldName){
		if(StringUtils.isEmpty(fieldName)){
			return;
		}
		
		if("defaultLimiterName".equals(fieldName.trim())){
			ILimiter l = EnterMain.getObjectFactory().getByName(defaultLimiterName);
			if(l != null) {
				limiter = l;
				logger.warn("Change limit to :{}",this.defaultLimiterName);
				LG.log(MC.LOG_DEBUG,FirstInterceptor.class,
						"Change limit to: "+defaultLimiterName);
			} else {
				logger.error("Limiter [{}] not found",defaultLimiterName);
				LG.log(MC.LOG_ERROR,FirstInterceptor.class,
						"Limiter ["+defaultLimiterName+"] not found");
			}
		}
	}
	
	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest req) throws RpcException {
		/*
		 if(limiter != null){
			boolean r = limiter.enter(req);
			if(!r){
				logger.warn("Limit exceep, forbidon this request");
				LG.log(MC.LOG_ERROR, FirstInterceptor.class, "Limit exceep, forbidon this request");
				return fastFail(req);
			}
		}
		*/
		
		IPromise<Object>  p = handler.onRequest(req);
		
		//通知限速器结束一个请求
		/*if(limiter != null){
			limiter.end(req);
		}*/
		
		return p;
	}

}
