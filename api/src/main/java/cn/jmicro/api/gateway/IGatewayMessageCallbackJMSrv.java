
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
package cn.jmicro.api.gateway;

import java.util.Set;

import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 
 * 后台可以通过网关给用户下发消息
 * @author Yulei Ye
 * @date 2020年3月26日
 */
@AsyncClientProxy
public interface IGatewayMessageCallbackJMSrv {

	public static final Integer MSG_TYPE_CHAT_CODE = 5556;//聊天消息
    public static final Integer MSG_TYPE_SENDER_CODE = 5555;//配送员端订单消息
	public static final Integer MSG_TYPE_CUST_CODE = 5557;//配送服务客户端消息
	
	public static final Integer MSG_OP_CODE_SUBSCRIBE = 1;//订阅消息
	public static final Integer MSG_OP_CODE_UNSUBSCRIBE = 2;//取消订阅消息
	public static final Integer MSG_OP_CODE_FORWARD = 3;//转发消息
	public static final Integer MSG_OP_CODE_FORWARD_BY_TOPIC = 4;//根据主题做转发
	
	//账号消息前缀
	public static final String USER_TOPIC_PREFIX = "/__act/msg/";
	
	/**
	 * 多消息发送，基于每个消息都是不同主题
	 * @param item
	 */
	void onPSMessage(PSDataJRso[] item);
	
	/**
	 * 单消息发送
	 * @param item
	 */
	void onOnePSMessage(PSDataJRso item);
	
	/**
	 * 一个消息发给N个用户，群发消息
	 * @param item
	 * @param userIds
	 */
	void onPSMessage2Users(PSDataJRso item, Set<Integer> userIds);
	
}
