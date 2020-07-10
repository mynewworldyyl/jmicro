package cn.jmicro.api.monitor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Void;

public class MonitorServerAsyncClientImpl extends AbstractClientServiceProxyHolder implements IMonitorServerAsyncClient {
  public IPromise<Void> submitAsync(MRpcItem[] items) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "submit", (java.lang.Object)(items));
  }

  public void submit(MRpcItem[] items) {
    this.proxyHolder.invoke("submit", (java.lang.Object)(items));
  }
}
