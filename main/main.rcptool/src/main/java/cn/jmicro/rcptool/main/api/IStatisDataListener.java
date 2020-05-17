package cn.jmicro.rcptool.main.api;

import java.util.Map;

import cn.jmicro.api.registry.ServiceMethod;

public interface IStatisDataListener {
	void onData(ServiceMethod sm,Map<Integer,Double> data);
}
