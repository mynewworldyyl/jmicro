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
package org.jmicro.api.exception;

import org.jmicro.common.CommonException;
/**
 * 
 * @author Yulei Ye
 * @date 2018年11月8日 上午9:29:20
 */
public class SessionCloseException extends CommonException {
	 
	private static final long serialVersionUID = 1343228923L;
	
	public SessionCloseException(String cause){
		super(cause);
	}
	
	public SessionCloseException(String cause,Throwable exp){
		super(cause,exp);
	}
	
	public SessionCloseException(String key,String cause){
		this(key,cause,null);
	}
	
	public SessionCloseException(String key,String cause,Throwable exp){
		super(key,cause,exp);
	}

}
