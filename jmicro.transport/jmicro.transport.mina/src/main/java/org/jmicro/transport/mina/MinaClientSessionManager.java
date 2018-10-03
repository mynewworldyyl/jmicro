package org.jmicro.transport.mina;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoFuture;
import org.apache.mina.api.IoSession;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.transport.nio.NioTcpClient;
import org.jmicro.api.IIdGenerator;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.client.IResponseHandler;
import org.jmicro.api.client.ReqResp;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.ISession;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcResponse;
import org.jmicro.api.server.ServerError;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(lazy=false)
public class MinaClientSessionManager implements IClientSessionManager{

	static final Logger LOG = LoggerFactory.getLogger(MinaClientSessionManager.class);

	AttributeKey<IClientSession> sessionKey = AttributeKey.createKey(IClientSession.class, Constants.SESSION_KEY);
	
	private final Map<Long,ReqResp> waitForResponse = new ConcurrentHashMap<>();
	
	//private final Map<Long,IClientSession> sessions = new ConcurrentHashMap<>();
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Override
	public void write(IRequest req, IResponseHandler handler,int retryCnt) {
		
		Message msg = new Message();
		msg.setType(Message.PROTOCOL_TYPE_BEGIN);
		msg.setId(this.idGenerator.getLongId(Message.class));
		msg.setReqId(req.getRequestId());
		msg.setSessionId(req.getSession().getSessionId());
		msg.setPayload(req.encode());
		msg.setExt((byte)0);
		msg.setReq(true);
		msg.setVersion(Message.VERSION_STR);
		
		waitForResponse.put(req.getRequestId(), new ReqResp(msg,req,handler,retryCnt));
		
		byte[] data = msg.encode();
		
		IClientSession cs = (IClientSession)req.getSession();

		cs.write(ByteBuffer.wrap(data));
		
	}

	@Override
	public IClientSession connect(String host, int port) {

        LOG.info("starting echo client");

        final NioTcpClient client = new NioTcpClient();
        client.setFilters();
        client.setIoHandler(new AbstractIoHandler() {
            @Override
            public void sessionOpened(final IoSession session) {
                LOG.info("session opened {}", session);
            }

            @Override
            public void messageReceived(IoSession session, Object message) {
                LOG.info("message received {}", message);
                doReceive(session,(ByteBuffer)message);
            }

            @Override
            public void messageSent(IoSession session, Object message) {
                LOG.info("message sent {}", message);
            }

            @Override
            public void sessionClosed(IoSession session) {
                LOG.info("session closed {}", session);
            }
        });

        try {
            IoFuture<IoSession> future = client.connect(new InetSocketAddress(host,port));
            try {
                IoSession session = future.get();
                LOG.info("session opened {}", session);
                
                MinaClientSession s = new MinaClientSession(session);
                s.setSessionId(this.idGenerator.getLongId(ISession.class));
                s.putParam(Constants.SESSION_KEY, session);
                
                session.setAttribute(sessionKey, s);
                //this.sessions.put(s.getSessionId(), s);
                
                LOG.info("session connected : {}", session);
                return s;
            } catch (ExecutionException e) {
                LOG.error("cannot connect : ", e);
            }

            LOG.debug("Running the client for 25 sec");
            Thread.sleep(25000);
        } catch (InterruptedException e) {
        }
    
		return null;
	}

	protected void doReceive(IoSession session, ByteBuffer message) {
		IClientSession cs = session.getAttribute(this.sessionKey);
		ByteBuffer b = cs.getReadBuffer();
		b.put(message);
		
		int totalLen = b.remaining();
		if(totalLen < Message.HEADER_LEN) {
			return;
		}
		
		b.flip();
		
		b.mark();
		
		int len = b.getInt();
		b.reset();
		if(totalLen-10 < len){
			return ;
		}
		
		
		byte[] data = new byte[Message.HEADER_LEN+len];
		b.get(data);
		b.compact();
		
		Message msg = new Message();
		msg.decode(data);
		
		//receive response
		 ReqResp rr = waitForResponse.get(msg.getReqId());
				
		if(rr != null && msg.getSessionId() != cs.getSessionId()) {
			rr.req.setSuccess(false);
			LOG.warn("Ignore MSG" + msg.getId() + "Rec session ID: "+msg.getSessionId()+",but this session ID: "+cs.getSessionId());
			return;
		}

		
		RpcResponse resp = new RpcResponse(msg.getId());
		resp.decode(msg.getPayload());
		resp.setMsg(msg);
		
		if(rr != null) {
			if(resp.getResult() instanceof ServerError){
				//should do retry or time logic
				ServerError se = (ServerError)resp.getResult();
				LOG.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg());
				rr.req.setSuccess(false);
				rr.handler.onResponse(resp,rr.req,se);
			}else {
				rr.req.setSuccess(true);
				rr.handler.onResponse(resp,rr.req,null);
			}
		}
		
		if(msg.getType() == Message.PROTOCOL_TYPE_END) {
			waitForResponse.remove(msg.getReqId());
			cs.close(false);
		}
		
	}
	
}
