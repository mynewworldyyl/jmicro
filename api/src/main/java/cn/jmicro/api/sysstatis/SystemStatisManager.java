package cn.jmicro.api.sysstatis;

import java.lang.management.ManagementFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.config.Config;

@Component
public class SystemStatisManager {

	@SuppressWarnings("restriction")
	private com.sun.management.OperatingSystemMXBean osBean = null;
	
	private java.lang.management.OperatingSystemMXBean mngBean = null;
	
	@SuppressWarnings("restriction")
	public void init() {
		osBean = ManagementFactory.getPlatformMXBean(
				com.sun.management.OperatingSystemMXBean.class);
		
		mngBean = ManagementFactory.getPlatformMXBean(
				java.lang.management.OperatingSystemMXBean.class);
	}
	
	@SuppressWarnings("restriction")
	public SystemStatis getStatis() {
		SystemStatis ss = new SystemStatis();
		ss.setCpuLoad(osBean.getSystemCpuLoad());
		ss.setFreeMemory(osBean.getFreePhysicalMemorySize());
		ss.setTotalMemory(osBean.getTotalPhysicalMemorySize());
		
		ss.setAvgCpuLoad(osBean.getSystemCpuLoad());
		ss.setCpuNum(mngBean.getAvailableProcessors());
		ss.setInsName(Config.getInstanceName());
		ss.setSysName(System.getProperty("os.name"));
		
		return ss;
	}
	
}
