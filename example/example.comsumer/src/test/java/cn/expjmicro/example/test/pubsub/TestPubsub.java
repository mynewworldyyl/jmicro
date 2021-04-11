package cn.expjmicro.example.test.pubsub;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import cn.expjmicro.example.comsumer.TestRpcClient;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.codec.PrefixTypeEncoderDecoder;
import cn.jmicro.api.mng.ConfigNode;
import cn.jmicro.api.mng.IConfigManager;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.registry.AsyncConfig;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestPubsub extends JMicroBaseTestCase{
	
	@Test
	public void testAsyncCallRpc() {
		SubmitItem si = new SubmitItem();
		of.get(TestRpcClient.class).testCallAsyncRpc();
		Utils.getIns().waitForShutdown();
	}
	
	@Test
	public void testEncodePSData() throws IOException {
		cn.jmicro.api.pubsub.PSData psd = new cn.jmicro.api.pubsub.PSData();
		AsyncConfig as = new AsyncConfig();
		as.setCondition("222");
		as.setEnable(true);
		as.setForMethod("hello");
		as.setMethod("callback");
		as.setNamespace("ts");
		as.setParamStr(null);
		as.setServiceName("testas");
		as.setVersion("222");
		psd.put("asc", as);
		
		psd.put("linkId", 222L);
		psd.put("key", "ssss");
		
		Object obj = psd;
		cn.jmicro.api.codec.ISerializeObject so = (cn.jmicro.api.codec.ISerializeObject)obj;
		JDataOutput out = new JDataOutput();
		so.encode(out);
		
		JDataInput ji = new JDataInput(out.getBuf());
		so.decode(ji);
		
		System.out.println(so);
		
	}
	
	@Test
	public void testEncodePSData1() throws IOException {
		PSData0 psd = new PSData0();
		AsyncConfig as = new AsyncConfig();
		psd.put("asc", as);
		
		psd.put("linkId", 222L);
		//psd.put("key", "ssss");
		
		JDataOutput out = new JDataOutput();
		psd.encode(out);
		
		PSData0 decodePsd = new PSData0();
		
		JDataInput ji = new JDataInput(out.getBuf());
		decodePsd.decode(ji);
		
		System.out.println(decodePsd);
	}
	
	@Test
	public void testEncodePSDatas() throws IOException {
		
		ICodecFactory ed = of.get(ICodecFactory.class);
		
		cn.jmicro.api.pubsub.PSData psd = new cn.jmicro.api.pubsub.PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setId(0);
		psd.setTopic("/test/testtopic");
		
		//psd.getContext().put("key", 222);
		
		RpcRequest0 req = new RpcRequest0();
		req.setMethod("method");
		req.setServiceName("serviceName");
		req.setNamespace("namespace");
		req.setVersion("0.0.1");
		Object[] args = new Object[] {"23fsaf",new Object[] {psd,psd}};
		req.setArgs(args);
		req.setRequestId(22L);
		req.setTransport(Constants.TRANSPORT_NETTY);
		req.setImpl(66);
		
		req.putObject(JMicroContext.LOGIN_KEY, "testlogin012");
		
		//Message msg = new Message();
		ByteBuffer bb = ICodecFactory.encode(ed, req, Message.PROTOCOL_BIN);
		RpcRequest0 obj = ICodecFactory.decode(ed, bb, null, Message.PROTOCOL_BIN);
		//ByteBuffer bb = (ByteBuffer)ed.getEncoder(Message.PROTOCOL_BIN).encode(req);
		
		//RpcRequest obj = (RpcRequest)ed.getDecoder(Message.PROTOCOL_BIN).decode(bb, null);
		
		/*msg.setPayload(bb);
		ByteBuffer msgBb = msg.encode();
		
		Message respMsg = Message.readMessage(msgBb);
		
		Object obj = ed.decode((ByteBuffer)respMsg.getPayload());*/
		
		System.out.println(obj);
	}
	
	@Test
	public void testEncodePSDataWithMapAsArgs() throws IOException {
		
		PrefixTypeEncoderDecoder ed = of.get(PrefixTypeEncoderDecoder.class);
		
		Map<Short,Object> data = new HashMap<>();
		data.put(MC.MT_CLIENT_CONNECT_FAIL, 222D);
		data.put(MC.MT_CLIENT_IOSESSION_CLOSE, new ServiceMethod());
		//data.values().iterator()
		//cn.jmicro.api.pubsub.PSData psData = new cn.jmicro.api.pubsub.PSData();
		
		PSData0 psData = new PSData0();
		psData.put(Constants.SERVICE_METHOD_KEY, new ServiceMethod());
		psData.put(Constants.SERVICE_NAME_KEY, 2222);
		psData.setData(data);
		
		ByteBuffer buffer = ed.encode(psData);
		
		PSData0 r = ed.decode(buffer);
		
		System.out.println(r);
	}
	
	
	@Test
	public void testEncodeRequestWithPSData() throws IOException {
		
		cn.jmicro.api.pubsub.PSData psd = new cn.jmicro.api.pubsub.PSData();
		psd.setData(new byte[] {22,33,33});
		psd.setId(0);
		psd.setTopic("/test/testtopic");
		
		//psd.getContext().put("key", 222);
		
		RpcRequest0 req = new RpcRequest0();
		req.setMethod("method");
		req.setServiceName("serviceName");
		req.setNamespace("namespace");
		req.setVersion("0.0.1");
		Object[] args = new Object[] {"23fsaf",new Object[] {psd,psd}};
		req.setArgs(args);
		req.setRequestId(22L);
		req.setTransport(Constants.TRANSPORT_NETTY);
		req.setImpl(88);
		
		JDataOutput jo = new JDataOutput();
		
		Object obj = req;
		
		//((ISerializeObject)obj).encode(jo, obj);
		req.encode(jo);
		
		
		JDataInput ji = new JDataInput(jo.getBuf());
		RpcRequest0 r1 = new RpcRequest0();
		r1.decode(ji);
		
		System.out.print(r1);
	}
	
	
	@Test
	public void testEncodeConfigNode() throws IOException {
		ICodecFactory ed = of.get(ICodecFactory.class);
		
		ConfigNode cn = new ConfigNode("/feaf/fda","22","dad");
		ConfigNode[] c = new ConfigNode[] {
				new ConfigNode("/feaf/fda","22","dad"),new ConfigNode("/feaf/fda","22","dad"),
				new ConfigNode("/feaf/fda","22","dad"),new ConfigNode("/feaf/fda","22","dad")
		};
		cn.setChildren(c);
		
		ConfigNode cn1 = new ConfigNode("/feaf/fda","22","dad");
		ConfigNode[] c1 = new ConfigNode[] {
				new ConfigNode("/feaf/fda","22","dad"),new ConfigNode("/feaf/fda","22","dad"),
				new ConfigNode("/feaf/fda","22","dad"),new ConfigNode("/feaf/fda","22","dad")
		};
		cn1.setChildren(c1);
		
		ByteBuffer bb = (ByteBuffer)ed.getEncoder(Message.PROTOCOL_BIN).encode(new ConfigNode[] {cn,cn1});
		
		ConfigNode[] obj = (ConfigNode[])ed.getDecoder(Message.PROTOCOL_BIN).decode(bb, null);
		
		System.out.print(obj);
		
	}
	
	@Test
	public void testEncodeConfigNode0() throws IOException {
		ICodecFactory ed = of.get(ICodecFactory.class);
		IConfigManager cm = of.get(IConfigManager.class);
		Resp<ConfigNode[]> nodes = cm.getChildren("/", true);
		
		ByteBuffer bb = (ByteBuffer)ed.getEncoder(Message.PROTOCOL_BIN).encode(nodes);
		
		ConfigNode[] objs = (ConfigNode[])ed.getDecoder(Message.PROTOCOL_BIN).decode(bb, null);
		
		System.out.print(objs);
		
	}
	
	@Test
	public void testGetConfigService() throws IOException {
		/*ByteBuffer buf = ByteBuffer.allocate(100);
		Message.writeUnsignedShort(buf, 65535);
		
		buf.flip();
		
		int val = Message.readUnsignedShort(buf);*/
		
		IConfigManager cm = of.getRemoteServie(IConfigManager.class.getName(),"configManager","0.0.1", null);
		Resp<ConfigNode[]> nodes = cm.getChildren("/", true);
		System.out.print(nodes);
		this.waitForReady(60*60);
	}
	
	@Test
	public void testReportDataEncodeDecode() throws IOException {
		
		ReportData0 rd = new ReportData0();
		rd.setTypes(new Short[] {1,2,3});
		rd.setLabels(new String[] {"","",""});
		rd.setDatas(new Double[] {null,0D,0D});
		
		JDataOutput jo = new JDataOutput();
		rd.encode(jo, rd);
		
		ReportData0 deData = new ReportData0();
		deData.decode(new JDataInput(jo.getBuf()));
		
		System.out.println(deData);
	}
	
	@Test
	public void testPubsubReportDataEncodeDecode() throws IOException {
		
		PrefixTypeEncoderDecoder ed = of.get(PrefixTypeEncoderDecoder.class);
		
		Map<Short,Object> data = new HashMap<>();
		data.put(MC.MT_CLIENT_CONNECT_FAIL, 222D);
		data.put(MC.MT_CLIENT_IOSESSION_CLOSE, new ServiceMethod());
		//data.values().iterator()
		//org.jmicro.api.pubsub.PSData psData = new org.jmicro.api.pubsub.PSData();
		
		ReportData rd = new ReportData();
		rd.setTypes(new Short[] {1,2,3});
		rd.setLabels(new String[] {null,"",""});
		
		Double[] datas = new Double[] {0D,null,0D};
		rd.setQps(datas);
		
		PSData psData = new PSData();
		psData.setData(rd);
		psData.setTopic("");
		psData.put(Constants.SERVICE_METHOD_KEY, "");
		
		//ByteBuffer bb = ByteBuffer.allocate(1024);
		
		/*JDataOutput jo = new JDataOutput();
		psData.encode(jo, psData);
		
		PSData deData = new PSData();
		deData.decode(new JDataInput(jo.getBuf()));*/
		
		ByteBuffer buffer = ed.encode(psData);
		PSData deData = ed.decode(buffer);
		
		System.out.println(deData);
	}
	
	
	@Test
	public void testMonitorServerStatusEncodeDecode() throws IOException {
		
		MonitorServerStatus totalStatus = new MonitorServerStatus();
		totalStatus.setCur(new double[10]);
		
		Set<Short> types = new HashSet<>();
		types.add((short)222);
		//totalStatus.getSubsriber2Types().put("sss", types);
		
		ICodecFactory ed = of.get(ICodecFactory.class);
		
		ByteBuffer bb = (ByteBuffer)ed.getEncoder(Message.PROTOCOL_BIN).encode(totalStatus);
		
		MonitorServerStatus objs = (MonitorServerStatus)ed.getDecoder(Message.PROTOCOL_BIN).decode(bb, null);
		
		System.out.println(objs);
	}
	
	@Test
	public void testMonitorServerStatusEncodeDecode0() throws IOException {
		
		MonitorServerStatus0 s = new MonitorServerStatus0();
		//s.setTotal(new double[] {2222D,2222D,2222D,2222D,2222D,2222D,});
		//s.setCur(new double[] {2222D,2222D,2222D,2222D,2222D,2222D,});
		
		JDataOutput jo = new JDataOutput(1024);
		s.encode(jo);
		
		MonitorServerStatus0 objs = new MonitorServerStatus0();
		objs.decode(new JDataInput(jo.getBuf()));
		
		System.out.println(objs);
	}
	
}
