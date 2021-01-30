package cn.expjmicro.example.api;

import cn.jmicro.api.net.Message;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IMessageHandler {

	public void onMessage(Message msg);
}
