package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class StatisMonitorAsyncClientImpl extends AbstractClientServiceProxyHolder implements IStatisMonitorAsyncClient {
  public IPromise<Boolean> startStatisAsync(String mkey, Integer t) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "startStatis", mkey,t);
  }

  public boolean startStatis(String mkey, Integer t) {
    return (java.lang.Boolean) this.proxyHolder.invoke("startStatis", mkey,t);
  }

  public IPromise<Boolean> stopStatisAsync(String mkey, Integer t) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "stopStatis", mkey,t);
  }

  public boolean stopStatis(String mkey, Integer t) {
    return (java.lang.Boolean) this.proxyHolder.invoke("stopStatis", mkey,t);
  }

  public IPromise<Map> index2LabelAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "index2Label");
  }

  public Map index2Label() {
    return (java.util.Map<java.lang.String,java.lang.Object>) this.proxyHolder.invoke("index2Label");
  }
}
