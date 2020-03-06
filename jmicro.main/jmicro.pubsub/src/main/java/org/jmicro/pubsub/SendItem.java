package org.jmicro.pubsub;

import org.jmicro.api.annotation.SO;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;

@SO
public class SendItem {

	transient public static final int TYPY_RESEND = 1;
	
	transient public ISubCallback cb;
	public PSData[] items;
	public int retryCnt=0;
	
	public long time = 0;
	
	public ServiceMethod sm = null;
	
	public String topic = null;
	
	public SendItem(int type,ISubCallback cb,PSData[] items,int retryCnt) {
		if(items == null || items.length == 0 || items[0] == null) {
			throw new CommonException("SendItem items PSData cannot be NULL");
		}
		this.cb = cb;
		this.items = items;
		this.retryCnt = retryCnt;
		time = System.currentTimeMillis();
		if(cb != null) {
			this.sm = cb.getSm();
			this.topic = this.sm.getTopic();
		} else {
			topic = items[0].getTopic();
		}
	}

}
