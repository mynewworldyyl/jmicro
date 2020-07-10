package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.IThreadPoolMonitor;
import java.lang.String;

public interface IThreadPoolMonitorAsyncClient extends IThreadPoolMonitor {
  IPromise<Resp> serverListAsync();

  IPromise<Resp> getInfoAsync(String key, String type);
}
