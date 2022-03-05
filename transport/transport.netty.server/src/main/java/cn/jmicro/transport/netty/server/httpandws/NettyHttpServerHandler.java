package cn.jmicro.transport.netty.server.httpandws;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.transport.netty.server.NettyServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;

@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettyHttpServerHandler extends ChannelInboundHandlerAdapter {
	
    static final Logger logger = LoggerFactory.getLogger(NettyHttpServerHandler.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.IO_SESSION_KEY+"NettyHttp"+TimeUtils.getCurTime());
	
	@Cfg("/NettyHttpServerHandler/readBufferSize")
	private int readBufferSize=1024*4;
	
	@Cfg("/NettyHttpServerHandler/heardbeatInterval")
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
	
	@Cfg(value = "/httpsEnable")
	private boolean httpsEnable = false;
	
	@Cfg(value="/nettyHttpPort",required=false,defGlobal=false)
	private int port = 0;
	
	@Cfg(value="/exportHttpIP",required=false,defGlobal=false)
	private String host = "";
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
    	if(this.openDebug) {
    		logger.debug("channelRead:" + msg.toString());
    	}
    	//logger.debug("channelRead:" + msg.toString());
    	
    	NettyServerSession session = ctx.attr(sessionKey).get();
    	
    	if(msg instanceof FullHttpRequest){
    		FullHttpRequest req = (FullHttpRequest)msg;
    		if(httpsEnable) {
        		if(session.getLocalAddress().getPort() == 80) {
        			//httpt重定向到https
        			doRedirect2Https(session,ctx,req);
        			return;
        		}
    		}
    		
    		//cors(req,session);
    		if(resourceHandler.canhandle(req)){
    			//全部GET请求转到资源控制器上面
    			resourceHandler.handle(ctx, req);
    		} else if(req.method().equals(HttpMethod.POST)){
    			//全部POST请求转到RPC控制器上面,因为RPC只能用POST请求
    	    	
    			ByteBuf bb = req.content();
    			byte[] bts = new byte[bb.readableBytes()];
    			bb.readBytes(bts);
    			
    			session.receive(ByteBuffer.wrap(bts));
    			
    			//String encodeType = req.headers().get(Constants.HTTP_HEADER_ENCODER);
    			
    			/*Message message = Message.decode(new JDataInput(ByteBuffer.wrap(bts)));
				JMicroContext.configProvider(session,message);
				receiver.receive(session,message);*/
				
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
    		logger.warn("Buffer: "+msg);
    		/*if(msg instanceof TextWebSocketFrame) {
    			TextWebSocketFrame txt = (TextWebSocketFrame)msg;
    			logger.warn("Buffer text: " + txt.text());
    		}*/
    		ctx.fireChannelRead(msg);
    	}
    }
    
	private void doRedirect2Https(NettyServerSession session,ChannelHandlerContext ctx, FullHttpRequest req) {
		FullHttpResponse response =  new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FOUND);
		HttpHeaders hs = response.headers();
		String url = req.uri();
		hs.set(HttpHeaderNames.LOCATION, "https://"+this.host+":"+this.port+url);
		NettyServerSession.cors(hs);
		hs.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		ctx.writeAndFlush(response);
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
    	
    	session.setReceiver(receiver);
    	session.setDumpDownStream(false);
    	session.setDumpUpStream(false);
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
