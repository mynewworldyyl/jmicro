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

import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IResponse;

/**
 * 
 * @author Beck Ye
 * @date 2018年10月4日-上午11:49:26
 */
public interface Monitor {

	public static final int CLIENT_REQ_BEGIN  = 0XFFFFFFF1;
	public static final int CLIENT_REQ_TIMEOUT = 0XFFFFFFF2;
	public static final int CLIENT_REQ_SERVICE_NOT_FOUND = 0XFFFFFFF3;
	public static final int CLIENT_REQ_SERVICE_CUT_DOWN = 0XFFFFFFF4;
	public static final int CLIENT_RESP_ERR = 0XFFFFFFF5;
	public static final int CLIENT_RESP_OK = 0XFFFFFFF5;

	public static final int SERVER_REQ_BEGIN = 0XFFFFFEF1;
	public static final int SERVER_REQ_LIMIT_FORBIDON = 0XFFFFFEF2;
	public static final int SERVER_REQ_LIMIT_OK = 0XFFFFFEF3;
	public static final int SERVER_REQ_TIMEOUT = 0XFFFFFEF4;
	public static final int SERVER_REQ_OK = 0XFFFFFEF5;
	public static final int SERVER_REQ_ERROR = 0XFFFFFEF6;
	
	public static final int SERVER_RESP_OK = 0XFFFFFEF7;
	public static final int SERVER_RESP_ERR = 0XFFFFFEF8;
	
	void request(int type,IRequest req,IResponse resp);
	
	
}
