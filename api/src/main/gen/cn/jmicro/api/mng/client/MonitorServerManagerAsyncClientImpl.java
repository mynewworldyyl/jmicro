package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Boolean;
import java.lang.String;
import java.lang.Void;

public class MonitorServerManagerAsyncClientImpl extends AbstractClientServiceProxyHolder implements IMonitorServerManagerAsyncClient {
  public IPromise<MonitorServerStatus[]> statusAsync(String[] srvKeys) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "status", (java.lang.Object)(srvKeys));
  }

  public MonitorServerStatus[] status(String[] srvKeys) {
    return (cn.jmicro.api.monitor.MonitorServerStatus[]) this.proxyHolder.invoke("status", (java.lang.Object)(srvKeys));
  }

  public IPromise<Boolean> enableAsync(String srvKey, Boolean enable) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "enable", srvKey,enable);
  }

  public boolean enable(String srvKey, Boolean enable) {
    return (boolean) this.proxyHolder.invoke("enable", srvKey,enable);
  }

  public IPromise<MonitorInfo[]> serverListAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "serverList");
  }

  public MonitorInfo[] serverList() {
    return (cn.jmicro.api.monitor.MonitorInfo[]) this.proxyHolder.invoke("serverList");
  }

  public IPromise<Void> resetAsync(String[] srvKeys) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "reset", (java.lang.Object)(srvKeys));
  }

  public void reset(String[] srvKeys) {
    this.proxyHolder.invoke("reset", (java.lang.Object)(srvKeys));
  }
}
