package cn.jmicro.ext.iot.service;

import java.util.HashSet;
import java.util.Set;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

/**
 * 设备开放能力，其他设备可能通过设备开放能力操作此设备
 	char *funDesc; //方法描述，或功能描述
	char *funName; //device unique function name
	sint8_t type; //PS item type, 默认是-128，当前只支持-128
	ctrl_fn fn; //function
	ctrl_arg* args;//方法用到的参数
	sint8_t ver;//版本
	
 * @author Yulei Ye
 * @date 2023年5月28日 上午9:35:35
 */
@SO
@Data
@Serial
public class DeviceFunJRso {
	
	public static final String TABLE = "t_device_fun";
	
	private Integer id;
	
	private Integer defId; //接口定义ID
	
	private Byte ver; //实现在功能版本
	
	private Byte type; //PS item type, 默认是-128，当前只支持-128
	
	private String deviceId; //实现接口中的设备ID
	
	private String funLabel; 
	
	private Integer srcActId; //设备所属账号ID
	
	private Set<DeviceFunOperationJRso> ctrlOps = new HashSet<>();
	
	//private Byte argLen; //操作个数
	private Boolean selfDefArg; //是否持自定义参数
	
	private Boolean showFront; //是否可以展示在UI上，用户可直接操作
	
	private Boolean del;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
