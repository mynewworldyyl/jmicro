package org.jmicro.transport.netty.httpandws;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.transport.netty.NettyServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.AttributeKey;

@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
	
    static final Logger logger = LoggerFactory.getLogger(NettyServerSession.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.SESSION_KEY+System.currentTimeMillis());
	
	@Cfg("/MinaServer/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Inject
	private StaticResourceHttpHandler resourceHandler;
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
    	//logger.debug("channelRead:" + msg.toString());
    	if(msg instanceof FullHttpRequest){
    		FullHttpRequest req = (FullHttpRequest)msg;
    		if(resourceHandler.canhandle(req)){
    			resourceHandler.handle(ctx, req);
    		} else {
    			NettyServerSession session = ctx.attr(sessionKey).get();
    			ByteBuf bb = req.content();
    			byte[] bts = new byte[bb.readableBytes()];
    			bb.readBytes(bts);
    			String result = new String(bts,Constants.CHARSET);
    			Message message = JsonUtils.getIns().fromJson(result, Message.class);
    			receiver.receive(session,message);
    		}
    	}else {
    		logger.debug("Error Http Request!");
    	}
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	super.handlerAdded(ctx);
    	NettyServerSession session = new NettyServerSession(ctx,readBufferSize,heardbeatInterval);
    	ctx.channel().attr(sessionKey).set(session);
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	super.handlerRemoved(ctx);
    	ctx.channel().attr(sessionKey).remove();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	logger.error("exceptionCaught",cause);
    	ctx.channel().attr(sessionKey).remove();
        ctx.channel().attr(sessionKey).get().close(true);
    }
    
}
