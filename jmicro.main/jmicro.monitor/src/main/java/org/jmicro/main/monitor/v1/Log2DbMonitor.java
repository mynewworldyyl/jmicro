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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.v1.AbstractMonitorDataSubscriber;
import org.jmicro.api.monitor.v1.IMonitorDataSubscriber;
import org.jmicro.api.monitor.v1.MonitorConstant;
import org.jmicro.api.monitor.v1.SubmitItem;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月18日
 */
//@Component
//@Service(version="0.0.1", namespace="log2DbMonitor",monitorEnable=0,handler=Constants.SPECIAL_INVOCATION_HANDLER)
public class Log2DbMonitor  extends AbstractMonitorDataSubscriber  implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(Log2DbMonitor.class);
	
	@Cfg(value="/Monitor/Log2DbMonitor/enable",defGlobal=false)
	private boolean enable = true;
	
	@Cfg(value="/Log2DbMonitor/openDebug",defGlobal=false)
	private boolean openDebug=false;
	
	@Inject
	private MongoDatabase mongoDb;
	
	private List<SubmitItem> siq = new LinkedList<>();
	
	@JMethod("init")
	public void init() {
		new Thread(this::doLog).start();
	}
	
	private void doLog() {
		while(true) {
			try {
				synchronized(siq) {
					saveLog();
				}
				try {
					Thread.sleep(2000);;
				} catch (Exception e) {
				}
			}catch(Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private void saveLog() {
		if(this.openDebug) {
			logger.debug("printLog One LOOP");
		}
		
		if(siq == null || siq.isEmpty()) {
			return;
		}
		
		List<Document> docs = new ArrayList<>();
		synchronized(siq) {
			Iterator<SubmitItem> itesm = siq.iterator();
			for(;itesm.hasNext();) {
				Document d = toLog(itesm.next());
				if(d != null) {
					docs.add(d);
				}
				itesm.remove();
			}
		}
		
		MongoCollection<Document> coll = mongoDb.getCollection("linker_log");
		coll.insertMany(docs);
	}
	

	private Document toLog(SubmitItem si) {
		Document d = Document.parse(JsonUtils.getIns().toJson(si));
		return d;
	}

	@Override
	public Short[] intrest() {
		return new Short[]{MonitorConstant.LINKER_ROUTER_MONITOR};
	}
	
	@Override
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(SubmitItem[] sis) {
		
		for(SubmitItem si : sis) {
			try {
			if(openDebug) {
				logger.debug("LinkRouterMonitor:{}",si);
			}
			
			if(si.getType() != MonitorConstant.LINKER_ROUTER_MONITOR) {
				logger.warn("LinkRouterMonitor LOG TYPE ERROR:{}",si);
				return;
			}
			
			synchronized(siq) {
				siq.add(si);
			}
			} catch (Throwable e) {
				logger.error("LinkRouterMonitor GOT ERROR:" + si.toString(),e);
			}
		}
		
	}
	
}
