package cn.jmicro.api.monitor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.IMonitorServer;
import cn.jmicro.api.monitor.MRpcItem;
import java.lang.Void;

public interface IMonitorServerAsyncClient extends IMonitorServer {
  IPromise<Void> submitAsync(MRpcItem[] items);
}
