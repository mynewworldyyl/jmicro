package org.jmicro.transport.mina;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import org.apache.mina.api.AbstractIoHandler;
import org.apache.mina.api.IoSession;
import org.apache.mina.session.AttributeKey;
import org.apache.mina.transport.nio.NioTcpServer;
import org.jmicro.api.IIdGenerator;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Lazy;
import org.jmicro.api.annotation.Server;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.server.IServer;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.servicemanager.JmicroManager;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Server(Constants.DEFAULT_SERVER)
@Lazy(false)
public class MinaServer implements IServer{

	static final Logger LOG = LoggerFactory.getLogger(MinaServer.class);
	
	AttributeKey<MinaServerSession> sessinKey = AttributeKey.createKey(MinaServerSession.class, Constants.SESSION_KEY);
	
	//@Inject(required=false)
	private  NioTcpServer acceptor;
	
	//@Inject(required=false)
	private AbstractIoHandler iohandler;
	
	//@Inject
	//private IRequestHandler reqHandler;
	
	@Inject
	private JmicroManager jmicroManager;
			 
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject(value=Constants.DEFAULT_CODEC_FACTORY,required=true)
	private ICodecFactory codecFactory;
	
	@Cfg("/bindIp")
	private String host;
	
	@Cfg("/port")
	private int port;
	
	@Override
	public void init() {
		start();
	}

	@Override
	public void start() {

        LOG.info("starting echo server");

        if(acceptor == null){
        	 acceptor = new NioTcpServer();
        }
        
        if(this.iohandler == null){
        	this.iohandler = new AbstractIoHandler() {
                @Override
                public void sessionOpened(final IoSession session) {
                    LOG.info("session opened {}", session);           
                    MinaServerSession s = new MinaServerSession(session);
                    s.putParam(Constants.SESSION_KEY, session);
                    //s.setSessionId(idGenerator.getLongId(ISession.class));
                    session.setAttribute(sessinKey, s);
                }

                @Override
                public void messageReceived(IoSession session, Object message) {
                    LOG.info("echoing");
                    ByteBuffer rb  = (ByteBuffer)message;
                    MinaServerSession s = session.getAttribute(sessinKey);
                    
                    ByteBuffer b = s.getReadBuffer();
        			b.put(rb);
        			
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
        			
        			Message msg = new Message();
        			msg.decode(b.array());
        			
        			b.position(len+Message.HEADER_LEN);
        			b.compact();
        			
        			if(s.getSessionId() != -1 && msg.getSessionId() != s.getSessionId()) {
        				LOG.warn("Ignore MSG" + msg.getId() + "Rec session ID: "+msg.getSessionId()+",but this session ID: "+s.getSessionId());
        				return;
        			}
                   
                    s.setSessionId(msg.getSessionId());
                    JMicroContext cxt = JMicroContext.get();
            		cxt.getParam(JMicroContext.SESSION_KEY, session);
            		
            		RpcRequest req = new RpcRequest();
            		req.decode(msg.getPayload());
            		req.setSession(s);
            		req.setMsg(msg);
                   
            		//IResponse resp = reqHandler.onRequest(req);
            		jmicroManager.addRequest(req);
            		
            		/*msg.setPayload(resp.encode());
            		msg.setReq(false);
            		msg.setType(Message.PROTOCOL_TYPE_END);
                    s.write(msg);*/
                }
            };
        }

        // create the filter chain for this service
        //acceptor.setFilters(new LoggingFilter("LoggingFilter1"));
        acceptor.setIoHandler(this.iohandler);

    	
        final SocketAddress address = new InetSocketAddress(this.port);
        acceptor.bind(address);
        LOG.debug("Running the server host["+this.host+"],port ["+this.port+"]");    
		
	}

	@Override
	public void stop() {
		 if(acceptor != null){
			 acceptor.unbind();
			 acceptor = null;
        }
	}

	@Override
	public String host() {
		return this.host;
	}

	@Override
	public int port() {
		return this.port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
