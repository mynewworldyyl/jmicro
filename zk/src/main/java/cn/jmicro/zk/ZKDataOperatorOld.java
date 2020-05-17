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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IConnectionStateChangeListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.INodeListener;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;

/**
 * 1 每个结点每种事件都只设置一个监听器,业务代码可自由在此增加监听器,由此做事件分发
 * 2 不保存结点信息
 * 3 不对线程安全做保证
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:10:34
 */
//@Component(value=Constants.DEFAULT_DATA_OPERATOR,level=0,active=true,lazy=false)
public class ZKDataOperatorOld implements IDataOperator{

	private final static Logger logger = LoggerFactory.getLogger(ZKDataOperatorOld.class);
	
	private boolean OPEN_DEBUG = true;
	
	//only use for testing
	private static ZKDataOperatorOld ins = new ZKDataOperatorOld();
	
	public static ZKDataOperatorOld getIns() {return ins;}
	
	private boolean isInit = false;
	
	public ZKDataOperatorOld(){}
	
	public void init(){
		if(isInit){
			return;
		}
		isInit = true;
		propes = new Properties();
		curator = createCuratorFramework();
	}
	
	@Override
	public void objectFactoryStarted(IObjectFactory of) {
		of.regist(CuratorFramework.class, curator);
	}

	//路径到子结点之间关系，Key是全路径，值只包括结点名称
	private Map<String,Set<String>> path2Children = new ConcurrentHashMap<>();
	
	//连接状态监听器，连上，断开，重连
	//若增加监听器时，边接已经建立，则立即给一个连接通知，否则不给连接通知
	private Set<IConnectionStateChangeListener> connListeners = new HashSet<>();
	
	//子结点监听器
	private Map<String,Set<IChildrenListener>> childrenListeners = new HashMap<>();
	
	//数据改变监听器
	private  Map<String,Set<IDataListener>> dataListeners = new HashMap<>();
	
	/**
	 * 结点增加或删除监听器
	 * 结点创建和结点删除，只针对指定的path做监听
	 */
	private  Map<String,Set<INodeListener>> nodeListeners = new HashMap<>();
	
	private CuratorFramework curator = null;
	
	private Properties propes = null;
	
	private final Watcher watcher =(WatchedEvent event)->{
		   String path = event.getPath();
	      //logger.info("Watcher for '{}' received watched event: {}",path, event);
	      if (event.getType() == EventType.NodeDataChanged) {
	    	  watchData(path);
	    	  dataChange(path);
	      }else if (event.getType() == EventType.NodeChildrenChanged) {
	    	  watchChildren(path);
	    	  childrenChange(path);
	      }else if(event.getType() == EventType.NodeDeleted){
	    	  logger.info("NodeDeleted for path:'{}'  evnet:{}",path, event);
	    	  //watchNode(path);
	    	  nodeDelete(path);
	      }else if(event.getType() == EventType.NodeCreated){
	    	  watchNode(path);
	    	  nodeCreate(path);
	      }
	    	  
	};
	
