package cn.jmicro.pubsub;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.CommonException;

@SO
public class SendItem {

	transient public static final int TYPY_RESEND = 1;
	
	transient public ISubscriberCallback cb;
	public PSData[] items;
	public int retryCnt=0;
	
	public long time = 0;
	
	public ServiceMethod sm = null;
	
	public String topic = null;
	
	public SendItem() {}
	
	public SendItem(int type,ISubscriberCallback cb,PSData[] items,int retryCnt) {
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
