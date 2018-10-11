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
package org.jmicro.api.monitor;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;

/**
 * 
 * @author Beck Ye
 * @date 2018年10月4日-上午11:49:26
 */
public interface MonitorConstant {

	public static void doSubmit(SubmitItemHolderManager monitor,int type,IRequest req,IResponse resp,Object... objs){
		if(JMicroContext.get().isMonitor() && monitor != null) {
			monitor.submit(type, req, null,objs);
		}
	}
	
    public static final int ALL  = 0X7FFFFFFF;
	
	public static final int CLIENT_REQ_BEGIN  = 0X7FFFFFF1;
	public static final int CLIENT_REQ_TIMEOUT = 0X7FFFFFF2;
	public static final int CLIENT_REQ_SERVICE_NOT_FOUND = 0X7FFFFFF3;
	public static final int CLIENT_REQ_SERVICE_FUSING = 0X7FFFFFF4;
	public static final int CLIENT_REQ_OK = 0X7FFFFFF6;
	public static final int CLIENT_REQ_TIMEOUT_FAIL = 0X7FFFFFFA;
	public static final int CLIENT_REQ_METHOD_NOT_FOUND = 0X7FFFFFF7;
	public static final int CLIENT_REQ_HAVE_FINISH = 0X7FFFFFF8;
	public static final int CLIENT_REQ_RETRY = 0X7FFFFFF9;
	public static final int CLIENT_REQ_CONN_FAIL = 0X7FFFFFFB;
	public static final int CLIENT_REQ_CONN_CLOSE = 0X7FFFFFFC;
	public static final int CLIENT_REQ_EXCEPTION_ERR = 0X7FFFFFF5;
	public static final int CLIENT_REQ_BUSSINESS_ERR = 0X7FFFFFFD;
	
	public static final int CLIENT_REQ_ASYNC1_SUCCESS= 0X7FFFFFFE;
	public static final int CLIENT_REQ_ASYNC2_SUCCESS= 0X7FFFFFE1;
	public static final int CLIENT_REQ_ASYNC2_FAIL= 0X7FFFFFE2;
	
	public static final int CLIENT_IOSESSION_CLOSE = 0X7FFFFFFD;
	public static final int CLIENT_IOSESSION_OPEN = 0X7FFFFFFE;
	public static final int CLIENT_IOSESSION_IDLE = 0X7FFFFFE5;
	public static final int CLIENT_IOSESSION_WRITE = 0X7FFFFFE0;
	public static final int CLIENT_IOSESSION_READ =  0X7FFFFFE1;
	public static final int CLIENT_IOSESSION_EXCEPTION = 0X7FFFFFE2;
	public static final int CLIENT_PACKAGE_SESSION_ID_ERR = 0X7FFFFFE3;
	
	public static final int SERVER_REQ_BEGIN =      0X7FFFFEF1;
    public static final int SERVER_REQ_LIMIT_FORBIDON=0X7FFFFEF2;
	public static final int SERVER_REQ_LIMIT_OK =   0X7FFFFEF3;
	public static final int SERVER_REQ_TIMEOUT =    0X7FFFFEF4;
	public static final int SERVER_REQ_OK =         0X7FFFFEF5;
	public static final int SERVER_REQ_ERROR =      0X7FFFFEF6;
	
	public static final int SERVER_RESP_OK =        0X7FFFFEF7;
	public static final int SERVER_RESP_ERR =       0X7FFFFEF8;
	public static final int SERVER_PACKAGE_SESSION_ID_ERR = 0X7FFFFEF9;
	
	public static final int SERVER_START =           0X7FFFFEFA;
	public static final int SERVER_STOP =            0X7FFFFEFB;
	public static final int SERVER_REQ_SERVICE_NOT_FOUND = 0X7FFFFEFC;
	
	public static final int SERVER_IOSESSION_CLOSE = 0X7FFFFEFD;
	public static final int SERVER_IOSESSION_OPEN =  0X7FFFFEFE;
	public static final int SERVER_IOSESSION_IDLE =  0X7FFFFEEF;
	public static final int SERVER_IOSESSION_WRITE = 0X7FFFFEE0;
	public static final int SERVER_IOSESSION_READ =  0X7FFFFEE1;
    public static final int SERVER_IOSESSION_EXCEPTION=0X7FFFFEE2;

}
