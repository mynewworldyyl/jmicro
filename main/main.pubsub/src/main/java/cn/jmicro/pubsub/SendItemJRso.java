package cn.jmicro.pubsub;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import lombok.Serial;

@SO
@Serial
public class SendItemJRso {

	transient public static final int TYPY_RESEND = 1;
	
	transient public ISubscriberCallback cb;
	public PSDataJRso[] items;
	public int retryCnt=0;
	
	public long time = 0;
	
	public ServiceMethodJRso sm = null;
	
	public String topic = null;
	
	public SendItemJRso() {}
	
	public SendItemJRso(int type,ISubscriberCallback cb,PSDataJRso[] items,int retryCnt) {
		if(items == null || items.length == 0 || items[0] == null) {
			throw new CommonException("SendItem items PSData cannot be NULL");
		}
		this.cb = cb;
		this.items = items;
		this.retryCnt = retryCnt;
		time = TimeUtils.getCurTime();
		if(cb != null) {
			this.sm = cb.getSm();
			this.topic = this.sm.getTopic();
		} else {
			topic = items[0].getTopic();
		}
	}

}
