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
package org.jmicro.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.jmicro.api.Config;
import org.jmicro.api.JMicroContext;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:10:34
 */
public class ZKCon {

	private static ZKCon ins = new ZKCon();
	public static ZKCon getIns() {return ins;}
	private ZKCon(){
		propes = new Properties();
		curator = createCuratorFramework();
	}
	
	private CuratorFramework curator = null;
	
	private Properties propes = null;
	
	public CuratorFramework getCurator() {
		return curator;
	}
	private CuratorFramework createCuratorFramework() {
	    CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
	    //getConfig();
	    
	    ACLProvider aclProvider = new ACLProvider() {  
	        private List<ACL> acl ;  
	        @Override  
	        public List<ACL> getDefaultAcl() {
	            if(acl ==null){
	                ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
	               // acl.clear();
	                //acl.add(new ACL(Perms.ALL, new Id("auth", propes.getProperty("auth")) ));
	                this.acl = acl;
	            }
	            return acl;
	        }  
	        @Override  
	        public List<ACL> getAclForPath(String path) {  
	            return acl;
	        }  
	    };  
	    builder.aclProvider(aclProvider);
	    
	    String connectString = Config.getRegistryUrl().getHost() + ":" + Config.getRegistryUrl().getPort();
	    builder.connectString(connectString);
	    
	    ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
	    builder.retryPolicy(retryPolicy);

	    if(propes.getProperty("sessionTimeout") != null) {
	    	Integer sessionTimeout = Integer.parseInt(propes.getProperty("sessionTimeout"));
		    if (sessionTimeout != null) {
		      builder.sessionTimeoutMs(sessionTimeout);
		    }
	    }
	    

	    String namespace = propes.getProperty("namespace");
	    if(namespace != null) {  
	    	 builder.namespace(namespace);
	    }
	    
	    String schema = propes.getProperty("schema");
	    String auth = propes.getProperty("auth");
	    if (schema != null && auth != null) {
	    	 builder.authorization(schema, auth.getBytes());
	    }
	    
	    curator = builder.build();
	    curator.start();
	    return curator;
	  }
	
	private void getConfig(){
		//propes = new Properties();
		//propes.put("connectString", "localhost:2180");
		//propes.put("auth", "electric:electric123");
		//propes.put("schema", "digest");
		
		//propes.put("rootPath", "/jmicro/config");
		//propes.put("prefix", "Test-");
		
		/*propes.put("sshAccount", "electric");
		propes.put("sshHost", "180.153.50.130");
		propes.put("sshPassword", "electric123");*/
		
		/*propes.put("sshAccount", "redis");
		propes.put("sshHost", "192.168.1.233");
		propes.put("sshPassword", "redis");
		
		propes.put("sshPort", "22");
		propes.put("cmd", "ls -l\nexit\n");*/
		
		
	}
}
