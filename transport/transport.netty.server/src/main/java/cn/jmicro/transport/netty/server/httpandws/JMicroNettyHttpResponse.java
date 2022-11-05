package cn.jmicro.transport.netty.server.httpandws;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import cn.jmicro.api.http.HttpResponse;
import cn.jmicro.common.Constants;
import cn.jmicro.transport.netty.server.NettyServerSession;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JMicroNettyHttpResponse implements HttpResponse {

	private FullHttpResponse r;
	private ChannelHandlerContext ctx;
	
	public JMicroNettyHttpResponse(FullHttpResponse r,ChannelHandlerContext ctx) {
		this.r = r;
		this.ctx = ctx;
	}
	
	@Override
	public void write(InputStream in,int len) {
		try {
			r.headers().set("content-Length",len);
			r.content().writeBytes(in, len);
			this.flush();
		} catch (IOException e) {
			log.error("Write stream: ",e);
		}
	}

	@Override
	public void setHeader(String key,Object value) {
		r.headers().set(key,value);
	}

	@Override
	public void write(byte[] content) {
		r.headers().set("content-Length",content.length);
		ByteBuf responseBuf = Unpooled.copiedBuffer(content);
		r.content().writeBytes(responseBuf);
		responseBuf.release();
		this.flush();
	}

	@Override
	public void write(String content) {
		try {
			write(content.getBytes(Constants.CHARSET));
		} catch (UnsupportedEncodingException e) {
			log.error(content,e);
		}
	}

	@Override
	public void write(ByteBuffer content) {
		r.headers().set("content-Length",content.limit());
		ByteBuf responseBuf = Unpooled.copiedBuffer(content);
		r.content().writeBytes(responseBuf);
		responseBuf.release();
		this.flush();
	}

	@Override
	public void flush() {
		ctx.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void setStatusCode(int statusCode) {
		HttpResponseStatus s = HttpResponseStatus.valueOf(statusCode);
		if(s != null) {
			r.setStatus(s);
		}
	}

	@Override
	public void redirect(int code,String url) {
		this.setStatusCode(code);
		HttpHeaders hs = r.headers();
		hs.set(HttpHeaderNames.LOCATION, url);
		//NettyServerSession.cors(hs);
		NettyHttpServerHandler.cors(hs);
		hs.set(HttpHeaderNames.CONTENT_LENGTH, r.content().readableBytes());
		flush();
	}
	
	
}
