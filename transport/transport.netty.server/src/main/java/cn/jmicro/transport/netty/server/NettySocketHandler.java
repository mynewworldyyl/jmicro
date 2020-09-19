package cn.jmicro.transport.netty.server;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettySocketHandler extends ChannelInboundHandlerAdapter {
	
    static final Logger logger = LoggerFactory.getLogger(NettySocketHandler.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_SESSION_KEY+"Netty" + System.currentTimeMillis());
	
	@Cfg("/MinaServer/readBufferSize")
	private int readBufferSize = 1024*4;

	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Cfg(value="/NettySocketHandler/openDebug",required=false,defGlobal=false)
	private boolean openDebug=false;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Cfg(value="/NettySocketHandler/dumpDownStream",defGlobal=false)
	private boolean dumpDownStream  = false;
	
	@Cfg(value="/NettySocketHandler/dumpUpStream",defGlobal=false)
	private boolean dumpUpStream  = false;
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
    	
    	//logger.info("st:  "+System.currentTimeMillis());
		
    	if(openDebug) {
    		logger.debug("channelRead Data: {}",msg);
    	}
    	if(!(msg instanceof ByteBuf)) {
    		ctx.fireChannelRead(msg);
    		return;
    	}
    	
    	ByteBuf bb = (ByteBuf)msg;
    	if(bb.readableBytes() <= 0) {
    		return;
    	}
    	
    	//logger.info("ed0:  "+System.currentTimeMillis());
    	
    	//byte[] arr = bb.array();
    	
    	ByteBuffer b = ByteBuffer.allocate(bb.readableBytes());
    	bb.readBytes(b);
    	b.flip();
    	bb.release();
    	
    	NettyServerSession session = ctx.channel().attr(sessionKey).get();
    	
    	//logger.info("ed1:  "+System.currentTimeMillis());
    	
    	//logger.info("ed2:  "+System.currentTimeMillis());
    	
    	session.receive(b);
    	
		//logger.info("ed1:  "+System.currentTimeMillis());

    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        /*if(openDebug) {
    		logger.debug("channelReadComplete: {}",ctx);
    	}*/
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	super.handlerAdded(ctx);
    	if(openDebug) {
    		logger.debug("handlerAdded: {}",ctx);
    	}
    	NettyServerSession session = new NettyServerSession(ctx,readBufferSize,heardbeatInterval,Constants.TYPE_SOCKET);
    	session.setReceiver(receiver);
    	session.setDumpDownStream(this.dumpDownStream);
    	session.setDumpUpStream(this.dumpUpStream);
    	session.init();
    	ctx.channel().attr(sessionKey).set(session);
    	
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	super.handlerRemoved(ctx);
    	if(openDebug) {
    		logger.debug("handlerRemoved: {}",ctx);
    	}
    	ctx.channel().attr(sessionKey).remove();
    	
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	logger.error("exceptionCaught close session",cause.getMessage());
    	ctx.channel().attr(sessionKey).get().close(true);
    	ctx.channel().attr(sessionKey).remove();
    	ctx.close();
    }
    
}
