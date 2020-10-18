package cn.jmicro.example.test.monitor;

import java.nio.ByteBuffer;

import org.junit.Test;

import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcLogItem;
import cn.jmicro.api.monitor.MRpcStatisItem;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.net.RpcRequest;
import cn.jmicro.test.JMicroBaseTestCase;

public class TestMonitorPersist2Db extends JMicroBaseTestCase{
	
	@Test
	public void testMonitorSubmitJsonData() {
		IMonitorDataSubscriber mds = of.getRemoteServie(IMonitorDataSubscriber.class.getName(), 
				"log2DbMonitor", "0.0.1", null);
		
		MRpcStatisItem mi = new MRpcStatisItem();
		mi.addType(MC.EP_START, 1, 1);
		mds.onSubmit(new MRpcStatisItem[] {mi,mi});
		
		this.waitForReady(1000);
	}
	
	@Test
	public void testEncodeDecodeMrpcItemWithJsonString() {
		MRpcLogItem mi = new MRpcLogItem();
		mi.addOneItem(MC.LOG_DEBUG,TestMonitorPersist2Db.class.getName(),"{\"id\":\"255\",\"host\":\"192.168.56.1\",\"instanceName\":\"Monitor0\",\"agentHost\":\"192.168.56.1\",\"agentInstanceName\":null,\"depId\":null,\"agentId\":null,\"agentProcessId\":null,\"pid\":\"26468\",\"cmd\":null,\"workDir\":\"D:\\\\opensource\\\\github\\\\jmicro\\\\monitor\\\\monitor.common\\\\data\\\\Monitor0\",\"active\":true,\"opTime\":1592215403575,\"timeOut\":0,\"startTime\":1592215403575,\"haEnable\":true,\"master\":true}");
		
		RpcRequest req = new RpcRequest();
		req.setArgs(new Object[] {mi,mi});
		
		ICodecFactory codecFactory = of.get(ICodecFactory.class);
		
		ByteBuffer pl = ICodecFactory.encode(codecFactory, req,Message.PROTOCOL_BIN);
		
		RpcRequest rst = ICodecFactory.decode(codecFactory, pl, RpcRequest.class, Message.PROTOCOL_BIN);
				
		System.out.println(rst);
				
	}
	
}
