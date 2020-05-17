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
package cn.jmicro.ext.mybatis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:30
 */
@Component(value="mybatisInterceptor",lazy=false,side=Constants.SIDE_PROVIDER)
@Interceptor
public class MybatisInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(MybatisInterceptor.class);
	
	@Inject
	private CurSqlSessionFactory curSqlSessionManager;
	
	public MybatisInterceptor() {}
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest req) throws RpcException {
		IResponse resp = null;
		try {
			resp = handler.onRequest(req);
			if(curSqlSessionManager.curSession() != null) {
				curSqlSessionManager.commitAndCloseCurSession();
			}
		} catch (Throwable e) {
			if(curSqlSessionManager.curSession() != null) {
				curSqlSessionManager.rollbackAndCloseCurSession();
			}
		}
		return resp;
	}

}
