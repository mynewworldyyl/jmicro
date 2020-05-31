package cn.jmicro.choreography.api;

import cn.jmicro.api.IListener;
import cn.jmicro.api.choreography.ProcessInfo;

public interface IInstanceListener extends IListener{

	void instance(int type, ProcessInfo pi);
	
}
