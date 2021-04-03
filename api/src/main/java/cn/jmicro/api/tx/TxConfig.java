package cn.jmicro.api.tx;

import cn.jmicro.api.annotation.SO;

@SO
public class TxConfig {

	private long timeout;

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	
}
