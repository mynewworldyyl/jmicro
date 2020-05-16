package org.jmicro.zk.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.jmicro.api.raft.IDataListener;
import org.jmicro.zk.ZKDataOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataManager {

	private final static Logger logger = LoggerFactory.getLogger(DataManager.class);
	
	private boolean openDebug = true;
	//数据改变监听器
	private  Map<String,Set<IDataListener>> dataListeners = new HashMap<>();
	
	private Object syncLocker = new Object();
	
	private CuratorFramework curator = null;
	
	private ZKDataOperator op;
	
	public DataManager(ZKDataOperator op,CuratorFramework c,boolean openDebug) {
		this.op = op;
		this.curator = c;
		this.openDebug = openDebug;
	}
	
	private final Watcher watcher =(WatchedEvent event)->{
		   String path = event.getPath();
	      //logger.info("Watcher for '{}' received watched event: {}",path, event);
	      if (event.getType() == EventType.NodeDataChanged) {
	    	  watchData(path);
	    	  synchronized(syncLocker) {
	    		  dataChange(path);
	    	  }
	      } 
	};
	
	private void dataChange(String path){
		String str = op.getData(path);
		Set<IDataListener> lis = dataListeners.get(path);
		if(lis != null && !lis.isEmpty()){
			for(IDataListener l : lis){
				l.dataChanged(path, str);
			}
		}
	}
	
	private void watchData(String path){
		GetDataBuilder getDataBuilder = this.curator.getData();
		 try {
			getDataBuilder.usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException e) {
			logger.error("watchData1: " +e.getMessage());
		}catch(Exception e){
			logger.error("watchData2: ",e);
		}
	}
	
	public void addDataListener(String path,IDataListener lis){
		if(dataListeners.containsKey(path)){
			Set<IDataListener> l = dataListeners.get(path);
			if(op.existsListener(l, lis)){
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
	
	public void removeDataListener(String path,IDataListener lis){
		if(!dataListeners.containsKey(path)){
			return;
		}
		Set<IDataListener> l = dataListeners.get(path);
		l.remove(lis);
	}
}
