package cn.jmicro.api.tx;

import lombok.Data;
import lombok.Serial;

@Serial
@Data
public class TxConfigJRso {

	private int pid;
	
	private long timeout;
	
}
