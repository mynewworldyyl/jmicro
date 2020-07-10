package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.String;

public class ThreadPoolMonitorAsyncClientImpl extends AbstractClientServiceProxyHolder implements IThreadPoolMonitorAsyncClient {
  public IPromise<Resp> serverListAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "serverList");
  }

  public Resp serverList() {
    return (cn.jmicro.api.Resp<java.util.List<cn.jmicro.api.executor.ExecutorInfo>>) this.proxyHolder.invoke("serverList");
  }

  public IPromise<Resp> getInfoAsync(String key, String type) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getInfo", key,type);
  }

  public Resp getInfo(String key, String type) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("getInfo", key,type);
  }
}
