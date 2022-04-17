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
package cn.jmicro.api.internal.pubsub;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:01
 */
@Service(version="0.0.1")
@AsyncClientProxy
public interface IInternalSubRpcJMSrv {

	/**
	 * @param item
	 * @return 状态码
	 */
	IPromise<RespJRso<Integer>> publishItem(PSDataJRso item);
	
	/**
	 * 
	 * @param topic
	 * @param items
	 * @return 状态码
	 */
	IPromise<RespJRso<Integer>> publishItems(String topic, PSDataJRso[] items);
	
	/**
	 * 
	 * @param topic
	 * @param content
	 * @return 状态码
	 */
	IPromise<RespJRso<Integer>> publishString(String topic,String content);
	
	/**
	 * 检测是否存在指定主题的订阅者
	 * @param topic
	 * @return
	 */
	IPromise<RespJRso<Boolean>> hasTopic(String topic);
	
}
