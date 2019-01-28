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
package org.jmicro.server;

import java.util.List;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IFactoryListener;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.common.Constants;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月9日-下午5:51:55
 */
@Component(active=true,value="InitServerReceiver")
public class InitServerReceiver implements IFactoryListener{

	@Override
	public void afterInit(IObjectFactory of) {
		List<IMessageHandler> list = of.getByParent(IMessageHandler.class);
		ServerMessageReceiver sr = of.get(ServerMessageReceiver.class);
		for(IMessageHandler h: list){
			Class<?> tcls = ProxyObject.getTargetCls(h.getClass());
			if(tcls.isAnnotationPresent(Component.class)){
				Component anno = tcls.getAnnotation(Component.class);
				if(anno.active() && Constants.SIDE_PROVIDER.equals(anno.side())){
					sr.registHandler(h);
				}
			}
		}
	}
	
	@Override
	public int runLevel() {
		return 0;
	}

	@Override
	public void preInit(IObjectFactory of) {
	}
	
	
}
