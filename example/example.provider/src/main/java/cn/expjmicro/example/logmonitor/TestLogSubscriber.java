package cn.expjmicro.example.logmonitor;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.ILogWarning;
import cn.jmicro.api.monitor.JMLogItem;
import cn.jmicro.common.Constants;

@Service(version="0.0.1",clientId=Constants.NO_CLIENT_ID)
@Component
public class TestLogSubscriber implements ILogWarning {
	
	@Override
	public void warn(JMLogItem log) {
		System.out.println(log.toString());
	}
	
}

