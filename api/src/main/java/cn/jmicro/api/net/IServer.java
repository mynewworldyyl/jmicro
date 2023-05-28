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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.common.serialize.kryo.utils.ReflectUtils;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.HashUtils;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:11
 */
public interface IServer{

	//void init();
	boolean isEnable();
	
	void start();
	
	void stop();
	
	String host();
	
	String port();
	
	public static boolean apiGatewayMsg(Message msg) {
		return msg.isOuterMessage() && msg.getType() == Constants.MSG_TYPE_REQ_JRPC;
	}
	
	public static String cacheKey(RpcClassLoader rpcClassloader, Message msg, ServiceMethodJRso sm,
			ICodecFactory codecFactory,Integer aid) {
		
		String mcode = sm.getKey().getSnvHash() + ":";
		if(Constants.CACHE_TYPE_MCODE == sm.getCacheType()) {
			//mcode +=  ah;
		}else if(Constants.CACHE_TYPE_ACCOUNT == sm.getCacheType()) {
			//ActInfoJRso ai = JMicroContext.get().getAccount();
			if(aid == null) {
				return null;
			}
			mcode += aid;
		} else if(Constants.CACHE_TYPE_PAYLOAD == sm.getCacheType()) {
			mcode += payloadCacheKey(rpcClassloader,msg,sm,codecFactory);
		} else if(Constants.CACHE_TYPE_PAYLOAD_AND_ACT == sm.getCacheType()) {
			//ActInfoJRso ai = JMicroContext.get().getAccount();
			if(aid == null) {
				return null;
			}
			mcode += aid +":" + payloadCacheKey(rpcClassloader,msg,sm,codecFactory);
		} else {
			return null;
		}
		
		return Constants.CACHE_DIR_PREFIX + mcode;
	}
	
	static Integer payloadCacheKey(RpcClassLoader rpcClassloader, Message msg, 
			ServiceMethodJRso sm, ICodecFactory codecFactory) {
		Integer ah = msg.getExtra(Message.EXTRA_KEY_ARG_HASH);

		if (ah != null) {
			return ah;
		}

		ByteBuffer sb = (ByteBuffer) msg.getPayload();
		
		/*
		 * sb.mark();
		RpcRequestJRso req;
		if (msg.isFromApiGateway() && msg.getUpProtocol() == Message.PROTOCOL_BIN) {
			req = parseApiGatewayRequest(rpcClassloader, msg, sm);
		} else {
			req = ICodecFactory.decode(codecFactory, sb, RpcRequestJRso.class, msg.getUpProtocol());
		}
		msg.setReq(req);
		ah = HashUtils.argHash(req.getArgs());
		sb.reset();
		*/
		
		ah = HashUtils.hash32(sb);
		msg.putExtra(Message.EXTRA_KEY_ARG_HASH, ah);

		return ah;
	}
	
	public static RpcRequestJRso parseApiGatewayRequest(RpcClassLoader rpcClassloader,Message msg, ServiceMethodJRso sm) {
		try {
			RpcRequestJRso req = new RpcRequestJRso();
			ByteBuffer bb = (ByteBuffer) msg.getPayload();
			if(bb != null && bb.remaining() > 0) {
				JDataInput ji = new JDataInput(bb);
				//req.setRequestId(ji.readLong());
				// req.setServiceName(ji.readUTF());
				// req.setNamespace(ji.readUTF());
				// req.setVersion(ji.readUTF());
				// req.setMethod(ji.readUTF());
				
				int eleNum = ji.readByte(); //extra元素个数
				if(eleNum < 0) {
					eleNum += 256; //参考encode方法说明
				}
		
				if (eleNum > 0) {
					for (int i = 0; i < eleNum; i++) {
						String k = ji.readUTF();
						String v = ji.readUTF();
						req.getParams().put(k, v);
					}
				}
				// si = getServiceItem(req);
				// sm = getServiceMethod(si,req);
				Class<?>[] paramsClses = ReflectUtils.desc2classArray(rpcClassloader, sm.getKey().getParamsStr());

				int argLen = ji.readByte(); //extra元素个数
				if(argLen < 0) {
					argLen += 256; //参考encode方法说明
				}
				
				if(argLen > 0) {
					req.setArgs(getArgs(paramsClses, ji,argLen));
				}
			}
			return req;
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Object[] getArgs(Class<?>[] clses, JDataInput ji, int argLen) {

		// ServiceItem item = registry.getServiceByImpl(r.getImpl());

		if (clses == null || clses.length == 0) {
			return new Object[0];
		}
		
		if(argLen != clses.length) {
			return null;
		}

		Object[] args = new Object[argLen];
		
		for(int i = 0; i < argLen; i++) {
			try {
				Object v = Message.decodeVal(ji);
				args[i] = v;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return args;
	}

	public static Object[] getArgs0(Class<?>[] clses, JDataInput ji) {

		// ServiceItem item = registry.getServiceByImpl(r.getImpl());

		if (clses == null || clses.length == 0) {
			return new Object[0];
		}

		Object[] args = new Object[clses.length];

		int i = 0;
		try {

			for (; i < clses.length; i++) {
				Class<?> pt = clses[i];
				Object a = null;
				if (pt == Integer.class || Integer.TYPE == pt) {
					Long v = ji.readLong();
					a = v.intValue();
				} else if (pt == Long.class || Long.TYPE == pt) {
					a = ji.readLong();
				} else if (pt == Short.class || Short.TYPE == pt) {
					Long v = ji.readLong();
					a = v.shortValue();
				} else if (pt == Byte.class || Byte.TYPE == pt) {
					Long v = ji.readLong();
					a = v.byteValue();
				} else if (pt == Float.class || Float.TYPE == pt) {
					Long v = ji.readLong();
					a = v.floatValue();
				} else if (pt == Double.class || Double.TYPE == pt) {
					Long v = ji.readLong();
					a = v.doubleValue();
				} else if (pt == Boolean.class || Boolean.TYPE == pt) {
					byte b = ji.readByte();
					a = b == 1;
				} else if (pt == Character.class || Character.TYPE == pt) {
					a = ji.readByte();
				} else if (pt == String.class) {
					a = ji.readUTF();
				} else if (Map.class.isAssignableFrom(pt)) {
					int len = (int) ji.readInt();
					if (len > 0) {
						Map<String, String> data = new HashMap<>();
						for (int x = 0; x < len; x++) {
							String k = ji.readUTF();
							String v = ji.readUTF();
							data.put(k, v);
						}
						a = data;
					}
				} else if (pt == new byte[0].getClass()) {
					int len = (int) ji.readInt();
					if (len > 0) {
						byte[] data = new byte[len];
						ji.readFully(data, 0, len);
						a = data;
					}
				}
				args[i] = a;
			}
		} catch (Exception e) {
			throw new CommonException("",e);
		}
		return args;
	}

}
