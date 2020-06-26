package cn.jmicro.api.monitor;

import java.util.Set;

import cn.jmicro.api.IListener;

public interface INamedTypeListener extends IListener {

	void namedTypeChange(int type,String name,Set<Short> types);
	
}
