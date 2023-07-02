package cn.jmicro.ext.iot.service;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

@SO
@Data
@Serial
public class OperationArgJRso {

	private String name; //参数名称
	private String val;//参数值
	
	private Byte len;//值长度
	
	private Byte valType;//值的类型
}
