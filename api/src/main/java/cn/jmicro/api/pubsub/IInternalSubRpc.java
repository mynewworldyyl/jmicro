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
package cn.jmicro.api.pubsub;

import cn.jmicro.api.annotation.Service;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:01
 */
@Service(namespace=Constants.DEFAULT_PUBSUB,version="0.0.1")
@AsyncClientProxy
public interface IInternalSubRpc {

	/**
	 * @param item
	 * @return 状态码
	 */
	int publishItem(PSData item);
	
	/**
	 * 
	 * @param topic
	 * @param items
	 * @return 状态码
	 */
	int publishItems(String topic, PSData[] items);
	
	/**
	 * 
	 * @param topic
	 * @param content
	 * @return 状态码
	 */
	int publishString(String topic,String content);
	
}
