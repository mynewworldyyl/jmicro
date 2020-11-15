package cn.jmicro.api.raft;

import cn.jmicro.api.IListener;

public interface IRaftListener<NodeType> extends IListener{

	void onEvent(int type,String node,NodeType data);
}
