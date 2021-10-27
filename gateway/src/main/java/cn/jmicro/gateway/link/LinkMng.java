package cn.jmicro.gateway.link;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.choreography.ProcessInfoJRso;
import cn.jmicro.api.classloader.RpcClassLoader;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.IServer;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.SecretManager;
import cn.jmicro.api.timer.TimerTicker;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;

@Component(value="linkMessageHandler")
public class LinkMng implements IMessageHandler {

	private final static Logger logger = LoggerFactory.getLogger(LinkMng.class);
	
	//消息与下行会话关系映射
	private ConcurrentHashMap<Long,LinkNode> ppMsgId2DownSess = new ConcurrentHashMap<>();
	
	private ConcurrentHashMap<Long,LinkNode> manyMsgId2DownSess = new ConcurrentHashMap<>();
	
	private Random r = new Random(System.currentTimeMillis()%1000);
	
	@Inject
	private RpcClassLoader rpcClassloader;
	
	@Inject
	private ICache cache;
	
	@Inject
	private SecretManager secretMng;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ProcessInfoJRso pi;
	
	@Inject(value="clientMessageReceiver")
	private IMessageReceiver cr;
	
	@Inject
	private ICodecFactory codeFactory;
	 
	@Cfg(value ="/gateway/linkTimeout",defGlobal=true)
	private long timeout = 5*60*1000;
	
    public static class LinkNode {
    	private int respType = Message.MSG_TYPE_PINGPONG;
    	private ISession sec;
    	private long msgId;//服务端全局唯一ID
    	private Message srcMsg;
    	private long cMsgId;//客户端过来的消息ID
    	private long lastActiveTime = TimeUtils.getCurTime();
    	
    	private ServiceMethodJRso sm;
    	
    	private Map<Byte,Object> extraMap;
    	private int flag;
    	private Byte type;
    	private int extrFlag;
    }
    
    public void jready() {
    	TimerTicker.doInBaseTicker(5, "", "", (key,att)->{
    		doCheck();
    	});
    }

	private void doCheck() {
		checkWithTimeout(ppMsgId2DownSess,this.timeout);
		checkWithTimeout(manyMsgId2DownSess,this.timeout*10);
	}