	private void dataChange(String path){
		String str = this.getData(path);
		Set<IDataListener> lis = dataListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			for(IDataListener l : lis){
				l.dataChanged(path, str);
			}
		}
	}
	
	private void nodeCreate(String path) {
		String str = this.getData(path);
		Set<INodeListener> lis = nodeListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			for(INodeListener l : lis){
				l.nodeChanged(INodeListener.ADD,path, str);
			}
		}
	}
	
	private void nodeDelete(String path) {
		//String str = this.getData(path);
		Set<INodeListener> lis = nodeListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			for(INodeListener l : lis){
				l.nodeChanged(INodeListener.REMOVE,path, null);
			}
		}
	}
	
	private void childrenChange(String path) {
		Set<String> news = this.getChildrenFromRaft(path);
		Set<String> exists = this.getChildrenFromCache(path);
		
		Set<String> adds = new HashSet<>();
		Set<String> removes = new HashSet<>();

		//计算增加的结点
		for(String n : news) {
			//在新的列表里面有，但老列表里面没有就是增加
			if(!exists.contains(n)) {
				adds.add(n);
			}
		}
		
		for(String r : exists) {
			//在老列表里面有，但是新列表里面没有，就是减少
			if(!news.contains(r)) {
				removes.add(r);
			}
		}
		
		if(!adds.isEmpty()) {
			Set<String> children = this.path2Children.get(path);
			if(children == null) {
				children = new HashSet<String>();
			}
			children.addAll(adds);
		}
		
		if(!removes.isEmpty()) {
			Set<String> children = this.path2Children.get(path);
			if(children != null) {
				children.removeAll(removes);
			}
		}
	
		
		Set<IChildrenListener> lis = childrenListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			
			if(!adds.isEmpty()) {
				for(String a : adds) {
					for(IChildrenListener l : lis){
						if(OPEN_DEBUG) {
							logger.debug("childrenChange add path:{}, children:{}",path,a);
						}
						String data = this.getData(path+"/"+a);
						l.childrenChanged(IListener.ADD,path,a,data);
					}
				}
			}
			
			if(!removes.isEmpty()) {
				for(String a : removes) {
					for(IChildrenListener l : lis){
						if(OPEN_DEBUG) {
							logger.debug("childrenChange remove path:{}, children:{}",path,a);
						}
						l.childrenChanged(IListener.REMOVE,path,a,null);
					}
				}
			}
		}
	}
	
	private void watchNode(String path){
		ExistsBuilder existsBuilder = this.curator.checkExists();
		try {
			existsBuilder.usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		} 
	}
	
	private void watchData(String path){
		GetDataBuilder getDataBuilder = this.curator.getData();
		 try {
			getDataBuilder.usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
	}
	
	private void watchChildren(String path){
		GetChildrenBuilder getChildBuilder = this.curator.getChildren();
		 try {
			 logger.debug("watchChildren: {}",path);
			 getChildBuilder.usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
	}
	
	
	@Override
	public void addNodeListener(String path, INodeListener lis) {
		if(nodeListeners.containsKey(path)){
			Set<INodeListener> l = nodeListeners.get(path);
			if(this.existsListener(l, lis)){
				return;
			}
			 if(!l.isEmpty()){
				//path已经在ZK中做监听，不需要重复加监听
		    	l.add(lis);
				return;
			  }
		}
		Set<INodeListener> set = new HashSet<INodeListener>();
		set.add(lis);
		nodeListeners.put(path, set);
		watchNode(path);
		
	}
	
	private <L> boolean existsListener(Set<L> listeners,L lis){
		for(L l : listeners){
			if(l == lis){
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void removeNodeListener(String path, INodeListener lis) {
		if(!nodeListeners.containsKey(path)){
			return;
		}
		Set<INodeListener> l = nodeListeners.get(path);
		l.remove(lis);
	}
	
	
	
	public void addListener(IConnectionStateChangeListener lis){
		connListeners.add(lis);
		if(this.curator.getState() == CuratorFrameworkState.STARTED) {
			lis.stateChanged(Constants.CONN_CONNECTED);
		}else if(this.curator.getState() == CuratorFrameworkState.STOPPED) {
			lis.stateChanged(Constants.CONN_LOST);
		}
	}
	
	public void addDataListener(String path,IDataListener lis){
		if(dataListeners.containsKey(path)){
			Set<IDataListener> l = dataListeners.get(path);
			if(this.existsListener(l, lis)){
				return;
			}
			 if(!l.isEmpty()){
		    	l.add(lis);
				return;
			  }
		}
		Set<IDataListener> l = new HashSet<IDataListener>();
		dataListeners.put(path,l);
		l.add(lis);
		watchData(path);
	
	}
	
	/**
	 * 监听孩子增加或删除
	 */
	public void addChildrenListener(String path,IChildrenListener lis){
		logger.debug("Add children listener for: {}",path);
		if(childrenListeners.containsKey(path)){
			Set<IChildrenListener> l = childrenListeners.get(path);
			if(this.existsListener(l, lis)){
				//监听器已经存在
				return;
			}
		    if(!l.isEmpty()){
		    	//列表已经存在,但监听器还不存在
		    	notifyChildrenAdd(lis,path);
		    	l.add(lis);
				return;
		    }
		}
		this.getChildrenFromRaft(path);
		//监听器和列表都不存在
		Set<IChildrenListener> l =  new HashSet<IChildrenListener>();
		childrenListeners.put(path, l);
		notifyChildrenAdd(lis,path);
		l.add(lis);
		watchChildren(path);
	}
	
	private void notifyChildrenAdd(IChildrenListener l,String path) {
		
		if(!this.path2Children.containsKey(path)) {
			return;
		}
		for(String c : this.path2Children.get(path)) {
			String data = this.getData(path+"/"+c);
			l.childrenChanged(IListener.ADD,path,c,data);
		}
	}
	
	public void removeDataListener(String path,IDataListener lis){
		if(!dataListeners.containsKey(path)){
			return;
		}
		Set<IDataListener> l = dataListeners.get(path);
		l.remove(lis);
	}
	
	public void removeChildrenListener(String path,IChildrenListener lis){
		if(!childrenListeners.containsKey(path)){
			return;
		}
		Set<IChildrenListener> l = childrenListeners.get(path);
		l.remove(lis);
	}
	
	public boolean exist(String path){
		//init();
		ExistsBuilder existsBuilder = this.curator.checkExists();
		try {
			if(this.path2Children.containsKey(path)) {
				return true;
			}
			Stat stat = existsBuilder.forPath(path);
			if(OPEN_DEBUG) {
				//logger.debug("[exist] path {}, Stat {}",path,stat);
			}
			return stat != null;
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
		return false;
	}
	
	public String getData(String path){
		//init();
		GetDataBuilder getDataBuilder = this.curator.getData();
  	    try {
			byte[] data = getDataBuilder.forPath(path);
			if(data != null) {
				return new String(data,Constants.CHARSET);
			}else {
				return "";
			}
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
  	    return "";
	}
	
	public void setData(String path,String data){
		//init();
		SetDataBuilder setDataBuilder = this.curator.setData();
  	    try {
  	    	byte[] d= data.getBytes(Constants.CHARSET);
			setDataBuilder.forPath(path,d);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
	}
	
	public Set<String> getChildrenFromCache(String path){
		if(this.path2Children.containsKey(path)) {
			Set<String> set = new HashSet<>();
			set.addAll(path2Children.get(path));
			return set;
		}else {
			return Collections.EMPTY_SET;
		}
	}
	
	public Set<String> getChildrenFromRaft(String path){
  	   try {
  		    GetChildrenBuilder getChildBuilder = this.curator.getChildren();
  		    List<String> l = getChildBuilder.forPath(path);
			Set<String> set = new HashSet<>();
			set.addAll(l);
			return set;
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
  	   return Collections.EMPTY_SET;
	}
	
	public Set<String> getChildren(String path,boolean fromCache){
	    Set<String> l = null;
	    if(fromCache) {
	    	l = this.getChildrenFromCache(path);
	    }
	    if(l == null || l.isEmpty()) {
	    	l = this.getChildrenFromRaft(path);
	    	if(l != null && !l.isEmpty()) {
	    		Set<String> set = new HashSet<>();
				set.addAll(l);
				this.path2Children.put(path, set);
	    	}
	    }
		return l;
	}
	
	/**
	 *如果结点已经存在，则直接更新数数
	 */
	public void createNode(String path,String data,boolean elp){
		if(this.exist(path)) {
			if(elp) {
				throw new CommonException("elp node ["+path+"] have been exists");
			}else {
				this.setData(path, data);
			}
			return;
		}
		String[] ps = path.trim().split("/");
		String p="";
		for(int i=1; i < ps.length-1; i++){
			p = p + "/"+ ps[i];
			if(!this.exist(p)){
				CreateBuilder createBuilder = this.curator.create();
				createBuilder.withMode(CreateMode.PERSISTENT);
		  	    try {
					createBuilder.forPath(p);
				} catch (KeeperException.NoNodeException e) {
					logger.error(e.getMessage());
				}catch(Exception e){
					logger.error("",e);
				}
			}
		}
		CreateBuilder createBuilder = this.curator.create();
  	    try {
  	    	byte[] d = data.getBytes(Constants.CHARSET);
  	    	if(elp){
  	    		createBuilder.withMode(CreateMode.EPHEMERAL);
  	    	}else {
  	    		createBuilder.withMode(CreateMode.PERSISTENT);
  	    	}
  	    	createBuilder.forPath(path,d);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
	
	}
	
	public void deleteNode(String path){
		//init();
		DeleteBuilder deleteBuilder = this.curator.delete();
  	    try {
  	    	deleteBuilder.forPath(path);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		}catch(Exception e){
			logger.error("",e);
		}
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