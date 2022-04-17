
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
package cn.jmicro.gateway.msg;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.IGatewayMessageCallbackJMSrv;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.common.Constants;

/**
 *用于外部客户端发送pubsub数据
 * 
 * @author Yulei Ye
 * @date 2020年3月26日
 */
@Component(side=Constants.SIDE_PROVIDER)
@Service(version="0.0.1",showFront=false,external=false,infs=IGatewayMessageCallbackJMSrv.class)
public class PsItemGatewayServiceImpl implements IGatewayMessageCallbackJMSrv{

	private final static Logger logger = LoggerFactory.getLogger(PsItemGatewayServiceImpl.class);
	
	public static final String MESSAGE_SERVICE_REG_ID = "__messageServiceRegId";
	public static final String TIMER_KEY = "__MessageRegistionStatusCheck";
	
	public static final String TAG = PsItemGatewayServiceImpl.class.getName();
	
	@Inject
	private MsgGatewayManager msgGm;
	
	@Override
	public void onOnePSMessage(PSDataJRso item) {
		publishOneMessage(item);
	}

	private void publishOneMessage(PSDataJRso i) {
		msgGm.publishOneMessage(i);
	}

	@Override
	@SMethod(needResponse=false,maxPacketSize=10240,asyncable=true,timeout=3000,retryCnt=0,needLogin=false)
	public void onPSMessage(PSDataJRso[] items) {
		if(items == null || items.length == 0) {
			logger.warn("Got items is null: ");
			return;
		}
		for(PSDataJRso i : items) {
			 publishOneMessage(i);
		}
	}
	
	@Override
	@SMethod(needResponse=false,maxPacketSize=10240,timeout=3000,retryCnt=0,needLogin=false)
	public void onPSMessage2Users(PSDataJRso item, Set<Integer> userIds) {
		for(Integer uid : userIds) {
			String topic = IGatewayMessageCallbackJMSrv.USER_TOPIC_PREFIX + uid;
			item.setTopic(topic);
			this.publishOneMessage(item);
		}
	}
	
	public void jready() {
		
	}
	
}
