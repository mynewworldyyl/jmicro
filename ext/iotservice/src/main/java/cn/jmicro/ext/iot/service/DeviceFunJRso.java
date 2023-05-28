package cn.jmicro.ext.iot.service;

import java.util.ArrayList;
import java.util.List;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

/**
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
	
	private Integer  id;
	
	private String deviceId;
	
	//设备所属账号ID
	private Integer srcActId;
	
	//设备物理地址或设备硬件ID
	private String funName;
	
	//后台统一以字符串存储
	private Byte ver=1;
	
	private String funDesc;//语意或功能描述
	
	private boolean del = false;
	
	private List<DeviceFunArgsJRso> args = new ArrayList<>();
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
