package org.jmicro.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.jmicro.api.exception.CommonException;
import org.jmicro.api.server.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSocket {

	static final Logger logger = LoggerFactory.getLogger(ClientSocket.class);
	
	private String host;
	private int port;
	
	private SocketChannel channel ;
	private Selector selector;
	
	private IMessageHandler handler;
	
	private AtomicLong idgenerator = new AtomicLong();
	
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024*1024*64);
	
	private boolean isConnected = false;
	
	private int registOpts = 0;
	
	private Queue<Message> writeQueue = new ConcurrentLinkedQueue<Message>();
	//private Queue<Integer> registions = new ConcurrentLinkedQueue<Integer>();
	
	public ClientSocket(String host,int port,IMessageHandler handler){
		this.port = port;
		this.host = host;
		this.handler = handler;
		init();
	}
	
	private void init() {
		  //创建Socket对象
        try {
        	selector = Selector.open();
            try {
            	channel = SocketChannel.open();
            } catch (IOException e) {
                throw new CommonException("can't create a new socket, out of file descriptors ?", e);
            }

            try {
            	channel.socket().setSoTimeout(10000000);
            } catch (SocketException e) {
                throw new CommonException("can't set socket timeout", e);
            }

            // non blocking
            try {
            	channel.configureBlocking(false);
            } catch (IOException e) {
                throw new CommonException("can't configure socket as non-blocking", e);
            }
            
            boolean connected;
            try {
                connected = channel.connect(new InetSocketAddress(host,port));
            } catch (IOException e) {
            	throw new CommonException("can't configure socket as non-blocking", e);
            }
            startReadWorker();
            if(connected){
            	this.isConnected = true;
    			channel.register(selector, SelectionKey.OP_READ);
            }else{
            	channel.register(selector, SelectionKey.OP_CONNECT);
            }
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void startReadWorker() {
		new Thread(()->{
			for(;;){
				try {
					 final int readyCount = selector.select();
					  if (readyCount > 0) {
	                        final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
	                        while (it.hasNext()) {
	                            final SelectionKey key = it.next();  
	                            it.remove();
	                            if(key.isReadable()){
	                            	int count = channel.read(readBuffer);
	 	                            if(count > 0){
	 	                            	readBuffer.flip();
	 	                            	processMessageReceived(readBuffer);
	 	                            	readBuffer.clear();
	 	                            } 
	                            }
	                            if(key.isConnectable()){
	                            	logger.debug("Connected");
	                            	if(this.writeQueue.isEmpty()){
	                            		channel.register(selector, SelectionKey.OP_READ);
	                            	}else {
	                            		channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
	                            	}
	                    			
	                    			isConnected = true;
	                    			selector.wakeup();
	                            }
	                            if(key.isWritable()){
	                            	logger.debug("Connected");
	                    		    doWrite();
	                            }
	                        }
	                    }
					  
					  if(this.registOpts != 0){
						  doRegist();
					  }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}).start();
		
	}

	private void doWrite() {
		
		//Message msg = this.writeQueue.poll();
		for(Message msg : this.writeQueue) {
			if(msg == null){
				logger.debug("No Message to write");
				continue;
			}
			
			try {
	            channel.write(ByteBuffer.wrap(msg.encode()));
	       } catch (final IOException e) {
	           logger.error("Exception while reading : ", e);
	       }
		}
		
	}

	private void processMessageReceived(ByteBuffer rb) {
		ByteBuffer b = readBuffer;
		b.put(rb);
		
		int totalLen = b.remaining();
		if(totalLen < Message.HEADER_LEN) {
			return;
		}
		
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
		
		this.handler.onMessage(msg);
	}
	
	private synchronized void doRegist() {
		try {
			channel.register(selector, registOpts);
			registOpts=0;
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void  writeMessage(Message msg) {
		writeQueue.offer(msg);
		registOpts = channel.keyFor(selector).interestOps() | SelectionKey.OP_WRITE;
		//registions.offer(SelectionKey.OP_WRITE);
		if(this.isConnected){
			selector.wakeup();
		}
	}
	
	
}
