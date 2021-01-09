package cn.jmicro.api.raft;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;

/**
 * 监听特定目录其直接子节点数据变化并通知注册的监听器
 * 
 * @author yeyulei
 *
 */
public class RaftNodeDataListener<NodeType> {

	private final static Logger logger = LoggerFactory.getLogger(RaftNodeDataListener.class);
	
	private String dir;
	
	private IDataOperator op;
	
	private Set<IRaftListener<NodeType>> rl = new HashSet<>();
	
	private Map<String,NodeType> datas = null;
	
	private boolean maintainDataList = false;
	
	private Class<NodeType> nodeClazz;
	
	private Object syncLock = new Object();
	
	private IDataListener dataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			String node = path.substring(dir.length()+1);
			updateItemData(node,data);
		}
	};
	
	public RaftNodeDataListener(IDataOperator op,String dir,Class<NodeType> clazz,boolean maintainDataList) {
		if(Utils.isEmpty(dir)) {
			throw new CommonException("Raft data dir cannot be NULL");
		}
		if(op == null) {
			throw new CommonException("Raft data operator cannot be NULL");
		}
		
		if(clazz == null) {
			throw new CommonException("Node class cannot be NULL");
		}
		
		if(maintainDataList) {
			this.datas = new ConcurrentHashMap<>();
		}
		
		if(!op.exist(dir)) {
			op.createNodeOrSetData(dir, "", IDataOperator.PERSISTENT);
		}
		
		this.dir = dir;
		this.op = op;
		this.maintainDataList = maintainDataList;
		this.nodeClazz = clazz;
		
		op.addChildrenListener(this.dir, new IChildrenListener() {
			@Override
			public void childrenChanged(int type,String parent, String child,String data) {
				if(IListener.ADD == type) {
					nodeAdd(child,data);
				}else if(IListener.REMOVE == type) {
					nodeRemove(child);
				}
			}
		});
	}

	protected void nodeRemove(String node) {
		op.removeDataListener(this.dir + "/" + node, this.dataListener);
		if(this.maintainDataList) {
			notify(IListener.REMOVE,node,this.datas.get(node));
			synchronized(syncLock) {
				this.datas.remove(node);
			}
		} else {
			notify(IListener.REMOVE,node,null);
		}
	}

	protected void nodeAdd(String node, String data) {
		NodeType n = JsonUtils.getIns().fromJson(data, this.nodeClazz);
		op.addDataListener(this.dir + "/" + node, this.dataListener);
		if(this.datas != null) {
			synchronized(syncLock) {
				this.datas.put(node, n);
			}
		}
		notify(IListener.ADD,node,n);
	}
	
	private void updateItemData(String node, String data) {
		NodeType n = JsonUtils.getIns().fromJson(data, this.nodeClazz);
		if(this.datas != null) {
			synchronized(syncLock) {
				datas.put(node, n);
			}
		}
		notify(IListener.DATA_CHANGE,node,n);
	}
	
	private void notify(int type,String node,NodeType data) {
		if(rl.isEmpty()) {
			return;
		}
		
		for(IRaftListener<NodeType> l : rl) {
			l.onEvent(type, node, data);
		}
	}
	
	public void addListener(IRaftListener<NodeType> lis) {
		if(rl.contains(lis)) {
			throw new CommonException("Listener exist!");
		}else {
			if(this.datas != null) {
				if(!this.datas.isEmpty()) {
					synchronized(syncLock) {
						for(String ke : this.datas.keySet()) {
							lis.onEvent(IListener.ADD,ke , this.datas.get(ke));
						}
					}
				}
			} else if(op.exist(this.dir)) {
				rl.add(lis);
				Set<String> children = this.op.getChildren(this.dir, false);
				if(children != null && !children.isEmpty()) {
					for(String ke : children) {
						String path = this.dir + "/" + ke;
						String ndata = op.getData(path);
						NodeType n = JsonUtils.getIns().fromJson(ndata, this.nodeClazz);
						if(n != null) {
							lis.onEvent(IListener.ADD,ke , n);
						}
					}
				}
			}
		}
	}
	
	public void removeListener(IRaftListener<NodeType> lis) {
		if(rl.contains(lis)) {
			rl.remove(lis);
		}
	}

	public String getDir() {
		return dir;
	}
	
	public NodeType getData(String node) {
		if(this.datas != null) {
			return this.datas.get(node);
		} else {
			String path = ChoyConstants.INS_ROOT+"/"+node;
			String ndata = op.getData(path);
			if(!Utils.isEmpty(ndata)) {
				return JsonUtils.getIns().fromJson(ndata, this.nodeClazz);
			}
		}
		return null;
	}
	
	public void forEachNodeName(Consumer<String> c) {
		if(this.datas != null) {
			if(this.datas.isEmpty()) {
				return;
			}
			Set<String> keys = new HashSet<>();
			
			synchronized(syncLock) {
				keys.addAll(this.datas.keySet());
			}
			for(String k : keys) {
				c.accept(k);
			}
			
		} else {
			Set<String> children = this.op.getChildren(this.dir, false);
			if(children != null && !children.isEmpty()) {
				for(String ke : children) {
					c.accept(ke);
				}
			}
		}
		
	}
	
	public void forEachNode(Consumer<NodeType> c) {
		if(this.datas != null) {
			if(this.datas.isEmpty()) {
				return;
			}
			Set<NodeType> keys = new HashSet<>();
			synchronized(syncLock) {
				keys.addAll(this.datas.values());
			}
			for(NodeType k : keys) {
				c.accept(k);
			}
		} else {
			Set<String> children = this.op.getChildren(this.dir, false);
			if(children != null && !children.isEmpty()) {
				for(String ke : children) {
					String path = this.dir + "/" + ke;
					String ndata = op.getData(path);
					NodeType n = JsonUtils.getIns().fromJson(ndata, this.nodeClazz);
					if(n != null) {
						c.accept(n);
					}
				}
			}
		}
		
	}
}
