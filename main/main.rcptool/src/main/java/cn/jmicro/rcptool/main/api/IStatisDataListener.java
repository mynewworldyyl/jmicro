package cn.jmicro.rcptool.main.api;

import java.util.Map;

import cn.jmicro.api.registry.ServiceMethodJRso;

public interface IStatisDataListener {
	void onData(ServiceMethodJRso sm,Map<Integer,Double> data);
}
