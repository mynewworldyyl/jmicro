
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
package cn.jmicro.gateway.lb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.ProcessInfo;
import cn.jmicro.api.client.IClientSessionManager;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.gateway.IBalance;
import cn.jmicro.api.gateway.MessageRoutTable;
import cn.jmicro.api.gateway.MessageRouteGroup;
import cn.jmicro.api.gateway.MessageRouteRow;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 
 *
 * @author Yulei Ye
 */
@Component(side = Constants.SIDE_PROVIDER)
public class ApiRawRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiRawRequestMessageHandler.class);
	private static final Class<?> TAG = ApiRawRequestMessageHandler.class;
	
	@Inject
	private Map<String,IBalance> balances = new HashMap<>();
	
	//@Inject("defBalance")
	private IBalance defaultBl;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private ProcessInfo pi;
	
	@Inject
	private MessageRoutTable mrt;
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Cfg("/ApiRawRequestMessageHandler/openDebug")
	private boolean openDebug = false;
	
	@Cfg(MessageRouteRow.API_MODEL)
	private boolean isPre = MessageRouteRow.API_MODEL_PRE;
	
	public void ready() {
		Config cfg = of.get(Config.class);
		String lbName = cfg.getString("balanceName", "defBalance");
		defaultBl = of.getByName(lbName);
		if(defaultBl == null) {
			throw new CommonException("Balance with name: " + lbName + " not found!");
		}
	}
	
	@Override
	public Byte type() {
		return Constants.MSG_TYPE_REQ_RAW;
	}
	
	@Override
	public void onMessage(ISession session, Message msg) {
		boolean succ = false;
		if(isPre) {
			succ = dispatch(session,msg);
		}
		
		if(!succ) {
			dispatch2Service(session,msg);
		}
	}

	private void dispatch2Service(ISession session, Message msg) {
		
	}

	private boolean dispatch(ISession session, Message msg) {
		String type = msg.getType()+"";
		MessageRouteGroup mrg = mrt.getGatewayRouteGroup(type);
		if(mrg == null ) return false;
		List<MessageRouteRow> list = mrg.getList();
		if(list == null || list.isEmpty() ) return false;
		
		IBalance bl = null;
		if(list.size() > 1) {
			 bl = balances.get(mrg.getAlg());
			if(bl == null) bl = this.defaultBl;
		}
		
		MessageRouteRow r = bl.balance(mrg);
		
		ISession cs = sessionManager.getOrConnect(Config.getInstanceName(), r.getIp(), r.getPort());
		cs.write(msg);
		
		return true;
	}
	
}
