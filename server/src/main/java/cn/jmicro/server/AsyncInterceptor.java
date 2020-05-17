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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.ServerError;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.PubSubManager;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.Constants;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月19日
 */
@Component(value="asyncInterceptor",lazy=false,side=Constants.SIDE_PROVIDER)
@Interceptor(order = 1)
public class AsyncInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(AsyncInterceptor.class);
	
	@Inject
	private PubSubManager pubsubManager;
	
	public AsyncInterceptor() {}
	
	public void init() {
		
	}
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		
		IResponse resp = handler.onRequest(req);
		if(!(resp.getResult() instanceof ServerError)) {
			//正常返回
			return resp;
		}

		ServerError se = (ServerError)resp.getResult();
		if(se.getErrorCode() != ServerError.SE_LIMITER) {
			//非限速返回
			return resp;
		}
		
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		
		if(sm == null) {
			//不许异步调用
			return resp;
		}
		
		String topic = sm.getKey().toKey(false, false, false);
		Map<String,Object>  params = req.getRequestParams();
		if(params == null ||!params.containsKey(topic)) {
			return resp;
		}
		
		AsyncConfig ac = (AsyncConfig)params.get(topic);
		if(ac == null || !ac.isEnable()) {
			return resp;
		}
		
		//被限速后,通过异步消息调用方法
		//在SubCallbackImpl中使用以下信息回调接受参数RPC服务
		Map<String,Object> cxt = new HashMap<>();
		//结果回调RPC方法
		cxt.put(Constants.SERVICE_NAME_KEY, ac.getServiceName());
		cxt.put(Constants.SERVICE_NAMESPACE_KEY, ac.getNamespace());
		cxt.put(Constants.SERVICE_VERSION_KEY, ac.getVersion());
		cxt.put(Constants.SERVICE_METHOD_KEY, ac.getMethod());
		
		//链路相关ID
		cxt.put(JMicroContext.LINKER_ID, JMicroContext.lid());
		cxt.put(JMicroContext.REQ_ID, req.getRequestId());
		cxt.put(JMicroContext.MSG_ID, req.getMsgId());
		
		//以目标方法的签名为消息主题
		
		PSData data = new PSData();
		data.setContext(cxt);
		data.setData(req.getArgs());
		data.setFlag(PSData.flag(PSData.FLAG_QUEUE,PSData.FLAG_ASYNC_METHOD));
		data.setTopic(topic);
		
		//异步后,就不一定是本实例接收到此RPC调用了
		long id = pubsubManager.publish(data);
		
		if(id >= 0) {
			//告诉客户端,调用自动转异步调用了,你自己确定怎么办吧
			se.setErrorCode(ServerError.SE_LIMITER_ENTER_ASYNC);
		}
		
		return resp;
	}

}
