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
package org.jmicro.main.monitor.v1;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.IdRequest;
import org.jmicro.api.monitor.v1.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.v1.IMonitorDataSubscriber;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SubmitItem;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.UniqueServiceKey;
import org.jmicro.common.Constants;
import org.jmicro.common.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午1:25:24
 */
//@Component
//@Service(version="0.0.1", namespace="printLogMonitor",monitorEnable=0,handler=Constants.SPECIAL_INVOCATION_HANDLER)
public class LinkRouterMonitor  extends AbstractMonitorDataSubscriber  implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(LinkRouterMonitor.class);
	
	@Cfg("/LinkRouterMonitor/enable")
	private boolean enable = false;
	
	@Cfg("/LinkRouterMonitor/openDebug")
	private boolean openDebug=false;
	
	//响应时间大于此时间，则要输了出日志,单位毫秒
	@Cfg("/LinkRouterMonitor/timeOutLogLong")
	private long timeOutLogLong = 1*1000;
	
	private Map<Long,List<SubmitItem>> siq = new HashMap<>();
	
	private Map<Long,Long> sldTimes = new HashMap<>();
	
	@JMethod("init")
	public void init() {
		new Thread(this::doLog).start();
	}
	
	private void doLog() {
		while(true) {
			try {
				synchronized(siq) {
					cleanLinkId();
					printLog();
				}
				try {
					Thread.sleep(3000);;
				} catch (Exception e) {
				}
			}catch(Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private void printLog() {
		/*if(this.openDebug) {
			logger.debug("printLog One LOOP");
		}*/
		for(Map.Entry<Long, List<SubmitItem>> e : siq.entrySet()) {
			
			List<SubmitItem> l = e.getValue();
			if(l.isEmpty()) {
				continue;
			}
			
			synchronized(l) {
				
				boolean flag = false;
				long minTime = System.currentTimeMillis();
				long maxTime = System.currentTimeMillis();
				
				for(SubmitItem si: l) {
					
					/*if(si.getLog().getLevel() >= MonitorConstant.LOG_WARN) {
						//LOG级别在警告以上，直接输出
						flag = true;
					}*/
					
					if(si.getTime() < minTime) {
						minTime = si.getTime();
					}
					
					if(si.getTime() > maxTime) {
						maxTime = si.getTime();
					}
				}
				
				if(!flag && timeOutLogLong <= 0) {
					flag = true;
				}else if(!flag) {
					flag = (maxTime-minTime) > timeOutLogLong;
				}
				
				if(!flag) {
					continue;
				}
					
				if(l.size()> 1) {
					l.sort((o1,o2)->o1.getTime() > o2.getTime() ? 1 : (o1.getTime() == o2.getTime()?0:-1));
				}
				
				logger.info(e.getKey()+",Take["+(maxTime-minTime)+"(MS)]======================================================");
				
				for(Iterator<SubmitItem> ite = l.iterator(); ite.hasNext();) {
					SubmitItem si = ite.next();
					ite.remove();
					
					switch(si.getType()) {
					case MonitorConstant.LINKER_ROUTER_MONITOR:
						doPrintLog(si);
						break;
					case MonitorConstant.SERVER_START:
						String msg = toLog(si);
						logger.info(msg);
						break;
					}
					
				}
			}
		}
	}

	private void doPrintLog(SubmitItem si) {
		String msg = toLog(si);
		switch(si.getLevel()) {
		case MonitorConstant.LOG_TRANCE:
			logger.trace(msg);
			break;
		case MonitorConstant.LOG_DEBUG:
			logger.debug(msg);
			break;
		case MonitorConstant.LOG_INFO:
			logger.info(msg);
			break;
		case MonitorConstant.LOG_WARN:
			logger.warn(msg);
			break;
		case MonitorConstant.LOG_ERROR:
			logger.error(msg);
			break;
		case MonitorConstant.LOG_FINAL:
			logger.error(msg);
			break;
		}
	}

	private void cleanLinkId() {
		if(sldTimes.isEmpty()) {
			return;
		}
		long curTime = System.currentTimeMillis();
		for(Iterator<Map.Entry<Long, Long>> ite = sldTimes.entrySet().iterator(); ite.hasNext(); ) {
			Map.Entry<Long, Long> e = ite.next();
			//超过5分钟的链路直接清除，一个调用链路超过5分钟都还没结束，日志将有缺失
			if(curTime - e.getValue() > 300000) {
				ite.remove();
				siq.remove(e.getKey());
			}
		}
	}

	@Override
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(SubmitItem[] sis) {
		
			for(SubmitItem si : sis) {
				try {
				if(openDebug) {
					logger.debug("LinkRouterMonitor:{}",si);
				}
				
				/*if(si.getType() != MonitorConstant.LINKER_ROUTER_MONITOR) {
					logger.warn("LinkRouterMonitor LOG TYPE ERROR:{}",si);
					return;
				}*/
				
				if(!siq.containsKey(si.getLinkId())) {
					synchronized(siq) {
						if(!siq.containsKey(si.getLinkId())) {
							siq.put(si.getLinkId(), new LinkedList<SubmitItem>());
						}
					}
				}
				
				List<SubmitItem> l = siq.get(si.getLinkId());
				synchronized(l) {
					l.add(si);
					sldTimes.put(si.getLinkId(), System.currentTimeMillis());
				}
				} catch (Throwable e) {
					logger.error("LinkRouterMonitor GOT ERROR:" + si.toString(),e);
				}
			}
		
	}

	private String toLog(SubmitItem si) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Ins [").append(si.getInstanceName())
		.append("] Tag [ ").append(si.getTag())
		.append("] lid[").append(si.getLinkId())
		.append("] T[ ").append(DateUtils.formatDate(new Date(si.getTime()), 
				DateUtils.PATTERN_HHMMSSZZZ)).append("] ")
		.append("desc[").append(si.getDesc()).append("]");
		if(si.getReq() != null) {
			if(si.getReq() instanceof IRequest) {
				sb.append("[IRequest] ");
				others(sb,si.getOthers());
				logHeaders(sb,si);
				reqeust(sb,(IRequest)si.getReq());
			}else if(si.getReq() instanceof ApiRequest) {
				sb.append("[ApiRequest] ");
				others(sb,si.getOthers());
				logHeaders(sb,si);
				reqeust(sb,(ApiRequest)si.getReq());
			}else if(si.getReq() instanceof IdRequest) {
				sb.append("[IdRequest] ");
				others(sb,si.getOthers());
				logHeaders(sb,si);
				reqeust(sb,(IdRequest)si.getReq());
			}
		} else if(si.getMsg() != null) {
			sb.append("[Message] ");
			others(sb,si.getOthers());
			logHeaders(sb,si);
			message(sb,si.getMsg());
		}else if(si.getResp() != null) {
			response(sb,si);
		} else {
			others(sb,si.getOthers());
			logHeaders(sb,si);
		}
		
		return sb.toString();
	}

	private void reqeust(StringBuilder sb, IdRequest req) {
		if(req == null) return;
		sb.append("class[").append(req.getClazz()).append("]");
		sb.append("num[").append(req.getNum()).append("]");
	}

	private void reqeust(StringBuilder sb, ApiRequest req) {
		if(req == null) return;
		sb.append("reqId[").append(req.getReqId()).append("]");
		service(sb, req.getServiceName(), req.getNamespace(), req.getVersion(), req.getMethod(),
				req.getArgs());
	}

	@Override
	public Short[] intrest() {
		return new Short[]{MonitorConstant.LINKER_ROUTER_MONITOR,MonitorConstant.SERVER_START,
				MonitorConstant.SERVER_START,MonitorConstant.REQ_ERROR,MonitorConstant.CLIENT_SERVICE_ERROR,
				MonitorConstant.CLIENT_RESPONSE_SERVER_ERROR};
	}

	private void response(StringBuilder sb, SubmitItem si) {
		if(si.getResp() == null) return;
		if(si.getResp() instanceof IResponse) {
			sb.append("[RpcResponse] ");
			others(sb,si.getOthers());
			logHeaders(sb,si);
			rpcResponse(sb,(IResponse)si.getResp());
		}else if(si.getResp() instanceof ApiResponse) {
			sb.append("[ApiResponse] ");
			others(sb,si.getOthers());
			logHeaders(sb,si);
			apiResponse(sb,(ApiResponse)si.getResp());
		}
	}

	private void apiResponse(StringBuilder sb, ApiResponse resp) {
		sb.append("reqId[").append(resp.getReqId()).append("] success[")
		.append(resp.isSuccess()).append("] result[")
		.append(resp.getResult()).append("]");
	}

	private void rpcResponse(StringBuilder sb, IResponse resp) {
		sb.append("reqId[").append(resp.getRequestId()).append("] success[")
		.append(resp.isSuccess()).append("] monitorable[")
		.append(resp.isMonitorEnable()).append("] result[")
		.append(resp.getResult()).append("]");
	}

	private void reqeust(StringBuilder sb, IRequest req) {
		if(req == null) return;
		sb.append("reqId[").append(req.getRequestId()).append("]");
		service(sb, req.getServiceName(), req.getNamespace(), req.getVersion(), req.getMethod(),
				req.getArgs());
	}

	private StringBuilder logHeaders(StringBuilder sb,SubmitItem si) {
		//sb.append("[").append(si.getTagCls());
		//sb.append("] LinkId [").append(si.getLinkId());
		//sb.append(" instanceName[").append(si.getInstanceName());
		sb.append("] localHost [").append(si.getLocalHost());
		sb.append("] localPort [").append(si.getLocalPort());
		sb.append("] remoteHost [").append(si.getRemoteHost());
		sb.append("] remotePort [").append(si.getRemotePort()).append("] ");
		return sb;
	}
	
	private void service(StringBuilder sb,String sn,String ns,String v,String method,Object[] args) {
		sb.append(" service[").append(UniqueServiceKey.serviceName(sn, ns, v).toString())
		.append("&").append(method).append("] args[");
		if(args != null && args.length > 0) {
			for(int i=0; i< args.length;i++) {
				sb.append(args[i].getClass()).append("=").append(args[i]);
				if(i != args.length-1) {
					sb.append("&");
				}
			}
		}
		sb.append("] ");
	}
	
	private void others(StringBuilder sb, Object[] others) {
		if(others == null || others.length == 0) return;
		if(others != null && others.length > 0) {
			sb.append("Others[");
			for(int i=0; i< others.length;i++) {
				sb.append(others[i]);
				if(i != others.length-1) {
					sb.append("&");
				}
			}
		}
		sb.append("]");
	}
	
	private void message(StringBuilder sb,  Message msg) {
		if(msg == null) return;
		sb.append(" msgId[").append(msg.getId())
		.append("] reqId[").append(msg.getReqId())
		.append("] version[").append(msg.getVersion())
		.append("] type[").append(Integer.toHexString(msg.getType()))
		.append("] flag[").append(Integer.toHexString(msg.getFlag()))
		//.append("] Stream[").append(msg.isStream())
		.append("] Monitorable[").append(msg.isMonitorable())
		.append("] response[").append(msg.isMonitorable())
		.append("] loggable[").append(msg.getLogLevel())
		.append("] level[").append(msg.getPriority())
		.append("] up protocol[").append(msg.getUpProtocol())
		.append("] down protocol[").append(msg.getDownProtocol())
		.append("] payload]").append(msg.getPayload())
		;
	}
	
}
