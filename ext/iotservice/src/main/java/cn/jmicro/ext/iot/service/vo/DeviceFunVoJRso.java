package cn.jmicro.ext.iot.service.vo;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.ext.iot.service.DeviceFunDefJRso;
import cn.jmicro.ext.iot.service.DeviceFunJRso;
import lombok.Data;
import lombok.Serial;

@SO
@Data
@Serial
public class DeviceFunVoJRso {

	private DeviceFunJRso df;
	
	private DeviceFunDefJRso dfd;
}
