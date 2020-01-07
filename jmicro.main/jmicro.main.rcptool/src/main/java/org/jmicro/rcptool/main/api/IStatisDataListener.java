package org.jmicro.rcptool.main.api;

import java.util.Map;

import org.jmicro.api.registry.ServiceMethod;

public interface IStatisDataListener {
	void onData(ServiceMethod sm,Map<Integer,Double> data);
}
