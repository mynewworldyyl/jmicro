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
package cn.jmicro.main.monitor.v2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.gateway.ApiRequest;
import cn.jmicro.api.gateway.ApiResponse;
import cn.jmicro.api.idgenerator.IdRequest;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcLogItem;
import cn.jmicro.api.monitor.MRpcStatisItem;
import cn.jmicro.api.monitor.OneLog;
import cn.jmicro.api.monitor.StatisItem;
import cn.jmicro.api.monitor.StatisMonitorClient;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IResponse;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.monitor.api.AbstractMonitorDataSubscriber;

/**
 * 
 * @author Yulei Ye
 * @date 2018年11月19日 下午1:25:24
 */
/*@Component
@Service(version="0.0.1", namespace="printLogMonitor",monitorEnable=0,handler=Constants.SPECIAL_INVOCATION_HANDLER)
*/public class LinkRouterMonitor extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(LinkRouterMonitor.class);
	
	@Cfg("/LinkRouterMonitor/enable")
	private boolean enable = false;
	
	@Cfg("/LinkRouterMonitor/openDebug")
	private boolean openDebug = false;
	
	//响应时间大于此时间，则要输了出日志,单位毫秒
	@Cfg("/LinkRouterMonitor/timeOutLogLong")
	private long timeOutLogLong = 1*1000;
	
	@Inject
	private StatisMonitorClient mm;
	
	private Map<String,List<MRpcStatisItem>> siq = new HashMap<>();
	
	private Map<String,Long> sldTimes = new HashMap<>();
	
	@Inject
	private IDataOperator op;
	
	@JMethod("ready")
	public void ready() {
		String skey = this.skey("printLogMonitor", "0.0.1");
		registType(op,skey, MC.MT_TYPES_ARR);
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
		for(List<MRpcStatisItem> ls : siq.values()) {
			
			if(ls.isEmpty()) {
				continue;
			}
			
			for(MRpcStatisItem si: ls) {
				
				String key= si.getKey();
				if(key == null) {
					key = si.getInstanceName();
				}
				
				boolean flag = false;
				long minTime =TimeUtils.getCurTime();
				long maxTime = TimeUtils.getCurTime();
				
				/*
				for(StatisItem o: si.getTypeStatis().values()) {
					if(o.getTime() < minTime) {
						minTime = o.getTime();
					}
					
					if(o.getTime() > maxTime) {
						maxTime = o.getTime();
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
					
				List<StatisItem> l = si.getItems();
				if(si.getItems().size()> 1) {
					l.sort((o1,o2)->o1.getTime() > o2.getTime() ? 1 : (o1.getTime() == o2.getTime()?0:-1));
				}
				*/
				
				logger.info(key+",Take["+(maxTime-minTime)+"(MS)]======================================================");
				
				for(Iterator<StatisItem> ite = si.getTypeStatis().values().iterator(); ite.hasNext();) {
					StatisItem oi = ite.next();
					ite.remove();
					if(oi != null) {
						String msg = toLog(si,oi);
						logger.info(msg);
					}
				}
			}
		
			ls.clear();
		}
	}

	private void doPrintLog(MRpcStatisItem si,StatisItem oi) {
		String msg = toLog(si,oi);
		logger.info(msg);
		/*switch(oi.getLevel()) {
		case MC.LOG_TRANCE:
			logger.trace(msg);
			break;
		case MC.LOG_DEBUG:
			logger.debug(msg);
			break;
		case MC.LOG_INFO:
			logger.info(msg);
			break;
		case MC.LOG_WARN:
			logger.warn(msg);
			break;
		case MC.LOG_ERROR:
			logger.error(msg);
			break;
		case MC.LOG_FINAL:
			logger.error(msg);
			break;
		}*/
	}

	private void cleanLinkId() {
		if(sldTimes.isEmpty()) {
			return;
		}
		long curTime = TimeUtils.getCurTime();
		for(Iterator<Map.Entry<String, Long>> ite = sldTimes.entrySet().iterator(); ite.hasNext(); ) {
			Map.Entry<String, Long> e = ite.next();
			//超过5分钟的链路直接清除，一个调用链路超过5分钟都还没结束，日志将有缺失
			if(curTime - e.getValue() > 300000) {
				ite.remove();
				siq.remove(e.getKey());
			}
		}
	}

	@Override
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(MRpcStatisItem[] sis) {
		
			for(MRpcStatisItem si : sis) {
				try {
				if(openDebug) {
					logger.debug("LinkRouterMonitor:{}",si);
				}
				
				/*if(si.getType() != MonitorConstant.LINKER_ROUTER_MONITOR) {
					logger.warn("LinkRouterMonitor LOG TYPE ERROR:{}",si);
					return;
				}*/
				
				String key = si.getKey();
				if(key == null) {
					key = si.getInstanceName();
				}
				
				if(!siq.containsKey(key)) {
					synchronized(siq) {
						if(!siq.containsKey(key)) {
							siq.put(key, new LinkedList<MRpcStatisItem>());
						}
					}
				}
				
				List<MRpcStatisItem> l = siq.get(key);
				synchronized(l) {
					l.add(si);
					sldTimes.put(key, TimeUtils.getCurTime());
				}
				} catch (Throwable e) {
					logger.error("LinkRouterMonitor GOT ERROR:" + si.toString(),e);
				}
			}
		
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

	private void response(StringBuilder sb, MRpcLogItem si,OneLog oi) {
		if(si.getResp() == null) return;
		if(si.getResp() instanceof IResponse) {
			sb.append("[RpcResponse] ");
			//others(sb,oi.getOthers());
			logHeaders(sb,si);
			rpcResponse(sb,(IResponse)si.getResp());
		}else if(si.getResp() instanceof ApiResponse) {
			sb.append("[ApiResponse] ");
			//others(sb,oi.getOthers());
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

	private StringBuilder logHeaders(StringBuilder sb,MRpcLogItem si) {
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
		sb.append(" service[").append(UniqueServiceKey.serviceName(sn, ns, v))
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
