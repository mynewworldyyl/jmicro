package cn.jmicro.resource;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadataJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.monitor.AbstractResource;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.ResourceDataJRso;
import cn.jmicro.common.util.StringUtils;

@Component
public class CpuResource extends AbstractResource implements IResource {
	
	private static final String RES_NAME = "cpu";
	private static final String SYSTEM_CPU_LOAD = "systemCpuLoad";
	private static final String SYSTEM_LOAD_AVERAGE = "systemLoadAverage";
	private static final String AVAILABLE_CPUS = "availableCpus";
	private static final String OS_NAME = "osName";
	
	@SuppressWarnings("restriction")
	private com.sun.management.OperatingSystemMXBean osBean = null;
	private java.lang.management.OperatingSystemMXBean mngBean = null;
	
	@Override
	@SuppressWarnings("restriction")
	public ResourceDataJRso getResource(Map<String,Object> query,String expStr) {
		ResourceDataJRso data = this.getData();
		data.putData(SYSTEM_CPU_LOAD, osBean.getSystemCpuLoad());
		data.putData(SYSTEM_LOAD_AVERAGE, osBean.getSystemLoadAverage());
		data.putData(AVAILABLE_CPUS, osBean.getAvailableProcessors());
		
		if(StringUtils.isNotEmpty(expStr) && !this.compuleByExp(expStr, data.getMetaData())) {
			return null;
		}
		return data;
	}

	@SuppressWarnings("restriction")
	public void jready() {
		super.jready0();
		
		osBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
		mngBean = ManagementFactory.getPlatformMXBean(java.lang.management.OperatingSystemMXBean.class);
		
		Set<CfgMetadataJRso> metadatas = new HashSet<>();
		pi.setMetadatas(RES_NAME, metadatas);
		
		CfgMetadataJRso md = new CfgMetadataJRso();
		md.setResName(RES_NAME);
		md.setName(SYSTEM_CPU_LOAD);
		md.setDataType(CfgMetadataJRso.DataType.Float.getCode());
		md.setDesc("CPU Load");
		metadatas.add(md);
		
		md = new CfgMetadataJRso();
		md.setResName(RES_NAME);
		md.setName(SYSTEM_LOAD_AVERAGE);
		md.setDataType(CfgMetadataJRso.DataType.Float.getCode());
		md.setDesc("System load average");
		metadatas.add(md);
		
		md = new CfgMetadataJRso();
		md.setResName(RES_NAME);
		md.setName(AVAILABLE_CPUS);
		md.setDataType(CfgMetadataJRso.DataType.Integer.getCode());
		md.setDesc("CPU Num");
		metadatas.add(md);
		
	}

	@Override
	public String getResourceName() {
		return RES_NAME;
	}
	
}
