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
package org.jmicro.api.fusing;

import java.lang.reflect.Method;
import java.util.Set;

import org.jmicro.api.registry.ServiceItem;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:05
 */
public interface IFusingHandler {

	boolean canHandle(Method method, Object[] args,Set<ServiceItem> items);
	Object onFusing( Method method, Object[] args,Set<ServiceItem> items);
}