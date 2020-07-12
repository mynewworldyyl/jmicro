package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.String;

public class RpcBAsyncClientImpl extends AbstractClientServiceProxyHolder implements IRpcBAsyncClient {
  public IPromise<String> invokeRpcBAsync(String bargs) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "invokeRpcB", (java.lang.Object)(bargs));
  }

  public String invokeRpcB(String bargs) {
    return (java.lang.String) this.proxyHolder.invoke("invokeRpcB", (java.lang.Object)(bargs));
  }
}
