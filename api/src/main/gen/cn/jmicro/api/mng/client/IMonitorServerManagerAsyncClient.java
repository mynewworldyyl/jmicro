package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.IMonitorServerManager;
import cn.jmicro.api.monitor.MonitorInfo;
import cn.jmicro.api.monitor.MonitorServerStatus;
import java.lang.Boolean;
import java.lang.String;
import java.lang.Void;

public interface IMonitorServerManagerAsyncClient extends IMonitorServerManager {
  IPromise<MonitorServerStatus[]> statusAsync(String[] srvKeys);

  IPromise<Boolean> enableAsync(String srvKey, Boolean enable);

  IPromise<MonitorInfo[]> serverListAsync();

  IPromise<Void> resetAsync(String[] srvKeys);
}