	private void checkWithTimeout(ConcurrentHashMap<Long, LinkNode> nodes, long to) {
		if(nodes.isEmpty()) return;
		Set<Long> keys = new HashSet<>();
		keys.addAll(nodes.keySet());
		Iterator<Long> ite = keys.iterator();
		long curTime = TimeUtils.getCurTime();
		
		while(ite.hasNext()) {
			Long mid = ite.next();
			LinkNode n = nodes.get(mid);
			if(n == null) continue;
			if(curTime - n.lastActiveTime > to) {
				nodes.remove(n.msgId);
				String errMsg = "Remove timeout link: " + n.srcMsg.toString();
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
		
		LinkNode n = new LinkNode();
		n.respType = msg.getRespType();
		n.sec = session;
		n.cMsgId = msg.getMsgId();
		n.msgId = idGenerator.getLongId(Message.class);
		n.srcMsg = msg;
		n.msgId = msg.getMsgId();
		n.lastActiveTime = TimeUtils.getCurTime();
		n.extrFlag = msg.getExtrFlag();
		
		n.sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		
		msg.setMsgId(n.msgId);
		
		Map<Byte,Object> extraMap = msg.getExtraMap();
		if(extraMap != null && !extraMap.isEmpty()) {
			n.extraMap = new HashMap<>();
			n.extraMap.putAll(extraMap);
		}
		
		n.type = msg.getType();
		n.flag = msg.getFlag();
		
		if(msg.getRespType() == Message.MSG_TYPE_MANY_RESP) {
			manyMsgId2DownSess.put(n.msgId, n);
			msg.putExtra(Message.EXTRA_KEY_MSG_ID, n.msgId);
		}else {
			ppMsgId2DownSess.put(n.msgId, n);
		}
	}

	@Override
	public Byte type() {
		return -1;
	}

	@Override
	public boolean onMessage(ISession session, Message msg) {
		
		LinkNode n = null;
		
		boolean pp = true;
		
		if(msg.getRespType() == Message.MSG_TYPE_PINGPONG) {
			pp = true;
			n = ppMsgId2DownSess.get(msg.getMsgId());
		}else if (msg.getRespType() == Message.MSG_TYPE_MANY_RESP) {
			pp = false;
			n = manyMsgId2DownSess.get(msg.getMsgId());
		}
		
		if(n == null) {
			//String msgErr = "Client link not found for:" + msg.toString();
			//logger.error(msgErr);
			//LG.log(MC.LOG_ERROR, LinkMng.class,msgErr);
			return false;
		}
		
		msg.setMsgId(n.cMsgId);//还原客户端的消息ID
		
		Map<Byte,Object> extraMap = msg.getExtraMap();
		if(extraMap != null && (msg.isUpSsl() || msg.isDownSsl())) {
			extraMap.remove(Message.EXTRA_KEY_SALT);
			extraMap.remove(Message.EXTRA_KEY_SEC);
			extraMap.remove(Message.EXTRA_KEY_SIGN);
		}
		
		msg.setOuterMessage(true);
		
		if(msg.getDownProtocol() == Message.PROTOCOL_BIN && 
				Message.is(n.flag, Message.FLAG_FORCE_RESP_JSON)) {
			//客户端要求强制转JSON
			final Object resp = ICodecFactory.decode(this.codeFactory, msg.getPayload(),
					RespJRso.class, msg.getUpProtocol());
			if(resp != null) {
				//二进制转JSON，web客户端无法识别带类型的二进制数据包
				Object jsonPayload = ICodecFactory.encode(this.codeFactory, resp, Message.PROTOCOL_JSON);
				msg.setPayload(jsonPayload);
				msg.setDownProtocol(Message.PROTOCOL_JSON);
			}
		}
		
		if(!msg.isError() && (Message.is(n.extrFlag, Message.EXTRA_FLAG_UP_SSL) 
				|| Message.is(n.extrFlag, Message.EXTRA_FLAG_DOWN_SSL)
				/*msg.isUpSsl() || msg.isDownSsl()*/)) {
			//由客户端决定返回数据加解密方式
			if(extraMap == null) {
				msg.setExtraMap(new HashMap<>());
			}
			msg.getExtraMap().putAll(n.extraMap);
			secretMng.signAndEncrypt(msg, msg.getInsId());
		} else {
			//错误不需要做加密或签名
			msg.setDownSsl(false);
			msg.setUpSsl(false);
		}
		
		msg.setInsId(pi.getId());
		
		cacheData(msg,n);
		
		n.sec.write(msg);
		
		if(pp) {
			//请求响应类消息
			ppMsgId2DownSess.remove(msg.getMsgId());
		} else {
			n.lastActiveTime = TimeUtils.getCurTime();
		}
		
		return true;
	}
    
	
	private void cacheData(Message msg,LinkNode n) {
		if(n.sm == null || msg.isError()) return;
		ServiceMethodJRso sm = n.sm;
		
		if(sm.getCacheType() != Constants.CACHE_TYPE_NO) {
			boolean doc = true;
			//默认非RespJRso响应实例，全部缓存
			/*if(resp.getResult() instanceof RespJRso) {
				RespJRso re = (RespJRso) resp.getResult();//成功响应才做缓存
				if(re.getCode() != RespJRso.CODE_SUCCESS || re.getData() == null) {
					doc = false;
				}
			}*/
			
			if(doc) {
				String ck = IServer.cacheKey(rpcClassloader,msg, sm,null);
				if(ck != null) {
					int et = sm.getCacheExpireTime();
					ByteBuffer sb = (ByteBuffer)msg.getPayload();
					sb.mark();
					if(et > 0) {
						et = et*1000;
						int bt = (int)(et*0.5);
						bt = r.nextInt() % bt;//加一个0到二分之一正负ET之间的随机数，避免缓存雪崩
						int rv = et + bt;
						cache.put(ck, msg.getPayload(),rv);
					}else {
						cache.put(ck, msg.getPayload(),Math.abs(r.nextInt()));
					}
					sb.reset();
				}
			}
		}
	}
}
