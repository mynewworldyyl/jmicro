package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.IStatisMonitor;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public interface IStatisMonitorAsyncClient extends IStatisMonitor {
  IPromise<Boolean> startStatisAsync(String mkey, Integer t);

  IPromise<Boolean> stopStatisAsync(String mkey, Integer t);

  IPromise<Map> index2LabelAsync();
}
