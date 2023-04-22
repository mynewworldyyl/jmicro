package cn.jmicro.ext.iot.service;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

@SO
@Data
@Serial
public class IotDeviceJRso {

	private Long  id;
	
	private String deviceId;
	
	//设置关联账号租户ID，一个租户可以关联N个设备
	private Integer srcClientId;
	
	//设备所属账号ID
	private Integer srcActId;
	
	private String name;
	
	private String desc;
	
	private int status;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
