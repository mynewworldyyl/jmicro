package org.jmicro.gateway;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.JMicro;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(side = Constants.SIDE_PROVIDER)
public class ApiRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiRequestMessageHandler.class);
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private IObjectFactory objFactory;
	
	@Override
	public Short type() {
		return Constants.MSG_TYPE_API_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		ApiRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				ApiRequest.class, msg.getProtocol());
		
		Object result = null;
		Object srv = JMicro.getObjectFactory().getServie(req.getServiceName(), 
				req.getNamespace(), req.getVersion());
		if(srv != null){
			Class<?>[] clazzes = null;
			if(req.getArgs() != null && req.getArgs().length > 0){
				clazzes = new Class<?>[req.getArgs().length];
				for(int index = 0; index < req.getArgs().length; index++){
					clazzes[index] = req.getArgs()[index].getClass();
				}
			} else {
				clazzes = new Class<?>[0];
			}
			try {
				Method m = srv.getClass().getMethod(req.getMethod(), clazzes);
				result = m.invoke(srv, req.getArgs());
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException e) {
				logger.error("",e);
			}
		}
		
		ApiResponse resp = new ApiResponse();
		resp.setResult(result);
		resp.setId(idGenerator.getLongId(ApiResponse.class));
		resp.setReqId(req.getReqId());
		resp.setMsg(msg);
		
		msg.setType((short)(msg.getType()+1));
		msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
		session.write(msg);
		
	}

}
