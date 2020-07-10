package cn.jmicro.api.monitor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Void;

public class MonitorAdapterAsyncClientImpl extends AbstractClientServiceProxyHolder implements IMonitorAdapterAsyncClient {
  public IPromise<MonitorInfo> infoAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "info");
  }

  public MonitorInfo info() {
    return (cn.jmicro.api.monitor.MonitorInfo) this.proxyHolder.invoke("info");
  }

  public IPromise<Void> enableMonitorAsync(boolean enable) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "enableMonitor", (java.lang.Object)(enable));
  }

  public void enableMonitor(boolean enable) {
    this.proxyHolder.invoke("enableMonitor", (java.lang.Object)(enable));
  }

  public IPromise<MonitorServerStatus> statusAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "status");
  }

  public MonitorServerStatus status() {
    return (cn.jmicro.api.monitor.MonitorServerStatus) this.proxyHolder.invoke("status");
  }

  public IPromise<Void> resetAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "reset");
  }

  public void reset() {
    this.proxyHolder.invoke("reset");
  }
}
