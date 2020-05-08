package org.jmicro.transport.netty.server.httpandws;

import java.nio.ByteBuffer;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.JDataInput;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.transport.netty.server.NettyServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.AttributeKey;

@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
	
    static final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_SESSION_KEY+"NettyHttp"+System.currentTimeMillis());
	
	@Cfg("/MinaServer/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Cfg("/NettyHttpServerHandler/openDebug")
	private boolean openDebug = false;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Inject
	private StaticResourceHttpHandler resourceHandler;
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
    	if(this.openDebug) {
    		logger.debug("channelRead:" + msg.toString());
    	}
    	//logger.debug("channelRead:" + msg.toString());
    	
    	if(msg instanceof FullHttpRequest){
    		FullHttpRequest req = (FullHttpRequest)msg;
    		if(resourceHandler.canhandle(req)){
    			//全部GET请求转到资源控制器上面
    			resourceHandler.handle(ctx, req);
    		} else if(req.method().equals(HttpMethod.POST)){
    			//全部POST请求转到RPC控制器上面,因为RPC只能用POST请求
    			NettyServerSession session = ctx.attr(sessionKey).get();
    	    	
    			ByteBuf bb = req.content();
    			byte[] bts = new byte[bb.readableBytes()];
    			bb.readBytes(bts);
    			//String encodeType = req.headers().get(Constants.HTTP_HEADER_ENCODER);
    			
    			Message message = Message.decode(new JDataInput(ByteBuffer.wrap(bts)));
				JMicroContext.configProvider(session,message);
				receiver.receive(session,message);
				
    			/*if(encodeType == null || encodeType.equals(Message.PROTOCOL_JSON+"")) {
    				String result = new String(bts,Constants.CHARSET);
        			message = JsonUtils.getIns().fromJson(result, Message.class);
        			JMicroContext.configProvider(session,message);
        			receiver.receive(session,message);
    			} else {
    				message = Message.decode(new JDataInput(ByteBuffer.wrap(bts)));
    				JMicroContext.configProvider(session,message);
    				receiver.receive(session,message);
    			}*/
    		}
    	}else {
    		logger.debug("Error Http Request!");
    		ctx.fireChannelRead(msg);
    	}
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	super.handlerAdded(ctx);
    	NettyServerSession session = new NettyServerSession(ctx,readBufferSize,heardbeatInterval,
    			Constants.TYPE_HTTP);
    	session.init();
    	ctx.channel().attr(sessionKey).set(session);
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	super.handlerRemoved(ctx);
    	ctx.channel().attr(sessionKey).remove();
    	ISession s = ctx.channel().attr(sessionKey).get();
    	if( s != null) {
    		s.close(true);
    	}
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	logger.error("exceptionCaught",cause);
    	ctx.channel().attr(sessionKey).remove();
    	ISession s = ctx.channel().attr(sessionKey).get();
    	if( s != null) {
    		s.close(true);
    	}
    }
    
}
