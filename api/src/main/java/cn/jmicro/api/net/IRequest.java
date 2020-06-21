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
package cn.jmicro.api.net;

import java.util.Map;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:46
 */
public interface IRequest extends IReq{

	ISession getSession();
	
	public String getServiceName();

	public String getTransport();

	public String getNamespace();

	//public void setGroup(String group);

	public String getVersion();

	//public void setVersion(String version);
	
	public String getMethod();
	//public void setMethod(String method);
	
	public String getImpl();
	public void setImpl(String impl);
	
	public Object[] getArgs();
	//public void setArgs(Object[] args);
	
	public long getRequestId();
	
	public long getReqParentId();
	
	void setRequestId(long reqId);
	
	public boolean isSuccess();
	void setSuccess(boolean isSuccess);
	
	public boolean isFinish();
	public void setFinish(boolean finish);
	
	public boolean isMonitorEnable();
	
	public Long getMsgId();
	
	public Map<String,Object> getRequestParams();
	
	int getLogLevel();
	
	void putObject(String key,Object value);
}
