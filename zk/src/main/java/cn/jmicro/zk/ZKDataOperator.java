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
package cn.jmicro.zk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IConnectionStateChangeListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.INodeListener;
import cn.jmicro.common.Constants;
import cn.jmicro.zk.children.ChildrenManager;
import cn.jmicro.zk.data.DataManager;
import cn.jmicro.zk.node.NodeManager;

/**
 * 1 每个结点每种事件都只设置一个监听器,业务代码可自由在此增加监听器,由此做事件分发
 * 2 不保存结点信息
 * 3 不对线程安全做保证
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:10:34
 */
//@Component(value=Constants.DEFAULT_DATA_OPERATOR,level=0,active=true,lazy=false)
public class ZKDataOperator implements IDataOperator{

	private final static Logger logger = LoggerFactory.getLogger(ZKDataOperator.class);
	
	private boolean openDebug = false;
	
	//only use for testing
	private static ZKDataOperator ins = new ZKDataOperator();
	
	public static ZKDataOperator getIns() {return ins;}
	
	private boolean isInit = false;
	
	private CuratorFramework curator = null;
	
	private NodeManager nodeManager;
	
	private DataManager dataManager;
	
	private ChildrenManager childrenManager;
	
	public ZKDataOperator(){}
	
	public void init(){
		if(isInit){
			return;
		}
		isInit = true;
		propes = new Properties();
		curator = createCuratorFramework();
		
		this.nodeManager = new NodeManager(this,curator,openDebug);
		this.dataManager = new DataManager(this,curator,openDebug);
		this.childrenManager = new ChildrenManager(this,curator,openDebug);
	}
	
	@Override
	public void objectFactoryStarted(IObjectFactory of) {
		of.regist(CuratorFramework.class, curator);
	}
	
	//连接状态监听器，连上，断开，重连
	//若增加监听器时，边接已经建立，则立即给一个连接通知，否则不给连接通知
	private Set<IConnectionStateChangeListener> connListeners = new HashSet<>();
	
	private Properties propes = null;
	
	@Override
	public void removeNodeListener(String path, INodeListener lis) {
		this.nodeManager.removeNodeListener(path, lis);
	}
	
	public void addNodeListener(String path, INodeListener lis) {
		this.nodeManager.addNodeListener(path, lis);
	}
	
	@Override
	public void addDataListener(String path, IDataListener lis) {
		this.dataManager.addDataListener(path, lis);
	}

	@Override
	public void removeDataListener(String path, IDataListener lis) {
		this.dataManager.removeDataListener(path, lis);
	}
	
	@Override
	public void addChildrenListener(String path, IChildrenListener lis) {
		this.childrenManager.addChildrenListener(path, lis);
	}

	@Override
	public void removeChildrenListener(String path, IChildrenListener lis) {
		this.childrenManager.removeChildrenListener(path, lis);
		
	}

	public void addListener(IConnectionStateChangeListener lis){
		connListeners.add(lis);
		if(this.curator.getState() == CuratorFrameworkState.STARTED) {
			lis.stateChanged(Constants.CONN_CONNECTED);
		}else if(this.curator.getState() == CuratorFrameworkState.STOPPED) {
			lis.stateChanged(Constants.CONN_LOST);
		}
	}
	
	public Set<String> getChildrenFromCache(String path){
		return this.childrenManager.getChildrenFromCache(path);
	}
	
	public Set<String> getChildrenFromRaft(String path){
		return this.childrenManager.getChildrenFromRaft(path);
	}
	
	public Set<String> getChildren(String path,boolean fromCache){
	   return this.childrenManager.getChildren(path, fromCache);
	}
	
	@Override
	public boolean exist(String path) {
		return this.nodeManager.exist(path);
	}

	@Override
	public String getData(String path) {
		return this.nodeManager.getData(path);
	}

	@Override
	public void setData(String path, String data) {
		this.childrenManager.removeCache(path);
		this.nodeManager.setData(path, data);
	}

	@Override
	public void createNode(String path, String data, boolean elp) {
		this.nodeManager.createNode(path, data, elp);
	}

	@Override
	public void deleteNode(String path) {
		this.childrenManager.removeCache(path);
		this.nodeManager.deleteNode(path);
	}
	
	public <L> boolean existsListener(Set<L> listeners,L lis){
		for(L l : listeners){
			if(l == lis){
				return true;
			}
		}
		return false;
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
	    
	    String connectString = Config.getRegistryHost() /*+ ":" + Config.getRegistryPort()*/;
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
	    
	    curator.getConnectionStateListenable().addListener((cf,state)->stateChanged(state));
	    
	    curator.start();
	    
	    return curator;
	  }
	
	protected void stateChanged(ConnectionState state){
		 int s = 0;
		 if(state == ConnectionState.LOST) {
			 s = Constants.CONN_LOST;
         } else if (state == ConnectionState.CONNECTED) {
        	 s = Constants.CONN_CONNECTED;
        	 //registListeners();
         } else if (state == ConnectionState.RECONNECTED) {
        	 s = Constants.CONN_RECONNECTED;
        	 //registListeners();
         }
		 if(s!= 0){
			 for(IConnectionStateChangeListener l : this.connListeners){
				 l.stateChanged(s);
			 }
		 }
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
