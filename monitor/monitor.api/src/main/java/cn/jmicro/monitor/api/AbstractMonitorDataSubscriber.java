package cn.jmicro.monitor.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MonitorTypeManager;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKey;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(AbstractMonitorDataSubscriber.class);
	
	protected String skey(String namespace,String version) {
		return UniqueServiceKey.serviceName(IMonitorDataSubscriber.class.getName(), namespace,version);
	}

	@Override
	public ReportData  getData(String srvKey, Short[] type, String[] dataType) {
		return null;
	}

	protected void registType(IDataOperator op, String srvKey, Short[] typess) {
		
		String path = Config.MonitorTypesDir + "/" + srvKey;
		
		if(op.exist(path)) {
			return;
		}

		if(typess == null || typess.length == 0) {
			logger.error(srvKey+" types is NULL");
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for(Short s : typess) {
			sb.append(s).append(MonitorTypeManager.TYPE_SPERATOR);
		}
		sb.delete(sb.length()-1, sb.length());
		
		op.createNodeOrSetData(path, sb.toString(), IDataOperator.PERSISTENT);
		
	}
	
}
