package cn.jmicro.example.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Void;

public class MessageHandlerAsyncClientImpl extends AbstractClientServiceProxyHolder implements IMessageHandlerAsyncClient {
  public IPromise<Void> onMessageAsync(Message msg) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "onMessage", (java.lang.Object)(msg));
  }

  public void onMessage(Message msg) {
    this.proxyHolder.invoke("onMessage", (java.lang.Object)(msg));
  }
}
