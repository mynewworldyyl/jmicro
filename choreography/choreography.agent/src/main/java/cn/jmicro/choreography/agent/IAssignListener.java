package cn.jmicro.choreography.agent;

import cn.jmicro.choreography.assign.Assign;

public interface IAssignListener {

	void change(int type,Assign as);
}
