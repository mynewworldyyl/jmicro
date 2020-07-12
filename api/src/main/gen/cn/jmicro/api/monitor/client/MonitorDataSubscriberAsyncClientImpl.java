package cn.jmicro.api.monitor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Short;
import java.lang.String;
import java.lang.Void;

public class MonitorDataSubscriberAsyncClientImpl extends AbstractClientServiceProxyHolder implements IMonitorDataSubscriberAsyncClient {
  public IPromise<Void> onSubmitAsync(MRpcItem[] sis) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "onSubmit", (java.lang.Object)(sis));
  }

  public void onSubmit(MRpcItem[] sis) {
    this.proxyHolder.invoke("onSubmit", (java.lang.Object)(sis));
  }

  public IPromise<ReportData> getDataAsync(String srvKey, Short[] type, String[] dataType) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getData", srvKey,type,dataType);
  }

  public ReportData getData(String srvKey, Short[] type, String[] dataType) {
    return (cn.jmicro.api.mng.ReportData) this.proxyHolder.invoke("getData", srvKey,type,dataType);
  }
}
