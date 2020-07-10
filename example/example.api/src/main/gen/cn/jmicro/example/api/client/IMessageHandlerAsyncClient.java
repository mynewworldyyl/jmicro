package cn.jmicro.example.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.net.Message;
import cn.jmicro.example.api.IMessageHandler;
import java.lang.Void;

public interface IMessageHandlerAsyncClient extends IMessageHandler {
  IPromise<Void> onMessageAsync(Message msg);
}
