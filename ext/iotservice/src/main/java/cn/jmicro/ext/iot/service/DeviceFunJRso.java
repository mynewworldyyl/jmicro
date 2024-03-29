package cn.jmicro.ext.iot.service;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

/**

一个功能属于一个产品，一个产品具有多个功能
一个设备属于一个产品，设备实现产品的具体功能

具有功能后，用户可以基于功能所关联的接口定义，定制具体的操作  DeviceFunOperationJRso
	
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
	
	private Integer productId; //实现接口产口ID
	
	private String funLabel;
	
	private Integer clientId;
	
	//private Integer srcActId; //设备所属账号ID
	
	//private Set<DeviceFunOperationJRso> ctrlOps = new HashSet<>();
	
	//private Byte argLen; //操作个数
	
	private Boolean selfDefArg; //是否持自定义参数
	
	private Boolean showFront; //是否可以展示在UI上，用户可直接操作
	
	private Boolean del;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
