package cn.jmicro.ext.iot.service;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

@SO
@Data
@Serial
public class IotDeviceJRso {

	public static final Byte TYPE_LIGHT = 1;//灯
	public static final Byte TYPE_TV = 2;//电视机
	public static final Byte TYPE_FRIG = 3;//冰箱
	public static final Byte TYPE_AP = 4;//路由器
	
	public static final Byte TYPE_OTHER = 127;//其他
	
	private Integer  id;
	
	private String deviceId;
	
	//设备物理地址或设备硬件ID
	private String macAddr;
	
	//设置关联账号租户ID，一个租户可以关联N个设备
	private Integer srcClientId;
	
	//设备所属账号ID
	private Integer srcActId;
	
	private String grpName = "Default";
	
	private String name;
	
	private String desc;
	
	//设备类型
	private Byte type;
	
	private int status;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
