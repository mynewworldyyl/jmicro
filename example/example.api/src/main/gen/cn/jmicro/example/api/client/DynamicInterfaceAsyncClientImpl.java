package cn.jmicro.example.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.String;
import java.lang.Void;

public class DynamicInterfaceAsyncClientImpl extends AbstractClientServiceProxyHolder implements DynamicInterfaceAsyncClient {
  public IPromise<Void> runAsync(String data) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "run", (java.lang.Object)(data));
  }

  public void run(String data) {
    this.proxyHolder.invoke("run", (java.lang.Object)(data));
  }
}
