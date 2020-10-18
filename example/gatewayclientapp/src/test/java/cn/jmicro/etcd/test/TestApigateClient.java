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
package cn.jmicro.etcd.test;

import java.util.Timer;
import java.util.TimerTask;

import org.junit.BeforeClass;
import org.junit.Test;

import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.gateway.client.ApiGatewayClient;
import cn.jmicro.gateway.client.ApiGatewayConfig;
import cn.jmicro.gateway.pubsub.ApiGatewayPubsubClient;
import cn.jmicro.gateway.pubsub.PSDataListener;

public class TestApigateClient {
	
	//主题
	private String TOPIC = "/jmicro/test/topic01";
	
	//private ApiGatewayClient socketClient = new ApiGatewayClient(new ApiGatewayConfig(Constants.TYPE_HTTP,"jmicro.cn",80));
	private ApiGatewayClient socketClient = null;
	
	@BeforeClass
	public void setUp() {
		ApiGatewayClient.initClient(new ApiGatewayConfig(Constants.TYPE_SOCKET,"jmicro.cn",9092));
		socketClient =  ApiGatewayClient.getClient();
	}
	
	//账号名
	private static final String ACT = "test01";
	//密码
	private static final String PWD = "1";
	
	@Test
	public void testLoginLogout() {
		socketClient.loginJMAsync(ACT, PWD)
		.success((resp,cxt)->{
			System.out.println("Success login"+resp.getData().getActName());
			socketClient.logoutJMAsync()
			.then((succ,fail0,cxt0)->{
				if(fail0 == null) {
					System.out.println("Success logout: "+succ);
				}else {
					System.out.println("Fail logout: "+fail0.toString());
				}
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testPublishString() {
		socketClient.loginJMAsync(ACT, PWD)
		.success((resp,cxt)->{
			System.out.println("Success login: "+resp.getData().getActName());
			 socketClient.getPubsubClient()
			.publishStringJMAsync(TOPIC, "Message from java client!",PSData.FLAG_DEFALUT,null)
			.success((id,cxt0)->{
				System.out.println("Publish result: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testPublishByte() {
		socketClient.loginJMAsync(ACT, PWD)
		.success((resp,cxt)->{
			System.out.println("Success login: "+resp.getData().getActName());
			 socketClient.getPubsubClient()
			.publishBytesJMAsync(TOPIC, "Message from java client!".getBytes(),PSData.FLAG_DEFALUT,null)
			.success((id,cxt0)->{
				System.out.println("Publish result: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testPublishMutilItems() {
		socketClient.loginJMAsync(ACT, PWD)
		.success((resp,cxt)->{
			System.out.println("Success login: "+resp.getData().getActName());
			
			 PSData pd = new PSData();
			 pd.setTopic(TOPIC);
			 pd.setData("Message from java client!");
			 
			 socketClient.getPubsubClient()
			//.publishOneItemJMAsync(null, pd)
			.publishMutilItemsJMAsync(new PSData[] {pd})
			.success((id,cxt0)->{
				System.out.println("Publish result: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
			});
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testPublishStringPresure() {
		String actName = ACT;
		socketClient.loginJMAsync(ACT, PWD)
		.success((resp,cxt)->{
			
			System.out.println("Success login: "+resp.getData().getActName());
			
			ApiGatewayPubsubClient cl = socketClient.getPubsubClient();
			 
			new Timer().scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					cl.publishStringJMAsync(TOPIC, "Message from java "+actName,PSData.FLAG_DEFALUT,null)
					.success((id,cxt0)->{
						System.out.println("Publish result: "+id);
					})
					.fail((code,msg,cxt1)->{
						System.out.println("Fail pubilish content: code: "+ code + ", msg: " + msg);
					});
				}
			}, 10, 1000);
			 
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		Utils.getIns().waitForShutdown();
	}
	
	
	@Test
	public void testSubscribeTopic() {
		PSDataListener lis = new PSDataListener() {
			int id = 0;
			
			@Override
			public void onMsg(PSData item) {
				System.out.println("Got message: " + item.getData().toString());
			}

			@Override
			public int getSubId() {
				return id;
			}

			@Override
			public void setSubId(int id) {
				this.id = id;
			}
			
		};
		
		socketClient.loginJMAsync(ACT, PWD)
		.success((resp,cxt)->{
			
			System.out.println("Success login: "+resp.getData().getActName());
			
			socketClient.getPubsubClient()
			.subscribeJMAsync(TOPIC, null, lis)
			.success((id,cxt0)->{
				System.out.println("Subscribe success: "+id);
			})
			.fail((code,msg,cxt1)->{
				System.out.println("Fail to subscribe code: "+ code + ", msg: " + msg);
			});
			 
		})
		.fail((code,msg,cxt)->{
			System.out.println("Fail login: code"+ code + ", msg: " + msg);
		});
		
		Utils.getIns().waitForShutdown();
	}
	
	
}
