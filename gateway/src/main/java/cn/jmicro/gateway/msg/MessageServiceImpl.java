
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.gateway.IGatewayMessageCallbackJMSrv;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

/**
 * 用于外部客户端订阅pubsub数据
 * 
 * @author Yulei Ye
 * @date 2020年3月26日
 */
@Component(side=Constants.SIDE_PROVIDER)
public class MessageServiceImpl implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);
	public static final String TAG = MessageServiceImpl.class.getName();
	
	@Inject
	private MsgGatewayManager msgGm;

	@Override
	public boolean onMessage(ISession session, Message msg) {
		Byte op = msg.getExtra(Message.EXTRA_KEY_PS_OP_CODE);

		int opCode = new Double(Double.parseDouble(op.toString())).intValue();
		if(opCode == IGatewayMessageCallbackJMSrv.MSG_OP_CODE_FORWARD) {
			//转发类消息,to actId为转发目标账号ID
			Integer tactId = msg.getExtra(Message.EXTRA_KEY_PS_ARGS);
			if (tactId == null) {
				responseError(session, msg, RespJRso.SE_INVLID_ARGS, "Invalid forward tactId");
				return true;
			}
			
			//备份客户端的消息ID
			Long msgId = msg.getMsgId();
			Long suc = msgGm.forward(msg,tactId);
			
			//给客户端返回服务器生成的消息全局唯一标识
			msg.putExtra(Message.EXTRA_KEY_SMSG_ID, msg.getMsgId());
			//还原客户端的消息ID
			msg.setMsgId(msgId);
			
			msg.setPayload(suc);
		} else if (opCode == IGatewayMessageCallbackJMSrv.MSG_OP_CODE_UNSUBSCRIBE) {
			// 取消订阅消息
			Integer subId = msg.getExtra(Message.EXTRA_KEY_PS_ARGS);
			if (subId == null) {
				responseError(session, msg, RespJRso.SE_INVALID_SUB_ID, "Invalid subscribe id");
				return true;
			}
			boolean suc = msgGm.unsubscribe(subId);
			msg.setPayload(suc);
		} else if (opCode == IGatewayMessageCallbackJMSrv.MSG_OP_CODE_SUBSCRIBE) {
			// 订阅消息
			String topic = msg.getExtra(Message.EXTRA_KEY_PS_ARGS);
			if (Utils.isEmpty(topic)) {
				responseError(session, msg, RespJRso.SE_INVALID_TOPIC, "Topic is null");
				return true;
			}
			int subId = msgGm.subscribe(session, topic, msg);
			msg.setPayload(subId);
			msg.putExtra(Message.EXTRA_KEY_EXT0, subId);
		}

		msg.setError(false);

		msg.setType(Constants.MSG_TYPE_PUBSUB_RESP);
		session.write(msg);
		return true;
	}

	private void responseError(ISession s,Message msg,int seInvalidTopic, String msgStr) {
		msg.setError(true);
		RespJRso se = new RespJRso(seInvalidTopic,msgStr);
		try {
			byte[] d = JsonUtils.getIns().toJson(se).getBytes(Constants.CHARSET);
			msg.setPayload(ByteBuffer.wrap(d));
			s.write(msg);
		} catch (UnsupportedEncodingException e) {
			logger.error(se.toString(),e);
		}
	}
	
	public void jready() {}

	@Override
	public Byte type() {
		return Constants.MSG_TYPE_PUBSUB;
	}
}
