package cn.jmicro.api.tx;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class TxConfig {

	private int pid;
	
	private long timeout;
	
}
