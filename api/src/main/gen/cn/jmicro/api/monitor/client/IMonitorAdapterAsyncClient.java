package cn.jmicro.api.monitor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.IMonitorAdapter;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import java.lang.Void;

public interface IMonitorAdapterAsyncClient extends IMonitorAdapter {
  IPromise<MonitorInfo> infoAsync();

  IPromise<Void> enableMonitorAsync(boolean enable);

  IPromise<MonitorServerStatus> statusAsync();

  IPromise<Void> resetAsync();
}
