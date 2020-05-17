package cn.jmicro.example.api;

import cn.jmicro.api.net.Message;

public interface IMessageHandler {

	public void onMessage(Message msg);
}
