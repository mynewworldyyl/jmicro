package cn.jmicro.resource;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.monitor.AbstractResource;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.ResourceData;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Utils;

@Component
public class DiskResource extends AbstractResource  implements IResource {

	private static String RES_NAME = "disk";
	
	private DecimalFormat DECIMALFORMAT = new DecimalFormat("#.##");
	
	public static final String PATH = "path";
	public static final String FREE_SPACE = "freeSpace";
	public static final String USED_SPACE = "usableSpace";
	public static final String TOTAL_SPACE = "totalSpace";
	public static final String USABLE_TASK_PERCENT = "usableTakePercent";
	public static final String USED_TAKE_PERCENT = "usedTakePercent";
	
	@Inject
	private IDataOperator op;
	
	@Override
	public ResourceData getResource(Map<String,Object> params) {
		this.data.putData("diskList", getInfo(params));
		return this.data;
	}

	public List<Map<String, String>> getInfo(Map<String,Object> params) {
		
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
 
		//获取磁盘分区列表
		File[] roots = File.listRoots();
		for (File file : roots) {
			Map<String, String> map = new HashMap<String, String>();
			
			long freeSpace = file.getFreeSpace();
			long totalSpace = file.getTotalSpace();
			long usedSpace = totalSpace - freeSpace;
			
			map.put(PATH, file.getPath());
			//空闲空间
			map.put(FREE_SPACE, Utils.getIns().bestDataSizeVal(freeSpace));
			//可用空间
			map.put(USED_SPACE, Utils.getIns().bestDataSizeVal(usedSpace));
			//总空间
			map.put(TOTAL_SPACE,Utils.getIns().bestDataSizeVal(totalSpace));
			//可用所占百分比
			map.put(USABLE_TASK_PERCENT, DECIMALFORMAT.format(((double)freeSpace/(double)totalSpace)*100));
			//已使用占百分比
			map.put(USED_TAKE_PERCENT, DECIMALFORMAT.format(((usedSpace*1.0)/totalSpace)*100));
			
			list.add(map);
		}
 
		return list;
	}

	@Override
	public void ready() {
		super.ready();
		
		this.data.setResName(RES_NAME);
		Set<CfgMetadata> metadatas = pi.getMetadatas();
		
		//根目录
		CfgMetadata md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(PATH);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.String.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(FREE_SPACE);
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
		md.setName(USED_SPACE);
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
		md.setName(TOTAL_SPACE);
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
		md.setName(USABLE_TASK_PERCENT);
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
		md.setName(USED_TAKE_PERCENT);
		if(!metadatas.contains(md)) {
			md.setDataType(CfgMetadata.DataType.Float.getCode());
			md.setDefVal("");
			md.setRequired(false);
			md.setUiBoxType(CfgMetadata.UiType.Text.getCode());
			md.setReadonly(true);
			md.setEnable(true);
			metadatas.add(md);
		}
		
	}

}
