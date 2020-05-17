package org.jmicro.transport.nio.handler;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.server.IRequestHandler;
import org.jmicro.api.server.IServerSession;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.servicemanager.JmicroManager;
import org.jmicro.common.Constants;

@Component
public class JMicroIoHandler implements IIoHandler{

	@Inject(value=Constants.DEFAULT_CODEC_FACTORY,required=true)
	private ICodecFactory codecFactory;
	
	@Inject(value=Constants.DEFAULT_HANDLER,required=true)
	private IRequestHandler h = null;
	
	private Executor exe = Executors.newFixedThreadPool(10);
	
	@Override
	public void handleRequestMessage(IServerSession session,Message msg) {
		JMicroContext cxt = JMicroContext.get();
		cxt.getParam(JMicroContext.SESSION_KEY, session);
		RpcRequest req = codecFactory.getDecoder().decode(msg.getPayload());
		req.setRequestId(msg.getMsgId());
		req.setSession(session);
		h.onRequest(req);
	}

	@Override
	public void handleResponseMessage(IServerSession session, Message msg) {
		
		
	}

}
