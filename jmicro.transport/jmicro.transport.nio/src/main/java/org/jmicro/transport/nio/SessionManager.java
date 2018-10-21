package org.jmicro.transport.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.server.IServerSession;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Constants;
import org.jmicro.transport.nio.handler.JMicroIoHandler;

@Component
public class SessionManager{

	@Inject(required=true)
	private JMicroIoHandler ioHandler;
	
	@Inject(required=true)
	private IIdGenerator idGenerator;
	
	private Map<Long,NioSession> sessions = new ConcurrentHashMap<Long,NioSession>();
	
    private  SelectorLoopPool readWriteSelectorPool;
    
    protected  Executor ioHandlerExecutor;
    
	public synchronized IServerSession createSession(SocketChannel clientSocket) throws IOException {
	      
	        SocketChannel socketChannel = clientSocket;
	        SelectorLoop readWriteSelectorLoop = readWriteSelectorPool.getSelectorLoop();
	        final NioSession session = new NioSession(socketChannel, readWriteSelectorLoop);
	        Long sessionId = idGenerator.getLongId(IServerSession.class);
	        session.setSessionId(sessionId);
	        sessions.put(sessionId, session);
	        
	        socketChannel.configureBlocking(false);
	        // add the session to the queue for being added to the selector
	        readWriteSelectorLoop.register(false, false, true, false, session, socketChannel, (selectionKey)->{
	        	  session.setSelectionKey(selectionKey);
	              session.setConnected(true);
	        });
	        return session;
	}
	
	@JMethod("init")
	public void init() {
		 this.ioHandlerExecutor = Executors.newFixedThreadPool(10);
		 this.readWriteSelectorPool = new FixedSelectorLoopPool("Jmicro ",2);
	}
	
}
