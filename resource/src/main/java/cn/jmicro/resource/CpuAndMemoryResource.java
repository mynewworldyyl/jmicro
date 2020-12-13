package cn.jmicro.resource;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.monitor.AbstractResource;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.ResourceData;

@Component
public class CpuAndMemoryResource extends AbstractResource implements IResource {
	
	private static final String RES_NAME = "cpuAndMemory";
	private static final String SYSTEM_CPU_LOAD = "systemCpuLoad";
	private static final String FREE_MEMORY_SIZE = "freePhysicalMemorySize";
	private static final String TOTAL_MEMORY_SIZE = "totalPhysicalMemorySize";
	private static final String SYSTEM_LOAD_AVERAGE = "systemLoadAverage";
	private static final String AVAILABLE_CPUS = "availableCpus";
	private static final String OS_NAME = "osName";
	
	@SuppressWarnings("restriction")
	private com.sun.management.OperatingSystemMXBean osBean = null;
	private java.lang.management.OperatingSystemMXBean mngBean = null;
	
	@Override
	@SuppressWarnings("restriction")
	public ResourceData getResource(Map<String,Object> params) {
		this.data.putData(SYSTEM_CPU_LOAD, osBean.getSystemCpuLoad());
		this.data.putData(FREE_MEMORY_SIZE, osBean.getFreePhysicalMemorySize());
		this.data.putData(TOTAL_MEMORY_SIZE, osBean.getTotalPhysicalMemorySize());
		this.data.putData(SYSTEM_LOAD_AVERAGE, osBean.getSystemLoadAverage());
		this.data.putData(AVAILABLE_CPUS, osBean.getAvailableProcessors());
		this.data.putData(OS_NAME, System.getProperty("os.name"));
		return this.data;
	}

	@SuppressWarnings("restriction")
	@Override
	public void ready() {
		super.ready();
		
		osBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
		mngBean = ManagementFactory.getPlatformMXBean(java.lang.management.OperatingSystemMXBean.class);
		
		this.data.setResName(RES_NAME);
		Set<CfgMetadata> metadatas = pi.getMetadatas();
		
		//根目录
		CfgMetadata md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(SYSTEM_CPU_LOAD);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.Float.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(FREE_MEMORY_SIZE);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.Float.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(TOTAL_MEMORY_SIZE);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.Float.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(SYSTEM_LOAD_AVERAGE);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.Float.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(AVAILABLE_CPUS);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.Integer.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(OS_NAME);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.String.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
	}
	
}
