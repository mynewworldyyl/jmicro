package cn.jmicro.api.monitor.v2;

import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.registry.UniqueServiceKey;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	protected String skey(String namespace,String version) {
		return UniqueServiceKey.serviceName(IMonitorDataSubscriber.class.getName(), namespace,
				version).toString();
	}

	@Override
	public ReportData  getData(String srvKey, Short[] type, String[] dataType) {
		return null;
	}

}
