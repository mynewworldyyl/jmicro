package cn.jmicro.ext.iot.service.vo;

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
public class DeviceFunVobJRso {
	
	private Integer id;
	
	private Integer defId; //接口定义ID
	private Integer funId; //接口ID
	
	private Integer clientId;
	
	private Byte ver; //实现在功能版本
	
	private Integer productId; //实现接口产口ID
	
	//设备物理地址或设备硬件ID
	private String funName;
	
	//功能中文名称，用于在UI上操作按钮显示
	private String labelName;
	
	private String funDesc; //语意或功能描述
	
	//private Byte argLen; //操作个数
	private Boolean selfDefArg; //是否持自定义参数
	
	private Boolean showFront; //是否可以展示在UI上，用户可直接操作
	
	private int createdBy;

}
