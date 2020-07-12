package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.String;
import java.lang.Void;

public class AsyncRpcCallbackAsyncClientImpl extends AbstractClientServiceProxyHolder implements IAsyncRpcCallbackAsyncClient {
  public IPromise<Void> callbackAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "callback", (java.lang.Object)(name));
  }

  public void callback(String name) {
    this.proxyHolder.invoke("callback", (java.lang.Object)(name));
  }
}
