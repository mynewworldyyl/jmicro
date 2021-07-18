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
package cn.jmicro.pubsub;

import java.util.Map;

import cn.jmicro.api.registry.ServiceMethodJRso;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月16日
 */
class SubcribeItem {

	public static final int TYPE_SUB = 1;
	public static final int TYPE_REMOVE = 2;
	public static final int TYPE_UPDATE = 3;
	
	public int type;
	public String topic;
	public ServiceMethodJRso sm;
	public Map<String, String> context;
	
	public SubcribeItem(int type,String topic,ServiceMethodJRso sm,Map<String, String> context) {
		this.type = type;
		this.topic = topic;
		this.sm = sm;
		this.context = context;
	}

}
