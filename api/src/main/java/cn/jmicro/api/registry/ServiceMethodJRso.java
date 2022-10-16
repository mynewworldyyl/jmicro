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
package cn.jmicro.api.registry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;

import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import lombok.Data;
import lombok.Serial;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:04:38
 */
@Serial
@Data
public final class ServiceMethodJRso {
	
	public transient ServiceItemJRso _serviceItem;

	private UniqueServiceMethodKeyJRso key = new UniqueServiceMethodKeyJRso();
	
	private String desc;
	
	//private String methodName="";
	//private String[] methodParamTypes; //full type name
	
	/**
	 * 是否可通过api网关使用
	 * true：可以通过API网关访问
	 *	false ：不可以通过API网关访问
	 */
	public boolean external;
	
	//-1 use Service config, 0 disable, 1 enable
	private int monitorEnable = -1;
	private byte logLevel  = MC.LOG_DEPEND;
	//dump 下行流，用于下行数问题排查
	private boolean dumpDownStream = false;
	//dump 上行流，用于上行数问题排查
	private boolean dumpUpStream = false;
	
	//开启debug模式，调模式下，会附加更多调试信息到网络消息包中，以方便调试
	//此设置针对服务方法，注意区别于针对组件的openDebug属性，openDebug设置组件输出更多日志到日志文件或日志中心
	private int debugMode = -1;
	
	private int retryCnt; //method can retry times, less or equal 0 cannot be retry
	private int retryInterval; // milliseconds how long to wait before next retry
	private int timeout; // milliseconds how long to wait for response before timeout 
	
	private BreakRuleJRso breakingRule = new BreakRuleJRso();
	
	private boolean asyncable = false;
	
	//统计服务数据基本时长，单位同baseTimeUnit确定 @link SMethod
	private long timeWindow = -1;
	
	//循环时钟每个单位的时间长度
	private int slotInterval = 1;
	
	//采样统计数据周期，单位由baseTimeUnit确定
	private long checkInterval = -1;
	
	//基本时间单位  @link SMethod
	private String baseTimeUnit = Constants.TIME_MILLISECONDS;
	
	/**
	 * true all service method will fusing, false is normal service status
	 */
	private boolean breaking = false;
	
	/**
	 * 失败时的默认返回值，包括服务熔断失败，降级失败等
	 */
	private String failResponse;
	
	/**
	 * Max failure time before degrade the service
	 */
	private int maxFailBeforeDegrade;
	
	/**
	 * after the service cutdown, system can do testing weather the service is recovery
	 * with this arguments to invoke the service method
	 */
	private String testingArgs;
	
	/**
	 * max qps，单位同baseTimeUnit确定
	 */
	private int maxSpeed = -1;
	
	private int limitType = 1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	private int avgResponseTime;
	
	/**
	 * 1 is normal status, 
	 * 
	 * update rule:
	 * 2 will trigger the maxSpeed=2*maxSpeed and minSpeed=2*minSpeed, n will trigger 
	 * maxSpeed=n*maxSpeed and minSpeed=n*minSpeed
	 * 
	 * degrade rule:
	 * -2 will trigger maxSpeed=maxSpeed/2 and minSpeed=minSpeed/2, and n will trigger
	 * maxSpeed=maxSpeed/n and minSpeed=minSpeed/n
	 * 
	 * 0 and -1 is a invalid value
	 */
	private int degrade = 1;
	
	//0: need response, 1:no need response
	private boolean needResponse = true;

	//true async return result,
	//public boolean async = false;

	//false: not stream, true:stream, more than one request and response double stream
	//a stream service must be async=true, and get got result by callback
	//private boolean stream = false;
	
	//如果客户端RPC异步调用，此topic值必须是方法全限定名，参考toKey方法实现
	private String topic = null;
	
	//是否需要强制赋予账号权限才能使用
	private boolean perType = false;
	
	private boolean isDownSsl = false;
	
	private boolean isUpSsl = false;
	
	//0:对称加密，1：RSA 非对称加密
	private byte encType = 0;
	
	//必须登陆才能使用
	private boolean needLogin = false;
	
	//
	private int forType;
	
	private int maxPacketSize = 0;
	
	private byte feeType = Constants.LICENSE_TYPE_FREE;
	
	private byte txType = TxConstants.TYPE_TX_NO;
	
	private byte txIsolation = Connection.TRANSACTION_READ_COMMITTED;
	
	private byte txPhase = TxConstants.TX_2PC;
	
	private byte cacheType = Constants.CACHE_TYPE_NO;
	private int cacheExpireTime = 60; //缓存超时是间
	
	private int[] authClients;
	
	//处理httpy请求的URL，默认不处理HTTP请求
	private String httpPath;
	
