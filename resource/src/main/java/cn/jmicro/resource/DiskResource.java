package cn.jmicro.resource;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadata;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.exp.Exp;
import cn.jmicro.api.exp.ExpUtils;
import cn.jmicro.api.monitor.AbstractResource;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.ResourceData;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@Component
public class DiskResource extends AbstractResource  implements IResource {

	private static String RES_NAME = "disk";
	
	private DecimalFormat DECIMALFORMAT = new DecimalFormat("#.##");
	
	public static final String PATH = "path";
	public static final String FREE_SPACE = "freeSpace";
	public static final String USED_SPACE = "usedSpace";
	public static final String TOTAL_SPACE = "totalSpace";
	public static final String USABLE_TASK_PERCENT = "usableTakePercent";
	public static final String USED_TAKE_PERCENT = "usedTakePercent";
	
	@Inject
	private IDataOperator op;
	
	@Override
	public ResourceData getResource(Map<String,Object> query,String expStr) {
		ResourceData data = this.getData();
		data.putData("diskList", getInfo(query,expStr));
		return data;
	}

	public List<Map<String, String>> getInfo(Map<String,Object> query,String expStr) {
		
		Exp exp = null;
		if(StringUtils.isNotEmpty(expStr)) {
			//客户端必须确保表达式是正确的
			exp = new Exp();
			List<String> subfix = ExpUtils.toSuffix(expStr);
			exp.setOriEx(expStr);
			exp.setSuffix(subfix);
		}
		
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
 
		//获取磁盘分区列表
		File[] roots = File.listRoots();
		for (File file : roots) {
			long freeSpace = file.getFreeSpace();
			long totalSpace = file.getTotalSpace();
			long usedSpace = totalSpace - freeSpace;
			
			if(exp != null) {
				Map<String,Object> cxt = new HashMap<>();
				cxt.put(PATH, file.getPath());
				cxt.put(FREE_SPACE, freeSpace);
				cxt.put(USED_SPACE, usedSpace);
				cxt.put(TOTAL_SPACE,totalSpace);
				cxt.put(USABLE_TASK_PERCENT, DECIMALFORMAT.format(((double)freeSpace/(double)totalSpace)*100));
				cxt.put(USED_TAKE_PERCENT, DECIMALFORMAT.format(((usedSpace*1.0)/totalSpace)*100));
				if(!ExpUtils.compute(exp, cxt, Boolean.class)) {
					continue;
				}
			}
			
			Map<String, String> map = new HashMap<String, String>();
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

	public void ready() {
		super.ready0();
		
		this.getData().setResName(RES_NAME);
		Set<CfgMetadata> metadatas = new HashSet<>();
		pi.setMetadatas(RES_NAME, metadatas);
		
		//根目录
		CfgMetadata md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(PATH);
		md.setDataType(CfgMetadata.DataType.String.getCode());
		md.setDesc("挂载盘符");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(FREE_SPACE);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("空闲空间大小");;
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(USED_SPACE);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("已使用空间大小");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(TOTAL_SPACE);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("总空间大小");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(USABLE_TASK_PERCENT);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("可用空间占比");
		metadatas.add(md);
		
		md = new CfgMetadata();
		md.setResName(RES_NAME);
		md.setName(USED_TAKE_PERCENT);
		md.setDataType(CfgMetadata.DataType.Float.getCode());
		md.setDesc("已使用空间占比");
		metadatas.add(md);
		
	}

}
