package cn.jmicro.example.test.codec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import cn.jmicro.api.codec.PrefixTypeEncoderDecoder;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.common.Constants;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestDecodeEncodeRpcRequest extends JMicroBaseTestCase {

	@Test
	public void testEncodeDecodeRpcRequest() throws IOException {

		RpcRequest req = new RpcRequest();
		req.setMethod("method");
		req.setServiceName("serviceName");
		req.setNamespace("namespace");
		req.setVersion("0.0.1");
		Map<String,String> arg1 = new HashMap<>();
		arg1.put("test", "");
		Object[] args = new Object[] {arg1};
		req.setArgs(args);
		req.setRequestId(22L);
		req.setTransport(Constants.TRANSPORT_NETTY);
		req.setImpl("fsafd");
		
		PrefixTypeEncoderDecoder ed = of.get(PrefixTypeEncoderDecoder.class);
		
		ByteBuffer buf = ed.encode(req);
		RpcRequest req0 = ed.decode(buf);
		System.out.println(req0);
		
		/*JDataOutput jo = new JDataOutput();
		req.encode(jo);
		
		JDataInput ji = new JDataInput(jo.getBuf());
		RpcRequest req1 = new RpcRequest();
		req1.decode(ji);*/
		
	}
	
	@Test
	public void testEncodeDecodeHashMap() throws IOException {

		Map<String,String> arg1 = new HashMap<>();
		arg1.put("test", "test22");
		
		PrefixTypeEncoderDecoder ed = of.get(PrefixTypeEncoderDecoder.class);
		
		Object[] arr = new Object[] {arg1,"test555"};
		
		ByteBuffer buf = ed.encode(arr);
		Object[] arr2 = ed.decode(buf);
		
		System.out.println(arr2);
		
	}
}
