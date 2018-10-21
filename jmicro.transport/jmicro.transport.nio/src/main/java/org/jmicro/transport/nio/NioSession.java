package org.jmicro.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.client.IResponseHandler;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IServerSession;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.servicemanager.JmicroManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NioSession implements IServerSession,SelectorListener{

	static final Logger LOG = LoggerFactory.getLogger(NioSession.class);
	
	//private static final AtomicLong reqId = new AtomicLong();
	
	//private static final ByteBuffer buffer = ByteBuffer.allocate(1024*64);
	
	private SelectorListener rwListener;
	
	private SocketChannel channel;
	
	private SelectorLoop selectorLoop;
	 
	private SelectionKey selectionKey;
	
	private boolean isConnected;
	
	private Long sessionId;
	
	private IIdGenerator idGenerator;

	private final Queue<Message> writeMessageQueue = new ConcurrentLinkedQueue<>();
	
	//client request server to response, maybe more than one responses for one request.
	private final Map<Long,ReqResp> waitForResponse = new ConcurrentHashMap<>();
	
	private static class ReqResp{
		public Message msg;
		public IRequest req;
		public IResponseHandler handler;
		public ReqResp(Message msg,IRequest req,IResponseHandler handler){
			this.msg = msg;
			this.req = req;
			this.handler = handler;
		}
	}
	
	enum SessionState {
	        CREATED, CONNECTED, CLOSING, CLOSED, SECURING, SECURED
	    }
	protected volatile SessionState state;
	    
	NioSession(SocketChannel channel,SelectorLoop readWriteSelectorLoop,IIdGenerator idGenerator){
		this.channel = channel;
		this.selectorLoop = readWriteSelectorLoop;
		this.idGenerator = idGenerator;
	}
	
	NioSession(SocketChannel channel,SelectorLoop readWriteSelectorLoop){
		this.channel = channel;
		this.selectorLoop = readWriteSelectorLoop;
	}

	@Override
	public void write(Message resp) {
		this.writeMessageQueue.offer(resp);
		flushWriteQueue();
	}
	
	/*@Override
	public void write(Message msg,IRequest req,IResponseHandler handler) {
		waitForResponse.put(req.getRequestId(), new ReqResp(msg,req,handler));
		this.writeMessageQueue.offer(msg);
		flushWriteQueue();
	}*/
	
	@Override
	public Object getParam(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putParam(String key, Object obj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void ready(boolean accept, boolean connect, boolean read, ByteBuffer readBuffer, boolean write) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("session {} ready for accept={}, connect={}, read={}, write={}", new Object[] { this, accept,
                                    connect, read, write });
        }
        if (connect) {
            try {

                boolean isConnected = channel.finishConnect();

                if (!isConnected) {
                    LOG.error("unable to connect session {}", this);
                } else {
                    // cancel current registration for connection
                    selectionKey.cancel();
                    selectionKey = null;

                    // Register for reading
                    selectorLoop.register(false, false, true, false, this, channel, new RegistrationCallback() {
                        @Override
                        public void done(SelectionKey selectionKey) {
                            setConnected(true);
                        }
                    });
                }
            } catch (IOException e) {
                LOG.debug("Connection error, we cancel the future", e);
            }
        }

        if (read) {
            processRead(readBuffer);
        }

        if (write) {
            processWrite();
        }
        if (accept) {
            throw new IllegalStateException("accept event should never occur on NioTcpSession");
        }
	}

	

	private void processRead(final ByteBuffer readBuffer) {
        try {
            LOG.debug("readable session : {}", this);

            // Read everything we can up to the buffer size
            final int readCount = channel.read(readBuffer);
           
            LOG.debug("read {} bytes", readCount);

            if (readCount < 0) {
                // session closed by the remote peer
                LOG.debug("session closed by the remote peer");
                close(true);
            } else if (readCount > 0) {
                // we have read some data
                // limit at the current position & rewind buffer back to start &
                // push to the chain
                readBuffer.flip();

                // Plain message, not encrypted : go directly to the chain
                processMessageReceived(readBuffer);

                // And now, clear the buffer
                readBuffer.clear();

            }
        } catch (final IOException e) {
            LOG.error("Exception while reading : ", e);
            this.selectorLoop.unregister(this, channel);
        }
    }

	public void close(final boolean immediately) {
        switch (state) {
        case CREATED:
            LOG.error("Session {} not opened", this);
            throw new IllegalStateException("cannot close an not opened session");
        case CONNECTED:
            state = SessionState.CLOSING;
            if (immediately) {
                channelClose();
                processSessionClosed();
            } else {
                flushWriteQueue();
            }
            break;
        case CLOSING:
            // return the same future
            LOG.warn("Already closing session {}", this);
            break;
        case CLOSED:
            LOG.warn("Already closed session {}", this);
            break;
        default:
            throw new IllegalStateException("not implemented session state : " + state);
        }
    }
	
	private void processSessionClosed() {
			
	}
	
	private void processWrite() {
		for(Message resp = writeMessageQueue.poll(); resp != null; resp = writeMessageQueue.poll()){
			if(idGenerator != null && resp.getMsgId() <= 0) {
				resp.setMsgId(this.idGenerator.getLongId(IServerSession.class));
			}
			byte[] data = resp.getPayload();
			ByteBuffer b = ByteBuffer.allocate(Message.HEADER_LEN+data.length);
			b.putInt(data.length);
			b.put(new byte[]{0,0,0});
			b.put((byte)0);
			b.put(resp.isReq()?Message.MSG_TYPE_REQ:Message.MSG_TYPE_RESP);
			b.putLong(resp.getSessionId());
			b.putLong(resp.getMsgId());	
			b.put(data);
			b.flip();
			this.writeDirect(b);
		}
	}
	
	private void processMessageReceived(ByteBuffer readBuffer) {
			ByteBuffer b = readBuffer;
			b.put(readBuffer);
			
			int totalLen = b.remaining();
			if(totalLen < Message.HEADER_LEN) {
				return;
			}
			
			b.mark();
			
			int len = b.getInt();
			if(totalLen-10 < len){
				b.reset();
				return ;
			}
			
			Message msg = new Message();
			
			//read protocal version
			byte[] vb = new byte[3];
			b.get(vb, 0, 3);
			msg.setVersion(vb[0]+"."+vb[1]+"."+vb[2]);
			
			//read type
			msg.setType(b.get());
			
			msg.setReq(b.get() == Message.MSG_TYPE_REQ);
			msg.setMsgId(b.getLong());
			msg.setSessionId(b.getLong());
			
			byte[] payload = new byte[len];
			b.get(payload, 0, len);
			msg.setPayload(payload);
			
			b.compact();
			
			if(msg.getSessionId() != this.sessionId) {
				LOG.warn("Ignore MSG" + msg.getMsgId() + "Rec session ID: "+msg.getSessionId()+",but this session ID: "+this.sessionId);
				return;
			}
			
			if(!msg.isReq()) {
				//receive response
				ReqResp rr = waitForResponse.get(msg.getMsgId());
				if(rr != null) {
					rr.handler.onResponse(msg,rr.req);
				}
				if(msg.getType() == Message.PROTOCOL_TYPE_END) {
					waitForResponse.remove(msg.getMsgId());
				}
			} else {
				JMicroContext cxt = JMicroContext.get();
				cxt.getParam(JMicroContext.SESSION_KEY, this);
				RpcRequest req = new RpcRequest();
				req.decode(msg.getPayload());
				req.setRequestId(msg.getMsgId());
				req.setSession(this);
				JmicroManager.getIns().addRequest(req);
			}
	}
	
	public void processSessionOpen() {
		 
	}
	 
	private void processException(Throwable e) {
	
	}
	
	protected void channelClose() {
        try {
            selectorLoop.unregister(this, channel);
            channel.close();
        } catch (final IOException e) {
            LOG.error("Exception while closing the channel : ", e);
            processException(e);
        }
	}
    
    protected int writeDirect(Object message) {
        try {
            return channel.write((ByteBuffer) message);
        } catch (final IOException e) {
            LOG.error("Exception while reading : ", e);
            return -1;
        }
    }
    
    public void flushWriteQueue() {
        selectorLoop.modifyRegistration(false, false, true, this, channel, true);
    }

	@Override
	public Long getSessionId() {
		return this.sessionId;
	}

	public void setSessionId(Long sessionId) {
		this.sessionId = sessionId;
	}

	public SelectorListener getRwListener() {
		return rwListener;
	}

	public void setRwListener(SelectorListener rwListener) {
		this.rwListener = rwListener;
	}

	public SocketChannel getChannel() {
		return channel;
	}

	public void setChannel(SocketChannel channel) {
		this.channel = channel;
	}

	public SelectorLoop getSelectorLoop() {
		return selectorLoop;
	}

	public void setSelectorLoop(SelectorLoop selectorLoop) {
		this.selectorLoop = selectorLoop;
	}

	public SelectionKey getSelectionKey() {
		return selectionKey;
	}

	public void setSelectionKey(SelectionKey selectionKey) {
		this.selectionKey = selectionKey;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
	}
	
    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return null;
        }
        final Socket socket = ((SocketChannel) channel).socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getRemoteSocketAddress();
    }

    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }

        final Socket socket = ((SocketChannel) channel).socket();

        if (socket == null) {
            return null;
        }

        return (InetSocketAddress) socket.getLocalSocketAddress();
    }
    
}
