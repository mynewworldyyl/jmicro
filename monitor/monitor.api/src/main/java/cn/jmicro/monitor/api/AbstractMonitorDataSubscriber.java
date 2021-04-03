package cn.jmicro.monitor.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.JMStatisItem;
import cn.jmicro.api.monitor.MonitorAndService2TypeRelationshipManager;
import cn.jmicro.api.monitor.StatisItem;
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
		
		String path = Config.getRaftBasePath(Config.MonitorTypesDir) + "/" + srvKey;
		
		if(op.exist(path)) {
			return;
		}

		if(typess == null || typess.length == 0) {
			logger.error(srvKey+" types is NULL");
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for(Short s : typess) {
			sb.append(s).append(MonitorAndService2TypeRelationshipManager.TYPE_SPERATOR);
		}
		sb.delete(sb.length()-1, sb.length());
		
		op.createNodeOrSetData(path, sb.toString(), IDataOperator.PERSISTENT);
		
	}
	
	protected String toLog(JMStatisItem si,StatisItem oi) {
		StringBuilder sb = new StringBuilder();
		sb.append("INS [").append(si.getInstanceName())
		.append(" M[").append(si.getKey()).append("]");
		//sb.append(",Cnt[").append(oi.getCnt()).append("]")
		sb.append(",Val[").append(oi.getVal()).append("]")
		.append(",Type[").append(oi.getType()).append("]");
		return sb.toString();
	}

	protected void log(JMStatisItem si) {
		for(Short type : si.getTypeStatis().keySet()) {
			//List<StatisItem> items = si.getTypeStatis().get(type);
			StringBuffer sb = new StringBuffer();
			sb.append("GOT: " + MC.MONITOR_VAL_2_KEY.get(type));
			sb.append(", SM: ").append(si.getKey());
			sb.append(", actName: ").append(si.getClientId());
			logger.debug(sb.toString()); 
		}
	}
	
}
