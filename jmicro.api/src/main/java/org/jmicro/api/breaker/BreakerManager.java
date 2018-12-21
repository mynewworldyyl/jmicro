package org.jmicro.api.breaker;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.OnePrefixDecoder;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.api.timer.ITickerAction;
import org.jmicro.api.timer.TimerTicker;
import org.jmicro.common.Base64Utils;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.StringUtils;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:49:55
 */
@Component
public class BreakerManager implements ITickerAction{
	
	private final static Logger logger = LoggerFactory.getLogger(BreakerManager.class);
	
	private final Map<Long,TimerTicker> timers = new ConcurrentHashMap<>();
	
	@Inject
	private ServiceManager srvManager;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private OnePrefixDecoder decoder;
	
	public void init(){
		
	}

	public void breakService(String key, ServiceMethod sm) {
		srvManager.breakService(sm);
		long interval = TimeUtils.getMilliseconds(sm.getBreakingRule().getCheckInterval(), sm.getBaseTimeUnit());
		if(sm.isBreaking()) {
			//服务熔断了,做自动服务检测
			TimerTicker.getTimer(timers,interval).addListener(key, this,sm);
		} else {
			//关闭服务自动检测
			TimerTicker.getTimer(timers,interval).removeListener(key);
		}
	}

	@Override
	public void act(String key, Object attachement) {
		ServiceMethod sm = (ServiceMethod)attachement;
		long interval = TimeUtils.getMilliseconds(sm.getBreakingRule().getCheckInterval(), sm.getBaseTimeUnit());;
		if(!sm.isBreaking()) {
			TimerTicker.getTimer(timers,interval).removeListener(key);
			return;
		}
		
		//服务熔断了,做自动服务检测
		Object srv = of.getServie(sm.getKey().getServiceName(),sm.getKey().getNamespace(),sm.getKey().getVersion());
		if(srv == null) {
			throw new CommonException("Service ["+sm.getKey().getServiceName()+"] not found");
		}
		
		Class<?>[] paramsTypeArr = UniqueServiceMethodKey.paramsClazzes(sm.getKey().getParamsStr());
		Object args = null;
		if(StringUtils.isEmpty(sm.getTestingArgs())) {
			args = new Object[0];
		} else {
			args = getParams(sm.getTestingArgs());
		}
		
		try {
			Method m = srv.getClass().getMethod(sm.getKey().getMethod(), paramsTypeArr);
			m.invoke(srv, args);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("act",e);
			throw new CommonException("act",e);
		}
	}
	
	private Object[] getParams(String testingArgs) {
		try {
			byte[] data = Base64Utils.decode(testingArgs.getBytes(Constants.CHARSET));
			Object[] args = (Object[]) this.decoder.decode(ByteBuffer.wrap(data));
			return args;
		} catch (UnsupportedEncodingException e) {
			logger.error("",e);
			throw new CommonException("",e);
		}
	}
}
