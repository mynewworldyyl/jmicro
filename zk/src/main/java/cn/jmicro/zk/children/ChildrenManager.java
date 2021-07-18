package cn.jmicro.zk.children;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.zk.ZKDataOperator;

public class ChildrenManager {

	private final static Logger logger = LoggerFactory.getLogger(ChildrenManager.class);

	private boolean openDebug = true;
	
	private Object syncLocker = new Object();

	// 路径到子结点之间关系，Key是全路径，值只包括结点名称
	private Map<String, Set<String>> path2Children = new ConcurrentHashMap<>();

	// 子结点监听器
	private Map<String, Set<IChildrenListener>> childrenListeners = new HashMap<>();

	private CuratorFramework curator = null;

	private ZKDataOperator op;

	public ChildrenManager(ZKDataOperator op, CuratorFramework c, boolean openDebug) {
		this.op = op;
		this.curator = c;
		this.openDebug = openDebug;
	}
	
	public void connStateChange(int type) {
		if(childrenListeners.isEmpty()) {
			return;
		}
		if(Constants.CONN_RECONNECTED == type) {
			Set<String> ls = new HashSet<>();
			ls.addAll(this.childrenListeners.keySet());
			
			for(String path : ls) {
				watchChildren(path);
			}
		}
		
	}

	/**
	 * 监听孩子增加或删除
	 */
	public void addChildrenListener(String path, IChildrenListener lis) {
		if(this.openDebug)
			logger.debug("Add children listener for: {}", path);
		
		if (childrenListeners.containsKey(path)) {
			//指定路经的监听器已经存在了
			Set<IChildrenListener> l = childrenListeners.get(path);
			if (op.existsListener(l, lis)) {
				//监听器已经存在,同时说明已经做过通知
				return;
			}
			if (!l.isEmpty()) {
				//监听器还不存在
				l.add(lis);
				// 列表已经存在,但监听器还不存在，通知已经存在的子结点
				notifyChildrenAdd(lis, path);
				return;
			}
		}
		Set<String> set = this.getChildrenFromRaft(path);
		this.path2Children.put(path, set);
		// 监听器和列表都不存在
		Set<IChildrenListener> l = new HashSet<IChildrenListener>();
		l.add(lis);
		childrenListeners.put(path, l);
		watchChildren(path);
		
		notifyChildrenAdd(lis, path);
		
	}

	public void removeChildrenListener(String path, IChildrenListener lis) {
		if (!childrenListeners.containsKey(path)) {
			return;
		}
		Set<IChildrenListener> l = childrenListeners.get(path);
		l.remove(lis);
	}

	public Set<String> getChildren(String path, boolean fromCache) {
		Set<String> l = null;
		if (fromCache) {
			l = this.getChildrenFromCache(path);
		}
		if (l == null || l.isEmpty()) {
			l = this.getChildrenFromRaft(path);
			if (l != null && !l.isEmpty()) {
				Set<String> set = new HashSet<>();
				set.addAll(l);
				this.path2Children.put(path, set);
			}
		}
		return l;
	}

	private final Watcher watcher = (WatchedEvent event) -> {
		String path = event.getPath();
		if (event.getType() == EventType.NodeChildrenChanged) {
			watchChildren(path);
			synchronized(syncLocker) {
				childrenChange(path);
			}
		}
	};

	private void childrenChange(String path) {
		Set<String> news = this.getChildrenFromRaft(path);
		Set<String> exists = this.getChildrenFromCache(path);

		Set<String> adds = new HashSet<>();
		Set<String> removes = new HashSet<>();

		//计算增加的结点
		for (String n : news) {
			//logger.debug("Add: "+n);
			// 在新的列表里面有，但老列表里面没有就是增加
			if (!exists.contains(n)) {
				adds.add(n);
			}
		}

		for (String r : exists) {
			// 在老列表里面有，但是新列表里面没有，就是减少
			if (!news.contains(r)) {
				removes.add(r);
			}
		}

		Set<String> children = this.path2Children.get(path);
		if (children == null) {
			children = new HashSet<String>();
			path2Children.put(path, children);
		}
		
		if (!adds.isEmpty()) {
			children.addAll(adds);
		}

		if (!removes.isEmpty()) {
			children.removeAll(removes);
		}

		 Set<IChildrenListener> lis = new HashSet<>();
		 lis.addAll(childrenListeners.get(path));
		 
		if (lis != null && !lis.isEmpty()) {

			if (!adds.isEmpty()) {
				for (String a : adds) {
					for (IChildrenListener l : lis) {
						if (openDebug) {
							logger.debug("childrenChange add path:{}, children:{}", path, a);
						}
						//String data = op.getData(path + "/" + a);
						l.childrenChanged(IListener.ADD, path, a/*, data*/);
					}
				}
			}

			if (!removes.isEmpty()) {
				for (String a : removes) {
					for (IChildrenListener l : lis) {
						if (openDebug) {
							logger.debug("childrenChange remove path:{}, children:{}", path, a);
						}
						l.childrenChanged(IListener.REMOVE, path, a/*, null*/);
					}
				}
			}
		}
	}

	private void watchChildren(String path) {
		GetChildrenBuilder getChildBuilder = this.curator.getChildren();
		try {
			if(this.openDebug)
				logger.debug("watchChildren: {}", path);
			getChildBuilder.usingWatcher(watcher).forPath(path);
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	//第一次增加监听器时，需要对每个已经存在的结点调用监听方法，通知结点增加
	private void notifyChildrenAdd(IChildrenListener l, String path) {

		if (!this.path2Children.containsKey(path)) {
			//当前路径下还没有子结点
			return;
		}
		
		synchronized(syncLocker) {
			 Set<String> set = new HashSet<>();
			 set.addAll(this.path2Children.get(path));
			 for(String c : set) {
				//String data = op.getData(path + "/" + c);
				l.childrenChanged(IListener.ADD, path, c/*, data*/);
			 }
		}
	}

	public Set<String> getChildrenFromCache(String path) {
		if (this.path2Children.containsKey(path)) {
			Set<String> set = new HashSet<>();
			set.addAll(path2Children.get(path));
			return set;
		} else {
			return Collections.EMPTY_SET;
		}
	}

	public Set<String> getChildrenFromRaft(String path) {
		try {
			GetChildrenBuilder getChildBuilder = this.curator.getChildren();
			List<String> l = getChildBuilder.forPath(path);
			Set<String> set = new HashSet<>();
			set.addAll(l);
			return set;
		} catch (KeeperException.NoNodeException e) {
			logger.error(e.getMessage());
		} catch (Exception e) {
			logger.error("", e);
		}
		return Collections.EMPTY_SET;
	}
	
	public void removeCache(String path) {
		if(StringUtils.isEmpty(path)) {
			return;
		}
		String subNodeName = null;
		String parentPath = null;
		int idx = path.lastIndexOf("/");
		if(idx > 0) {
			subNodeName = path.substring(idx+1, path.length());
			parentPath = path.substring(0, idx);
		}
		if(subNodeName != null && this.path2Children.containsKey(parentPath)) {
			this.path2Children.get(parentPath).remove(subNodeName);
		}
	}

}
