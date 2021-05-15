package cn.jmicro.api.tx;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class TxInfo {

	private int serverId;
	
	private long txid;
	
}
