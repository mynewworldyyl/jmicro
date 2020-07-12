package cn.jmicro.api.monitor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MRpcItem;
import java.lang.Short;
import java.lang.String;
import java.lang.Void;

public interface IMonitorDataSubscriberAsyncClient extends IMonitorDataSubscriber {
  IPromise<Void> onSubmitAsync(MRpcItem[] sis);

  IPromise<ReportData> getDataAsync(String srvKey, Short[] type, String[] dataType);
}
