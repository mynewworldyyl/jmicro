package cn.jmicro.api.monitor.genclient;

import java.util.Set;

import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.IOutterMonitorService;
import cn.jmicro.api.monitor.OneLogJRso;

public interface IOutterMonitorService$Gateway$JMAsyncClient extends IOutterMonitorService {
  @WithContext
  IPromise<Void> submitJMAsync(Set<OneLogJRso> logs, Object context);

  IPromise<Void> submitJMAsync(Set<OneLogJRso> logs);
}
