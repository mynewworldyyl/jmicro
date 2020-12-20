package cn.jmicro.resource;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.monitor.AbstractResource;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.ResourceData;
import cn.jmicro.api.utils.TimeUtils;
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
	public ResourceData getResource(Map<String,Object> query,String expStr) {
		ResourceData data = this.getData();
		data.putData(SYSTEM_CPU_LOAD, osBean.getSystemCpuLoad());
		data.putData(SYSTEM_LOAD_AVERAGE, osBean.getSystemLoadAverage());
		data.putData(AVAILABLE_CPUS, osBean.getAvailableProcessors());
		data.putData(OS_NAME, System.getProperty("os.name"));
		
		if(StringUtils.isNotEmpty(expStr) && !this.compuleByExp(expStr, data.getMetaData())) {
			return null;
		}
		return data;
	}

	@SuppressWarnings("restriction")
	@Override
	public void ready() {
		super.ready();
		
		osBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
		mngBean = ManagementFactory.getPlatformMXBean(java.lang.management.OperatingSystemMXBean.class);
		
		Set<CfgMetadata> metadatas = new HashSet<>();
		pi.setMetadatas(RES_NAME, metadatas);
		
		CfgMetadata md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(SYSTEM_CPU_LOAD);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("CPU Load");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(SYSTEM_LOAD_AVERAGE);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("System load average");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(AVAILABLE_CPUS);
		md.setDataType(CfgMetadata.DataType.Integer.getCode());
		md.setDesc("CPU个数");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(OS_NAME);
		md.setDataType(CfgMetadata.DataType.String.getCode());
		md.setDesc("操作系统名称");
		metadatas.add(md);
		
	}

	@Override
	public String getResourceName() {
		return RES_NAME;
	}
	
}
