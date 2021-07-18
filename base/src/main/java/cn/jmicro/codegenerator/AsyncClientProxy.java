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
package cn.jmicro.codegenerator;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncClientProxy {
	
	public static final String SO_SUBFIX = "JRso";
	
	public static final String SRV_SUBFIX = "JMSrv";
	
	public static final String PKG_SUBFIX = "genclient";
	
	public static final String INT_SUBFIX = "$JMAsyncClient";
	
	public static final String INT_GATEWAY = "$Gateway";
	
	public static final String IMPL =  "Impl";
	
	public static final String IMPL_SUBFIX = INT_SUBFIX + IMPL;
	
	public static final String ASYNC_METHOD_SUBFIX = "JMAsync";
	
	public static final String INT_GATEWAY_CLASS = INT_GATEWAY +INT_SUBFIX;// "$Gateway$JMAsyncClient";
	
	public int dataVersion() default 0;
	
	public int clientId() default -1;
}
