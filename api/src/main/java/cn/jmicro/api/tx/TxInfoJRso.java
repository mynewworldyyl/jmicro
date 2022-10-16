package cn.jmicro.api.tx;

import lombok.Data;
import lombok.Serial;

@Serial
@Data
public class TxInfoJRso {

	private int serverId;
	
	private long txid;
	
}
