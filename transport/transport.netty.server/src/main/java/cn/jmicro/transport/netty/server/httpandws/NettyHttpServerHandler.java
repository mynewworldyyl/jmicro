package cn.jmicro.transport.netty.server.httpandws;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.http.IHttpRequestHandler;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.net.IMessageReceiver;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.transport.netty.server.NettyServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
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
import io.netty.handler.codec.http.HttpVersion;
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
	
	@Inject(required=false)
	private Map<String,IHttpRequestHandler> handlers = new HashMap<>();
	
	//普通的HTTP请求转化为RPC调用，相当于JMicro服务与普通HTTP请求之间一个桥梁，或适配器
	//正常只在API网关有效
	@Inject(required=false, value="srvDispatcher")
	private IHttpRequestHandler srvDispatcher;
	
	@Inject(required=false, value="fsd")
	private IHttpRequestHandler fsDispatcher;
	
	@Cfg(value = "/httpsEnable")
	private boolean httpsEnable = false;
	
	@Cfg(value="/nettyHttpPort",required=false,defGlobal=false)
	private int port = 0;
	
	@Cfg(value="/exportHttpIP",required=false,defGlobal=false)
	private String host = "";
	
	//@Cfg("/NettyHttpServerHandler/staticFileMatchPattern")
	private String staticFileMatchPattern="^.+\\.(js|css|html|htm|jpg|png|jpeg|ico|icon|svg)$";
	
	private Pattern staticFilePattern = null;
	
	public void jready() {
		staticFilePattern = Pattern.compile(staticFileMatchPattern);
	}
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
    	if(this.openDebug) {
    		logger.debug("channelRead:" + msg.toString());
    	}
    	//logger.debug("channelRead:" + msg.toString());
    	if(!(msg instanceof FullHttpRequest)){
    		logger.warn("Buffer: "+msg);
    		/*if(msg instanceof TextWebSocketFrame) {
    			TextWebSocketFrame txt = (TextWebSocketFrame)msg;
    			logger.warn("Buffer text: " + txt.text());
    		}*/
    		ctx.fireChannelRead(msg);
    		return;
    	}

		String path = null;
		FullHttpRequest req = (FullHttpRequest)msg;
		try {
			
			if("options".equalsIgnoreCase(req.method().name())) {
				FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
				NettyHttpServerHandler.cors(response.headers());
				ctx.writeAndFlush(response);
				return;
			}
			
			NettyServerSession session = ctx.attr(sessionKey).get();
			if(httpsEnable) {
				if(session.getLocalAddress().getPort() == 80) {
					//httpt重定向到https
					doRedirect2Https(session,ctx,req);
					return;
				}
			}
			
			path = req.uri();

			if("/favicon.ico".equals(path) || 
				req.method().equals(HttpMethod.GET) 
				&& path != null 
				&& path.startsWith(Constants.HTTP_statis)) {
				//静态资源
				resourceHandler.handle(ctx, req);
				return;
			}
			
			if(req.method().equals(HttpMethod.POST) && path != null 
				&& path.startsWith(Constants.HTTP_httpContext)){
				//源生http RPC
				ByteBuf bb = req.content();
				byte[] bts = new byte[bb.readableBytes()];
				bb.readBytes(bts);
				session.receive(ByteBuffer.wrap(bts));
				return;
			}
			
			if(staticFilePattern.matcher(req.uri()).matches()) {
				//资源未找到
				//剩下的交由静态资源处理
				resourceHandler.handle(ctx, req);
				return;
			}
			
			if(this.handleHttpToRpc(session,ctx, req)) {
				return;
			}
		} catch (Throwable e) {
			logger.error(path,e);
			//this.responseText(ctx, "System error");
			//return;
		}
		
		//资源未找到
		//剩下的交由静态资源处理
		resourceHandler.handle(ctx, req);
	
    }
    
    public static void cors(HttpHeaders hs) {
    	//String vary = "Vary";
		//HttpHeaders hs = response.headers(); itoken
		hs.add("Origin","*");
		hs.add("Access-Control-Request-Method","POST,GET");
		hs.add("Access-Control-Request-Headers","*");
		hs.add("Access-Control-Allow-Headers","*");
		hs.add("Access-Control-Allow-Origin","*");
		hs.add("Access-Control-Allow-Method","POST,GET");
    }
    
    public boolean handleHttpToRpc(NettyServerSession session,
    		ChannelHandlerContext ctx,FullHttpRequest req) throws IOException {
    	
    	String path = req.uri();
		if(path.contains("?")) {
			path = path.substring(0,path.indexOf("?"));
		}
		
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		NettyHttpServerHandler.cors(response.headers());
		
		JMicroNettyHttpRequest rr = new JMicroNettyHttpRequest(req,session);
		if(!rr.isSuccess()) {
			this.responseText(ctx, rr.getRetMsg());
			return true;
		}
		
		JMicroNettyHttpResponse resp = new JMicroNettyHttpResponse(response,ctx);
		String key = req.headers().get(IHttpRequestHandler.HANDLER_KEY);
		if(key == null) {
			key = rr.getReqParam(IHttpRequestHandler.HANDLER_KEY);
		}
		
		if(key != null && handlers.containsKey(key)) {
			//基于头部KEY匹配
			handlers.get(key).handle(rr,resp);
			//ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		}
		
		if(req.method().equals(HttpMethod.GET) && path != null && path.startsWith(Constants.HTTP_fsContext)) {
			//静态资源,文件下载
			fsDispatcher.handle(rr, resp);
			return true;
		}
		
		return srvDispatcher.handle(rr, resp);
	}
    
    private void responseText(ChannelHandlerContext ctx,String text) {
    	FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.headers().set("Content-Length",text.length());
		response.content().writeCharSequence(text,Charset.forName(Constants.CHARSET));
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    //httpt重定向到https
	private void doRedirect2Https(NettyServerSession session,ChannelHandlerContext ctx, FullHttpRequest req) {
		FullHttpResponse response =  new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.FOUND);
		HttpHeaders hs = response.headers();
		String url = req.uri();
		hs.set(HttpHeaderNames.LOCATION, "https://"+this.host+":"+this.port+url);
		NettyHttpServerHandler.cors(response.headers());
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
