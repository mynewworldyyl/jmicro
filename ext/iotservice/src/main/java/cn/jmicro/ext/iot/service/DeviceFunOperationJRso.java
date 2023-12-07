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
	
	public static final String TABLE = "t_device_fun_op";
	
	//操作定义来源，设备，产品，用户
	public static final byte SRC_PRODUCE = 1;
	public static final byte SRC_DEVICE = 2;
	public static final byte SRC_USER = 3;
	
	//操作ID设备操作都是０，终端用户提交操作ID全局唯一
	private Integer id;
	
	private Integer clientId;
	
	//操作所属定义ID
	private Integer defId;
	
	//操作所属功能ID
	private Integer funId;
	
	//操作所属产品ID
	private Integer productId;
	
	//当by==2时，设备指令
	private String deviceId; //实现接口中的设备ID
	
	//参数是否可以由用户自定义，在调用前，弹出确认或修改参数对话框，参数来源于DeviceFunDefJRso.selfDefArg
	private Boolean selfDefArg;
	//外部资源标识，用平匹配命令，比如根据ASRPRO语音识别获取对应命令
	//private Integer resId;
	
	//private Integer srcActId; //设备所属账号ID
	
	//操作定义来源，设备，产品，用户   1:产品， 2： 设备,  3:用户
	private byte by;
	
	private boolean enable;
	
	private String name;
	
	private String desc;//语意或功能描述
	
	private Byte argLen;
	
	//private Boolean isRequired;
	
	private Set<OperationArgJRso> args;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
