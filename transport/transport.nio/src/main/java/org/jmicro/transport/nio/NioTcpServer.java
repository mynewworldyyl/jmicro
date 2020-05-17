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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.exception.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a TCP NIO based server.
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
@Component
public class NioTcpServer implements SelectorListener {
    /** A logger for this class */
    static final Logger LOG = LoggerFactory.getLogger(NioTcpServer.class);

    /** the bound local address */
    private InetSocketAddress address = null;

    // the key used for selecting accept event
    private SelectionKey acceptKey = null;

    // the server socket for accepting clients
    private ServerSocketChannel serverChannel = null;

    private  SelectorLoop acceptSelectorLoop = new NioSelectorLoop("NioTcpServerLoop",1);
    
    // does the reuse address flag should be positioned
    private boolean reuseAddress = false;

    @Inject(required=true)
    private SessionManager sessionManager;

    private ServiceState state;
    
    /**
     * The Service states
     */
    protected enum ServiceState {
        /** Initial state */
        NONE,
        /** The service has been created */
        CREATED,
        /** The service is started */
        ACTIVE,
        /** The service has been suspended */
        SUSPENDED,
        /** The service is being stopped */
        DISPOSING,
        /** The service is stopped */
        DISPOSED
    }


    public NioTcpServer() {}

    @Override
    public void ready(final boolean accept, boolean connect, final boolean read, final ByteBuffer readBuffer,
            final boolean write) {
        if (accept) {
            LOG.debug("acceptable new client");

            // accepted connection
            try {
                LOG.debug("new client accepted");
                sessionManager.createSession(getServerSocketChannel().accept());
            } catch (final IOException e) {
                LOG.error("error while accepting new client", e);
            }
        }

        if (read || write) {
            throw new IllegalStateException("should not receive read or write events");
        }
    }
    
    public void bind(final int port) {
        bind(new InetSocketAddress(port));
    }

    public synchronized void bind(SocketAddress localAddress) {
        if(localAddress == null) {
        	throw new CommonException("Bind Address is NULL");
        }

        // check if the address is already bound
        if (address != null) {
            throw new IllegalStateException("address " + address + " already bound");
        }

        LOG.info("binding address {}", localAddress);

        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().setReuseAddress(isReuseAddress());
            serverChannel.socket().bind(localAddress);
            serverChannel.configureBlocking(false);
            this.address = (InetSocketAddress)serverChannel.getLocalAddress();
            //this.address = serverChannel.getLocalAddress();
           
        } catch (IOException e) {
            throw new CommonException("can't bind address" + address, e);
        }

        acceptSelectorLoop.register(true, false, false, false, this, serverChannel, null);

    }

    public synchronized void unbind() {
        LOG.info("unbinding {}", address);
        if (this.address == null) {
            throw new IllegalStateException("server not bound");
        }
        try {
            serverChannel.socket().close();
            serverChannel.close();
        } catch (IOException e) {
            throw new CommonException("can't unbind server", e);
        }

        acceptSelectorLoop.unregister(this, serverChannel);

        this.address = null;

    }

    public SocketAddress getBoundAddress() {
        return address;
    }
    
    /**
     * @return true if the IoService is active
     */
    public boolean isActive() {
        return this.state == ServiceState.ACTIVE;
    }

    /**
     * @return true if the IoService is being disposed
     */
    public boolean isDisposing() {
        return this.state == ServiceState.DISPOSING;
    }

    /**
     * @return true if the IoService is disposed
     */
    public boolean isDisposed() {
        return this.state == ServiceState.DISPOSED;
    }

    /**
     * @return true if the IoService is suspended
     */
    public boolean isSuspended() {
        return this.state == ServiceState.SUSPENDED;
    }

    /**
     * @return true if the IoService is created
     */
    public boolean isCreated() {
        return this.state == ServiceState.CREATED;
    }

    /**
     * Sets the IoService state to CREATED.
     */
    protected void setCreated() {
        this.state = ServiceState.CREATED;
    }

    /**
     * Sets the IoService state to ACTIVE.
     */
    protected void setActive() {
        this.state = ServiceState.ACTIVE;
    }

    /**
     * Sets the IoService state to DISPOSED.
     */
    protected void setDisposed() {
        this.state = ServiceState.DISPOSED;
    }

    /**
     * Sets the IoService state to DISPOSING.
     */
    protected void setDisposing() {
        this.state = ServiceState.DISPOSING;
    }

    /**
     * Sets the IoService state to SUSPENDED.
     */
    protected void setSuspended() {
        this.state = ServiceState.SUSPENDED;
    }

    /**
     * Initialize the IoService state
     */
    protected void initState() {
        this.state = ServiceState.NONE;
    }
    
    /**
     * Set the reuse address flag on the server socket
     * 
     * @param reuseAddress <code>true</code> to enable
     */
    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    /**
     * Is the reuse address enabled for this server.
     * 
     * @return
     */
    public boolean isReuseAddress() {
        return this.reuseAddress;
    }
    
    /**
     * Get the inner Server socket for accepting new client connections
     * 
     * @return
     */
    public synchronized ServerSocketChannel getServerSocketChannel() {
        return serverChannel;
    }

    public synchronized void setServerSocketChannel(final ServerSocketChannel serverChannel) {
        this.serverChannel = serverChannel;
    }
    
    /**
     * @return the acceptKey
     */
    public SelectionKey getAcceptKey() {
        return acceptKey;
    }

    /**
     * @param acceptKey the acceptKey to set
     */
    public void setAcceptKey(final SelectionKey acceptKey) {
        this.acceptKey = acceptKey;
    }

	public void setAcceptSelectorLoop(SelectorLoop acceptSelectorLoop) {
		this.acceptSelectorLoop = acceptSelectorLoop;
	}


	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}


	public InetSocketAddress getAddress() {
		return address;
	}


	public void setAddress(InetSocketAddress address) {
		this.address = address;
	}


	public ServerSocketChannel getServerChannel() {
		return serverChannel;
	}


	public void setServerChannel(ServerSocketChannel serverChannel) {
		this.serverChannel = serverChannel;
	}


	public ServiceState getState() {
		return state;
	}


	public void setState(ServiceState state) {
		this.state = state;
	}

}