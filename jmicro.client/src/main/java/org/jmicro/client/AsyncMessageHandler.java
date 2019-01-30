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
package org.jmicro.client;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IMessageCallback;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月10日-下午12:56:40
 */
@Component(side=Constants.SIDE_COMSUMER)
public class AsyncMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(AsyncMessageHandler.class);
	
	@Cfg("/AsyncMessageHandler/openDebug")
	private boolean openDebug;
	
	@Cfg("/respBufferSize")
	private int respBufferSize = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_ASYNC_RESP;
	}

	@Override
	public void onMessage(ISession session,Message msg) {
		//new Fiber<Void>(() ->handleResponse((IClientSession)session,msg)).start();
		handleResponse((IClientSession)session,msg);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void handleResponse(IClientSession session,Message msg){
		RpcResponse resp = ICodecFactory.decode(this.codecFactory,msg.getPayload(),RpcResponse.class,msg.getProtocol());
		resp.setMsg(msg);
		
		//req.setMsg(msg);
		String key = msg.getReqId()+"";
		
		if(openDebug) {
			logger.debug("Receive Async msg ReqId: "+key);
		}
		
		/*ServiceMethod si = (ServiceMethod) session.getParam(key);
		if(si == null){
			logger.error("No Service Method found for stream callback");
			return;
		}*/
		
		/*String streamCb = si.getStreamCallback();
		if(StringUtils.isEmpty(streamCb)){
			logger.error("Callback canot be NULL");
			return;
		}*/
		
		/*
		String[] arr = streamCb.split("#");
		if(arr.length != 2){
			logger.error("Callback ["+streamCb+" params invalid");
			return;
		}
		
		String serviceName = arr[0];
		String methodName = arr[1];
		Object srv = ComponentManager.getObjectFactory().getByName(serviceName);
		if(srv == null){
			logger.error("Service ["+serviceName+" not found!");
			return;
		}
		
		int i = methodName.indexOf("(");
		if(i < 0) {
			logger.error("Callback ["+streamCb+" config invalid");
			return;
		}
		//String ps = methodName.substring(i, methodName.lastIndexOf(")"));
		methodName = methodName.substring(0,i);
		*/
		
		IMessageCallback callback = (IMessageCallback)session.getParam(key);
		if(callback == null){
			logger.error("Service [ "+key+"] not found!");
			return;
		}
		callback.onMessage(resp.getResult());
		
		/*try {
			Class<?>[] pts = ServiceLoader.getMethodParamsType((Object[])resp.getResult());
			Method m = srv.getClass().getMethod(methodName, pts);
			if(m != null){
				m.invoke(srv, new Object[]{resp.getResult()});
			}
			
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("",e);
		}*/
	}
}
