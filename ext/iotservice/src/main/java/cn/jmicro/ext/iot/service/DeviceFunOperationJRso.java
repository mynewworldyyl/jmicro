package cn.jmicro.ext.iot.service;

import java.util.Set;

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
public class DeviceFunOperationJRso {
	
	//操作ID设备操作都是０，终端用户提交操作ID全局唯一
	private Integer idv;
	
	//外部资源标识，用平匹配命令，比如根据ASRPRO语音识别获取对应命令
	private Integer resId;
	
	//true 设备预置操作
	private boolean fromDevice;
	
	private boolean enable;
	
	private String name;
	
	private String desc;//语意或功能描述
	
	//数据类型，参考DecoderConstant常量
	//private Byte type;
	
	//默认值
	//private String defVal;
	
	private Byte argLen;
	
	//private Boolean isRequired;
	
	private Set<OperationArgJRso> args;
	
}
