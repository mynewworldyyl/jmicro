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
package org.jmicro.api.pubsub;

import org.jmicro.api.annotation.Service;
import org.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:01
 */
@Service(namespace=Constants.DEFAULT_PUBSUB,version="0.0.1")
public interface IInternalSubRpc {

	//boolean subcribe(String topic,String srv,String namespace,String version,String method);
	
	//boolean unsubcribe(String topic,String srv,String namespace,String version,String method);
	/**
	 * 
	 * @param item
	 * @return 小于或等于0表示错误状态，此次消息发送失败，需要客户端处理，大于0表示消息消息发送成功，返回值即为消息ID
	 */
	long publishData(PSData item);
	
	/**
	 * 
	 * @param topic
	 * @param content
	 * @return 小于或等于0表示错误状态，此次消息发送失败，需要客户端处理，大于0表示消息消息发送成功，返回值即为消息ID
	 */
	long publishString(String topic,String content);
	
	//boolean publish(String topic,byte[] content);
	
}
