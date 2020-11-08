package cn.jmicro.api.monitor;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.config.Config;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

public class MonitorClientStatusAdapter implements IMonitorAdapter {

	private Logger logger = null;
	
	public boolean monitoralbe = false;

	private long lastStatusTime = 0;

	private ServiceCounter sc = null;

	private String group = null;

	public final Short[] TYPES;

	public String[] typeLabels = null;

	private String key;

	public MonitorClientStatusAdapter(Short[] ts, String[] labels, String key, String group) {
		if (ts == null || ts.length == 0) {
			throw new CommonException("Monitor status type cannot be null");
		}
		if (StringUtils.isEmpty(key)) {
			throw new CommonException("Monitor status checker KEY cannot be NULL");
		}
		
		logger = LoggerFactory.getLogger(key);

		if (StringUtils.isEmpty(group)) {
			this.group = "defalutMonitor";
		} else {
			this.group = group;
		}
		
		monitoralbe = false;
		TYPES = ts;
		this.typeLabels = labels;
		this.key = key;
		this.init0();
		sc = new ServiceCounter(this.key, TYPES, 60 * 3L, 1L, TimeUnit.SECONDS);
	}

	private void init0() {
		typeLabels = new String[TYPES.length];
		for (int i = 0; i < TYPES.length; i++) {
			typeLabels[i] = MC.MONITOR_VAL_2_KEY.get(TYPES[i]);
		}
	}

	public ServiceCounter getServiceCounter() {
		return sc;
	}

	public void checkTimeout() {
		if (monitoralbe && (TimeUtils.getCurTime() - lastStatusTime > 300000)) {// 5分钏没有状态请求
			logger.warn("ServiceCounter timeout 5 minutes, and stop it");
			this.enableMonitor(false);
		}
	}

	@Override
	public MonitorServerStatus status() {
		if (!monitoralbe) {
			enableMonitor(true);
		}

		lastStatusTime = TimeUtils.getCurTime();

		MonitorServerStatus s = new MonitorServerStatus();
		// s.setInstanceName(Config.getInstanceName());
		// s.setSubsriberSize(regSubs.size());
		// s.getSubsriber2Types().putAll(this.monitorManager.getMkey2Types());
		// s.setSendCacheSize(sentItems.size());

		double[] qpsArr = new double[TYPES.length];
		double[] curArr = new double[TYPES.length];
		double[] totalArr = new double[TYPES.length];

		s.setCur(curArr);
		s.setQps(qpsArr);
		s.setTotal(totalArr);

		for (int i = 0; i < TYPES.length; i++) {
			Short t = TYPES[i];

			totalArr[i] = sc.getTotal(t);
			curArr[i] = new Double(sc.get(t));
			if (t == MC.Ms_CheckLoopCnt) {
				// System.out.println("");
			}
			qpsArr[i] = sc.getQps(TimeUnit.SECONDS, t);

		}

		return s;
	}

	@Override
	public void enableMonitor(boolean enable) {
		if (enable && !monitoralbe) {
			sc.start();
			monitoralbe = true;
		} else if (!enable && monitoralbe) {
			sc.stop();
			monitoralbe = false;
		}
	}

	@Override
	public MonitorInfo info() {
		MonitorInfo info = new MonitorInfo();
		info.setGroup(this.group);
		info.setTypeLabels(typeLabels);
		info.setTypes(TYPES);
		info.setRunning(monitoralbe);
		info.setInstanceName(Config.getInstanceName());
		// info.getSubsriber2Types().putAll(monitorManager.getMkey2Types());;
		return info;
	}

	public boolean isMonitoralbe() {
		return monitoralbe;
	}

	@Override
	public void reset() {
	}

}
