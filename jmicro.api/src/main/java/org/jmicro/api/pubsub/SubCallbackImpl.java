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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:09:47
 */
public class SubCallbackImpl implements ISubCallback{

	private final static Logger logger = LoggerFactory.getLogger(SubCallbackImpl.class);
	
	private UniqueServiceMethodKey mkey = null;
	
	private Object srvProxy = null;
	
	private Method m = null;
	
	public SubCallbackImpl(UniqueServiceMethodKey mkey,Object srv){
		if(mkey == null) {
			throw new CommonException("SubCallback service method cannot be null");
		}
		
		if(srv == null) {
			throw new CommonException("SubCallback service cannot be null");
		}
		this.mkey = mkey;
		this.srvProxy = srv;
		setMt();
	}
	
	@Override
	public void onMessage(PSData item) {
		try {
			m.invoke(this.srvProxy, item);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new CommonException("fail to notify [" + mkey.toString()+"]", e);
		}
	}

	private void setMt() {
		try {
			this.m = this.srvProxy.getClass().getMethod(mkey.getMethod(), PSData.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new CommonException("Get ["+mkey.toString() +"] fail",e);
		}
	}

	@Override
	public String info() {
		return mkey.toKey(false, false, false);
	}

	@Override
	public String toString() {
		return info();
	}

	@Override
	public int hashCode() {
		return info().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return hashCode() == obj.hashCode();
	}
	
}
