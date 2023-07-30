package cn.jmicro.ext.iot.service;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

@SO
@Data
@Serial
public class DeviceDataJRso {
	
	private Integer  id;
	
	private Integer productId;
	
	private String deviceId;
	
	//设置关联账号租户ID，一个租户可以关联N个设备
	private Integer srcClientId;
	
	//设备所属账号ID
	private Integer srcActId;
	
	//设备物理地址或设备硬件ID
	private String name;
	
	//后台统一以字符串存储
	private String val;
	
	private String desc;//语意或功能描述
	
	//数据类型，参考DecoderConstant常量
	private Byte type;
	
	private int status;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
