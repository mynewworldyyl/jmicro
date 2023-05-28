package cn.jmicro.ext.iot.service;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

/**
 	char *name; //参数名称，如op,code,msg等
	sint8_t type; //参数类型，参考jm_msg.h文件  PREFIX_TYPE_BYTE，PREFIX_TYPE_SHORT等
	sint8_t maxLen; //参数最大长度
	char *defVal; //默认值
 * @author Yulei Ye
 * @date 2023年5月28日 上午9:39:39
 */
@SO
@Data
@Serial
public class DeviceFunArgsJRso {
	
	private Integer  id;
	
	private Integer  funId;
	
	//设备物理地址或设备硬件ID
	private String name;
	
	//数据类型，参考DecoderConstant常量
	private Byte type;
	
	//默认值
	private String defVal;
	
	private String desc;//语意或功能描述
	
	private Byte maxLen;
	
	private Byte status;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
