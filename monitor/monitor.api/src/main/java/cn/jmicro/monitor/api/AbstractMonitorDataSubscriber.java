package cn.jmicro.monitor.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.MonitorClient;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.registry.UniqueServiceKey;
import cn.jmicro.common.util.StringUtils;

public abstract class AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(AbstractMonitorDataSubscriber.class);
	
	protected String skey(String namespace,String version) {
		return UniqueServiceKey.serviceName(IMonitorDataSubscriber.class.getName(), namespace,
				version).toString();
	}

	@Override
	public ReportData  getData(String srvKey, Short[] type, String[] dataType) {
		return null;
	}

	protected void registType(IDataOperator op, String srvKey, Short[] typess) {

		if(typess == null || typess.length == 0) {
			logger.error(srvKey+" types is NULL");
			return;
		}
		StringBuilder sb = new StringBuilder();
		for(Short s : typess) {
			sb.append(s).append(MonitorClient.TYPE_SPERATOR);
		}
		sb.delete(sb.length()-1, sb.length());
		
		String path = Config.MonitorTypesDir + "/" + srvKey;
		

		if(op.exist(path)) {
			String ts = op.getData(path);
			if(StringUtils.isEmpty(ts)) {
				ts = sb.toString();
			} else {
				String[] tsArr = ts.split(MonitorClient.TYPE_SPERATOR);
				for(short sv : typess) {
					boolean f = false;
					for(String v : tsArr) {
						short ssv = Short.parseShort(v);
						if(ssv == sv) {
							f = true;
							break;
						}
					}
					
					if(!f) {
						ts = ts + MonitorClient.TYPE_SPERATOR + sv;
					}
				}
			}
			op.setData(path, ts);
		} else {
			op.createNodeOrSetData(path, sb.toString(), IDataOperator.PERSISTENT);
		}
		
	}
	
}
