package org.jmicro.example.test;

import java.nio.ByteBuffer;

import org.jmicro.api.JMicro;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.codec.IDecoder;
import org.jmicro.api.codec.IEncoder;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.junit.Test;

public class TestCodec {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testEncodeDecode() {
		IObjectFactory of = JMicro.getObjectFactoryAndStart(new String[]{"-DinstanceName=TestCodec"});
		ICodecFactory codeFactory = of.get(ICodecFactory.class);
		
		IEncoder encoder = codeFactory.getEncoder(Message.PROTOCOL_BIN);
		
		RpcResponse resp = new RpcResponse(1,new Integer[]{1,2,3});
		resp.setId(33l);
		resp.setMonitorEnable(true);
		resp.setSuccess(true);
		resp.getParams().put("key01", 3);
		resp.getParams().put("key02","hello");
		resp.setMsg(new Message());
		
		ByteBuffer bb = (ByteBuffer) encoder.encode(resp);
		
		IDecoder decoder = codeFactory.getDecoder(Message.PROTOCOL_BIN);
		resp = (RpcResponse)decoder.decode(bb, RpcResponse.class);
		System.out.println(resp);
	}
}
