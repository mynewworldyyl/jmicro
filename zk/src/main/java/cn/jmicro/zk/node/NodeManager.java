package cn.jmicro.zk.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.raft.INodeListener;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.zk.ZKDataOperator;

public class NodeManager {

	private final static Logger logger = LoggerFactory.getLogger(NodeManager.class);
	
	private boolean openDebug = true;
	
	/**
	 * 结点增加或删除监听器
	 * 结点创建和结点删除，只针对指定的path做监听
	 */
	private  Map<String,Set<INodeListener>> nodeListeners = new HashMap<>();
	
	private CuratorFramework curator = null;
	
	private ZKDataOperator op;
	
	public NodeManager(ZKDataOperator op,CuratorFramework c,boolean openDebug) {
		this.op = op;
		this.curator = c;
		this.openDebug = openDebug;
	}
	
	private final Watcher watcher =(WatchedEvent event)->{
		   String path = event.getPath();
		   watchNode(path);
	      if(event.getType() == EventType.NodeCreated){
	    	  //watchNode(path);
	    	  nodeCreate(path);
	      } else if(event.getType() == EventType.NodeDeleted){
	    	  logger.info("NodeDeleted for path:'{}'  evnet:{}",path, event);
	    	  //watchNode(path);
	    	  nodeDelete(path);
	      }else if(event.getType() == EventType.NodeDataChanged) {
	    	  nodeDataChange(path);
	      }
	};
	
	private void nodeCreate(String path) {
		Set<INodeListener> lis = nodeListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			String str = op.getData(path);
			for(INodeListener l : lis){
				l.nodeChanged(INodeListener.ADD,path, str);
			}
		}
	}
	
	private void nodeDataChange(String path) {
		Set<INodeListener> lis = nodeListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			String str = op.getData(path);
			for(INodeListener l : lis){
				l.nodeChanged(INodeListener.DATA_CHANGE,path, str);
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
	
	public void addNodeListener(String path, INodeListener lis) {
		if(nodeListeners.containsKey(path)){
			Set<INodeListener> l = nodeListeners.get(path);
			if(op.existsListener(l, lis)){
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
	
	public void removeNodeListener(String path, INodeListener lis) {
		if(!nodeListeners.containsKey(path)){
			return;
		}
		Set<INodeListener> l = nodeListeners.get(path);
		l.remove(lis);
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
	
	public boolean exist(String path){
		//init();
		ExistsBuilder existsBuilder = this.curator.checkExists();
		try {
			Stat stat = existsBuilder.forPath(path);
			if(openDebug && stat == null) {
				logger.debug("Path not found: {}",path);
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
	
}
