package cn.jmicro.ext.iot.service;

import java.util.ArrayList;
import java.util.List;

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
public class DeviceFunDefJRso {
	
	public static final String TABLE = "t_device_fun_def";
	
	public static final String DEF_GROUP = "default";
	
	private Integer  id;
	
	//设备所属账号ID
	private Integer actId;
	
	private Integer clientId;
	
	//设备物理地址或设备硬件ID
	private String funName;
	
	//功能中文名称，用于在UI上操作按钮显示
	private String labelName;
	
	private String grp = DEF_GROUP;
	
	private Boolean selfDefArg; //是否可以由终端用户自定义参数调用接口
	
	//功能版本
	private Byte ver=1;
	
	private Byte funType;
	
	//private Byte argNum;//参数个数
	
	private String funDesc; //语意或功能描述
	
	private boolean showFront = false; //是否开放给用户界面直接触发
	
	private boolean del = false;
	
	private List<DeviceFunDefArgsJRso> args = new ArrayList<>();
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