	//处理httpy请求方法，默认处理全部请求方法
	private String httpMethod;
	
	//HTTP请求体内容类型
	private String httpReqContentType;
	
	//整个HTTP请求数据作为一个整体作为参数唯一方法，参数可以是字符串或VO对象
	private boolean httpReqBody;
	
	private byte httpRespType;
	
	private String[] paramNames;
	
	public void formPersisItem(ServiceMethodJRso p){
		this.monitorEnable = p.monitorEnable;

		this.retryCnt = p.retryCnt;
		this.retryInterval = p.retryInterval;
		this.timeout = p.timeout;
		this.timeWindow = p.timeWindow;
		this.slotInterval = p.slotInterval;

		this.maxFailBeforeDegrade = p.maxFailBeforeDegrade;
		this.getBreakingRule().from(p.getBreakingRule());
		this.asyncable = p.asyncable;

		this.testingArgs = p.testingArgs;
		this.breaking = p.breaking;

		this.degrade = p.degrade;
		this.maxSpeed = p.maxSpeed;
		this.limitType = p.limitType;
		this.avgResponseTime = p.avgResponseTime;
		this.failResponse = p.failResponse;
		this.needResponse = p.needResponse;
		
		this.logLevel = p.logLevel;
		
		this.baseTimeUnit = p.baseTimeUnit;
		this.checkInterval = p.checkInterval;
		
		this.dumpDownStream = p.dumpDownStream;
		this.dumpUpStream = p.dumpUpStream;
		this.debugMode = p.debugMode;
		
		this.topic = p.topic;
		
		this.perType = p.perType;
		this.needLogin = p.needLogin;
		this.maxPacketSize = p.maxPacketSize;
		
		this.isUpSsl = p.isUpSsl;
		this.isDownSsl = p.isDownSsl;
		this.encType = p.encType;
		
		this.feeType = p.feeType;
		this.authClients = p.authClients;
		
		this.forType = p.forType;
		this.txType = p.txType;
		this.txIsolation = p.txIsolation;
		this.txPhase = p.txPhase;
		
		this.desc = p.desc;
		this.cacheType = p.cacheType;
		this.cacheExpireTime = p.cacheExpireTime;
		
		this.external = p.external;
		
		this.httpMethod = p.httpMethod;
		this.httpRespType = p.httpRespType;
		this.httpPath = p.httpPath;
		this.httpReqContentType = p.httpReqContentType;
		this.httpReqBody = p.httpReqBody;
		this.paramNames = p.paramNames;
		
		this.key.form(p.key);
	}
	
	public String toJson(){
		StringBuffer sb = new StringBuffer("{");
		Field[] fields = this.getClass().getDeclaredFields();
		
		for(int i =0; i < fields.length; i++){
			Field f = fields[i];
			if(Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())){
				continue;
			}
			try {
				Object v = f.get(this);
				sb.append(f.getName()).append(":").append(v == null?"":v.toString());
				if(i != fields.length-1){
					sb.append(",");
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("toJson service mehtod error: "+f.getName());
			}
		}
		
		return sb.toString();
	}
	
	public void fromJson(String mstr){
		mstr = mstr.substring(1,mstr.length()-1);
		String[] kvs = mstr.split(",");
		Class<?> cls = this.getClass();
		for(String kv: kvs){
			String[] ms = kv.split(":");
			if(ms.length < 1){
				throw new CommonException("Parse service mehtod error: "+mstr);
			}
			if(ms.length == 1){
				continue;
			}
			try {
				Field f = cls.getDeclaredField(ms[0]);
				f.setAccessible(true);
				if(f.getType() == String.class){
					f.set(this, ms[1]);
				}else if(f.getType() == Integer.TYPE){
					f.set(this, Integer.parseInt(ms[1]));
				}else if(f.getType() == Boolean.TYPE){
					f.set(this, Boolean.parseBoolean(ms[1]));
				}else if(f.getType() == Float.TYPE){
					f.set(this, Float.parseFloat(ms[1]));
				}else if(f.getType() == Double.TYPE){
					f.set(this, Double.parseDouble(ms[1]));
				}else if(f.getType() == Byte.TYPE){
					f.set(this, Byte.parseByte(ms[1]));
				}else if(f.getType() == Short.TYPE){
					f.set(this, Short.parseShort(ms[1]));
				}else if(f.getType() == Character.TYPE){
					f.set(this,ms[1]);
				}
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				throw new CommonException("Parse service mehtod error: "+mstr,e);
			}
		}
	}
	
	public String methodID() {
		return this.key.toKey(false, false, false,false, false, true,true);
	}
	
	
	@Override
	public int hashCode() {
		return this.key==null?"".hashCode():this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	public boolean isRsa() {
		return encType==1;
	}

}