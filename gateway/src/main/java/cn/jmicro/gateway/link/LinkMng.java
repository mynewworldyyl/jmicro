package cn.jmicro.gateway.link;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;

@Component(value="linkMessageHandler")
public class LinkMng implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(LinkMng.class);
	
	//消息与下行会话关系映射
	private ConcurrentHashMap<Long,LinkNode> msgId2DownSess = new ConcurrentHashMap<>();
	
	@Cfg(value ="/gateway/linkTimeout",defGlobal=true)
	private long timeout = 5*60*1000;
	
    public static class LinkNode {
    	private int respType = Message.MSG_TYPE_PINGPONG;
    	private ISession sec;
    	private long msgId;
    	private Message srcMsg;
    	
    	private long lastActiveTime = TimeUtils.getCurTime();
    	
    }
    
    public void ready() {
    	TimerTicker.doInBaseTicker(5, "", "", (key,att)->{
    		doCheck();
    	});
    }

	private void doCheck() {
		
		if(msgId2DownSess.isEmpty()) return;
		Set<Long> keys = new HashSet<>();
		keys.addAll(msgId2DownSess.keySet());
		Iterator<Long> ite = keys.iterator();
		long curTime = TimeUtils.getCurTime();
		
		while(ite.hasNext()) {
			Long mid = ite.next();
			LinkNode n = msgId2DownSess.get(mid);
			if(n == null) continue;
			if(curTime - n.lastActiveTime > this.timeout) {
				msgId2DownSess.remove(n.msgId);
				String errMsg = "Close timeout link: " + n.srcMsg.toString();
				logger.warn(errMsg);
				LG.log(MC.LOG_WARN, LinkMng.class, errMsg);
			}
		}
		
	}

	public void createLinkNode(ISession session, Message msg) {
		if(msg.getRespType() == Message.MSG_TYPE_NO_RESP) {
			//单向消息
			return;
		}
		
		LinkNode n = msgId2DownSess.get(msg.getMsgId());
		if(n == null) {
			synchronized(session) {
				n = msgId2DownSess.get(msg.getMsgId());
				if(n == null) {
					n = new LinkNode();
					n.respType = msg.getRespType();
					n.sec = session;
					n.srcMsg = msg;
					n.msgId = msg.getMsgId();
					n.lastActiveTime = TimeUtils.getCurTime();
					msgId2DownSess.put(n.msgId, n);
				}
			}
		}
	}

	@Override
	public Byte type() {
		return 0;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		LinkNode n = msgId2DownSess.get(msg.getMsgId());
		if(n == null) {
			String msgErr = "Client link not found for:" + msg.toString();
			logger.error(msgErr);
			LG.log(MC.LOG_ERROR, LinkMng.class,msgErr);
			return;
		}
		
		n.sec.write(msg);
		
		if(msg.getRespType() == Message.MSG_TYPE_PINGPONG) {
			//请求响应类消息
			msgId2DownSess.remove(msg.getMsgId());
		} else {
			n.lastActiveTime = TimeUtils.getCurTime();
		}
		
	}
    
}
