package cn.jmicro.example.logmonitor;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.ILogWarning;
import cn.jmicro.api.monitor.MRpcLogItem;

@Service(namespace="logmonitor",version="0.0.1")
@Component
public class TestLogSubscriber implements ILogWarning {
	
	@Override
	public void warn(MRpcLogItem log) {
		System.out.println(log.toString());
	}
	
}
