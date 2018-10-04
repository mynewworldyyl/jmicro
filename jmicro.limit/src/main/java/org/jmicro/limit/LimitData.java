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
package org.jmicro.limit;

import org.jmicro.api.registry.ServiceItem;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:12:06
 */
public class LimitData {

	private long reqTime = System.currentTimeMillis();
	
	private ServiceItem si;

	public long getReqTime() {
		return reqTime;
	}

	public void setReqTime(long reqTime) {
		this.reqTime = reqTime;
	}

	public ServiceItem getSi() {
		return si;
	}

	public void setSi(ServiceItem si) {
		this.si = si;
	}
	
	
	
}
