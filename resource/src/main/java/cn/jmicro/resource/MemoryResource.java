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
public class MemoryResource extends AbstractResource  implements IResource {
	
	private static final String RES_NAME = "memory";
	private static final String FREE_MEMORY_SIZE = "freePhysicalMemorySize";
	private static final String TOTAL_MEMORY_SIZE = "totalPhysicalMemorySize";
	private static final String OS_NAME = "osName";
	
	@SuppressWarnings("restriction")
	private com.sun.management.OperatingSystemMXBean osBean = null;
	private java.lang.management.OperatingSystemMXBean mngBean = null;
	
	@Override
	@SuppressWarnings("restriction")
	public ResourceDataJRso getResource(Map<String,Object> query,String expStr) {
		ResourceDataJRso data = this.getData();
		data.putData(FREE_MEMORY_SIZE, osBean.getFreePhysicalMemorySize());
		data.putData(TOTAL_MEMORY_SIZE, osBean.getTotalPhysicalMemorySize());
		if(StringUtils.isNotEmpty(expStr) && !this.compuleByExp(expStr, data.getMetaData())) {
			return null;
		}
		return data;
	}

	@SuppressWarnings("restriction")
	public void ready() {
		super.ready0();
		
		osBean = ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
		mngBean = ManagementFactory.getPlatformMXBean(java.lang.management.OperatingSystemMXBean.class);
		
		Set<CfgMetadataJRso> metadatas = new HashSet<>();
		pi.setMetadatas(RES_NAME, metadatas);
		
		CfgMetadataJRso md = new CfgMetadataJRso();
		md.setResName(RES_NAME);
		md.setName(FREE_MEMORY_SIZE);
		md.setDataType(CfgMetadataJRso.DataType.Float.getCode());
		md.setDesc("剩余的物理内存");
		metadatas.add(md);
		
		md = new CfgMetadataJRso();
		md.setResName(RES_NAME);
		md.setName(TOTAL_MEMORY_SIZE);
		md.setDataType(CfgMetadataJRso.DataType.Float.getCode());
		md.setDesc("总的物理内存");
		metadatas.add(md);
		
		/*md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(OS_NAME);
		md.setDataType(CfgMetadata.DataType.String.getCode());
		md.setDesc("操作系统名称");
		metadatas.add(md);*/
		
	}

	@Override
	public String getResourceName() {
		return RES_NAME;
	}

}
