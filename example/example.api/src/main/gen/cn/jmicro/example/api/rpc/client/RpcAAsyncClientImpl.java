package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.String;

public class RpcAAsyncClientImpl extends AbstractClientServiceProxyHolder implements IRpcAAsyncClient {
  public IPromise<String> invokeRpcAAsync(String aargs) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "invokeRpcA", aargs);
  }

  public String invokeRpcA(String aargs) {
    return (java.lang.String) this.proxyHolder.invoke("invokeRpcA", aargs);
  }
}
