/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.jmicro.transport.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.server.IServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a TCP NIO based client.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Component
public class ClientSessionManager implements IClientSessionManager{

    private static final Logger LOG = LoggerFactory.getLogger(ClientSessionManager.class);

    private final SelectorLoop readWriteSelectorPool;
    private final SelectorLoop connectSelectorLoop;
    
    private final SelectorLoopPool selectorPool;

    private int connectTimeoutInMillis = 10000;
    
    private Map<Long,IClientSession> managedSessions = new HashMap<Long,IClientSession>();

    private Map<String,IClientSession> hostSessions = new HashMap<String,IClientSession>();
    
    private IIdGenerator idGenerator;
    
    public ClientSessionManager() {
    	selectorPool = new FixedSelectorLoopPool("Client",2);
        readWriteSelectorPool = selectorPool.getSelectorLoop();
        connectSelectorLoop = selectorPool.getSelectorLoop();
    }

    public IClientSession connect(String host,int port) {
    	String key = host+port;
    	if(hostSessions.containsKey(key)){
    		return hostSessions.get(key);
    	}
    	IClientSession session = this.connect(new InetSocketAddress(host,port));
    	hostSessions.put(key, session);
    	return session;
    }
    
    public IClientSession connect(SocketAddress remoteAddress) {
        assert remoteAddress!= null;

        SocketChannel clientSocket;
        try {
            clientSocket = SocketChannel.open();
        } catch (IOException e) {
            throw new CommonException("can't create a new socket, out of file descriptors ?", e);
        }

        try {
            clientSocket.socket().setSoTimeout(getConnectTimeoutMillis());
        } catch (SocketException e) {
            throw new CommonException("can't set socket timeout", e);
        }

        // non blocking
        try {
            clientSocket.configureBlocking(false);
        } catch (IOException e) {
            throw new CommonException("can't configure socket as non-blocking", e);
        }

        // apply idle configuration
        // Has to be final, as it's used in a inner class...
        final NioSession session = new NioSession(clientSocket, selectorPool.getSelectorLoop(),this.idGenerator);
        managedSessions.put(session.getSessionId(), session);
        
        // connect to a running server. We get an immediate result if
        // the socket is blocking, and either true or false if it's non blocking
        boolean connected;
        try {
            connected = clientSocket.connect(remoteAddress);
        } catch (IOException e) {
        	throw new CommonException("can't configure socket as non-blocking", e);
        }

        if (!connected) {
            // async connection, let's the connection complete in background, the selector loop will detect when the
            // connection is successful
            connectSelectorLoop.register(false, true, false, false, session, clientSocket, new RegistrationCallback() {
                @Override
                public void done(SelectionKey selectionKey) {
                    session.setSelectionKey(selectionKey);
                }
            });
        } else {
            // already connected (probably a loopback connection, or a blocking socket)
            // register for read
            this.readWriteSelectorPool.register(false, false, true, false, session, clientSocket, new RegistrationCallback() {
                @Override
                public void done(SelectionKey selectionKey) {
                    session.setSelectionKey(selectionKey);
                }
            });
            session.setConnected(true);
        }

        return session;
    }

    public synchronized void disconnect() throws IOException {
        LOG.info("Disconnecting sessions");
        for (IServerSession session : managedSessions.values()) {
            session.close(true);
        }
        fireServiceInactivated();
    }

	private void fireServiceInactivated() {
		
		
	}
	
	public int getConnectTimeoutMillis() {
        return connectTimeoutInMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutInMillis) {
        this.connectTimeoutInMillis = connectTimeoutInMillis;
   }
    
}