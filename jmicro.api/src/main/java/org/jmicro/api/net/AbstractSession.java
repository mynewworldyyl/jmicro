/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.api.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.api.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:22
 */
public abstract class AbstractSession implements ISession{

	private static final AtomicInteger ID = new AtomicInteger(0);
	private static final Logger logger = LoggerFactory.getLogger(AbstractSession.class);
	
	private long sessionId = -1L;
	
	private Map<String,Object> params = new ConcurrentHashMap<String,Object>();
	
	private int bufferSize = 4096;
	
	private volatile ByteBuffer readBuffer = null;
	
	private int heardbeatInterval;
	
	private long lastActiveTime = System.currentTimeMillis();
	
	private boolean isClose = false;
	
	private boolean openDebug = false;
	
	private boolean dumpDownStream = false;
	
	private boolean dumpUpStream = false;
	
	private IMessageReceiver receiver = null;
	
	private Queue<ByteBuffer> readQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	
	private Worker worker = new Worker();
	
	private class Worker extends Thread {
		public void run() {
			doWork();
		}
	}
	
	public AbstractSession(int bufferSize,int heardbeatInterval){
		//this.receiver = receiver;
		this.bufferSize = bufferSize;
		this.heardbeatInterval = heardbeatInterval;
		if(!this.isServer()) {
			//会对客户端会话，支持N个RPC同时并发
			worker.setName("JMicro-"+Config.getInstanceName()+"_Session_Reader"+ID.incrementAndGet());
			worker.start();
		}
	}
	
	public void doWork() {
		while(true) {
			try {
				if(readQueue.isEmpty()) {
					synchronized (worker) {
						worker.wait();
					}
				}
				ByteBuffer bb = null;
				while((bb = readQueue.poll()) != null) {
					doRead(bb);
				}
				
			}catch(Throwable e) {
				logger.error("",e);
			}
		}
	}
	
	@Override
	public void receive(ByteBuffer msg) {
		if(this.isServer()) {
			doRead(msg);
		} else {
			readQueue.offer(msg);
			if(readQueue.size() == 1) {
				synchronized (worker) {
					worker.notify();;
				}
			}
		}
	}

	private void doRead(ByteBuffer msg) {

    	//合并上次剩下的数据
     	ByteBuffer lb = null;
     	
     	//logger.debug("T {}, GOT DATA: {}",Thread.currentThread().getName(),msg);
     	
     	if(this.readBuffer == null || this.readBuffer.remaining() <= 0) {
     		this.readBuffer = null;
     		lb = msg;
     	} else {
     		 if(openDebug) {
     			logger.debug("combine buffer {}/{}",msg,this.readBuffer);
     		 }
     		
     		int size = msg.remaining() + this.readBuffer.remaining();
     		lb = ByteBuffer.allocate(size);
     		
     		lb.put(this.readBuffer);
     		this.readBuffer = null;
     		
     		lb.put(msg);
     		
         	lb.flip();
     	}
    	
     	while(true) {
     		
     		 Message message = null;
              try {
            	 message =  Message.readMessage(lb);
          		 if(message == null){
                   	break;
                  }
          		  if(message.isDebugMode()) {
          			 //logger.debug("T{},payload:{}",Thread.currentThread().getName(),message);
          		  }
			} catch (Throwable e) {
				this.close(true);
				logger.error("",e);
				throw e;
			}
              
              //服务言接收信息是上行，客户端接收信息是下行
       		  //dump(lb.array(),this.isServer(),message);
              
              if(openDebug) {
              	logger.debug("Got message reqId: "+ message.getReqId());
              }
             //JMicroContext.configProvider(message);
              receiver.receive(this,message);
     	 }
     	
     	if(lb.remaining() > 0) {
     		if(openDebug) {
     			logger.debug("remaiding data: {}",lb);
              }
     		this.readBuffer = lb;
     	}
     	
	}
	
	public void dump(byte[] data,boolean up) {
		if(!this.dumpUpStream && !this.dumpDownStream ) {
			//全为false,直接返回
			return;
		}

 		try {
			if(up && (this.dumpUpStream)) {
				this.doDumpUpStream(ByteBuffer.wrap(data));
			} else if(!up && (this.dumpDownStream)) {
				this.doDumpDownStream(ByteBuffer.wrap(data));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void dump(byte[] data,boolean up,Message message) {

		if(!this.dumpUpStream && !this.dumpDownStream 
				&& !message.isDumpDownStream() && !message.isDumpUpStream()) {
			//全为false,直接返回
			return;
		}

 		try {
			if(up && (this.dumpUpStream || message.isDumpUpStream())) {
				this.doDumpUpStream(ByteBuffer.wrap(data));
			} else if(!up && (this.dumpDownStream || message.isDumpDownStream())) {
				this.doDumpDownStream(ByteBuffer.wrap(data));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	
	
	public abstract InetSocketAddress getLocalAddress();
	
	public abstract InetSocketAddress getRemoteAddress();
	
	public abstract boolean isServer();

	private void doDumpDownStream(ByteBuffer buffer) {
		DumpManager.getIns().doDump(buffer);
	}
	
	private void doDumpUpStream(ByteBuffer buffer) {
		DumpManager.getIns().doDump(buffer);
	}

	@Override
	public int getReadBufferSize() {
		return this.bufferSize;
	}

	@Override
	public void active() {
		lastActiveTime = System.currentTimeMillis();
	}

	@Override
	public boolean isActive() {
		return (System.currentTimeMillis() - this.lastActiveTime) < (this.heardbeatInterval * 1000)*5;
	}

	public long getId() {
		return sessionId;
	}

	public boolean isOpenDebug() {
		return openDebug;
	}

	public void setOpenDebug(boolean openDebug) {
		this.openDebug = openDebug;
	}

	public void setId(long sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public int hashCode() {
		return new Long(sessionId).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null){
			return false;
		}
		if(!(obj instanceof AbstractSession)) {
			return false;
		}
		AbstractSession as = (AbstractSession)obj;
		return this.sessionId == as.getId();
	}

	@Override
	public void close(boolean flag) {
		params.clear();
		this.sessionId=-1L;
		this.isClose = true;
	}

	@Override
	public Object getParam(String key) {
		return this.params.get(key);
	}

	@Override
	public void putParam(String key, Object obj) {
		this.params.put(key, obj);
	}

	@Override
	public boolean isClose() {
		return this.isClose;
	}

	@Override
	public String remoteHost() {
		return this.getRemoteAddress().getHostName();
	}

	@Override
	public int remotePort() {
		return this.getRemoteAddress().getPort();
	}

	@Override
	public String localHost() {
		return this.getLocalAddress().getHostName();
	}

	@Override
	public int localPort() {
		return this.getLocalAddress().getPort();
	}
	
	public boolean isDumpDownStream() {
		return dumpDownStream;
	}

	public void setDumpDownStream(boolean dump) {
		this.dumpDownStream = dump;
	}

	public boolean isDumpUpStream() {
		return dumpUpStream;
	}

	public void setDumpUpStream(boolean dump) {
		this.dumpUpStream = dump;
	}

	public IMessageReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(IMessageReceiver receiver) {
		this.receiver = receiver;
	}
	
}
