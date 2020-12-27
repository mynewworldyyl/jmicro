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
package cn.jmicro.example.comsumer;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.example.api.rpc.ISimpleRpc;
import cn.jmicro.example.api.rpc.genclient.ISimpleRpc$JMAsyncClient;

/**
 * 
 * @author Yulei Ye
 *
 * @date: 2018年11月10日 下午9:23:25
 */
public class ServiceComsumer {

	public static void main(String[] args) {
		
		IObjectFactory of = JMicro.getObjectFactoryAndStart(args);
		//JMicroContext.get().setParam("routerTag", "tagValue");
		
		//got remote service from object factory
		//ISimpleRpc src = of.getRemoteServie(ISimpleRpc.class,null);
		LG.log(MC.LOG_DEBUG, ServiceComsumer.class, "test submit nonrpc log");
		//ISimpleRpc src = of.get(ISimpleRpc.class);
		ISimpleRpc$JMAsyncClient src = (ISimpleRpc$JMAsyncClient)of.get(ISimpleRpc.class);
		//invoke remote service
		//System.out.println(src.hello("Hello JMicro"));
		/*src.helloJMAsync("Hello JMicro").then((rst, fail,ctx)->{
			System.out.println(rst);
		});*/
		
		src.linkRpc("Hello Link RPC")
		.then((rst, fail,ctx)->{
			System.out.println(rst);
		});
		
		/*src.linkRpcAs("test out linkRpcAs")
		.success((rst,cxt)->{
			System.out.println(rst);

			src.linkRpcAs("inner linkRpcAs0")
			.success((rst0,cxt0)->{
				System.out.println(rst);
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code: " + code +", msg: " + msg);
			});
			
			src.linkRpcAs("inner linkRpcAs1")
			.success((rst0,cxt0)->{
				System.out.println(rst);
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code: " + code +", msg: " + msg);
			});
			
			src.linkRpcAs("inner linkRpcAs2")
			.success((rst0,cxt0)->{
				System.out.println(rst);
			})
			.fail((code,msg,cxt0)->{
				System.out.println("code: " + code +", msg: " + msg);
			});
			
		})
		.fail((code,msg,cxt)->{
			System.out.println("code: " + code +", msg: " + msg);
		});*/
		
		try {
			Thread.sleep(1000*30);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
