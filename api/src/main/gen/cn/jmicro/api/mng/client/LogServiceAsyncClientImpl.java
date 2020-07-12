package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Long;
import java.lang.String;
import java.util.Map;

public class LogServiceAsyncClientImpl extends AbstractClientServiceProxyHolder implements ILogServiceAsyncClient {
  public IPromise<Resp> countAsync(Map<String, String> queryConditions) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "count", (java.lang.Object)(queryConditions));
  }

  public Resp count(Map<String, String> queryConditions) {
    return (cn.jmicro.api.Resp<java.lang.Long>) this.proxyHolder.invoke("count", (java.lang.Object)(queryConditions));
  }

  public IPromise<Resp> queryAsync(Map<String, String> queryConditions, int pageSize, int curPage) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "query", queryConditions,pageSize,curPage);
  }

  public Resp query(Map<String, String> queryConditions, int pageSize, int curPage) {
    return (cn.jmicro.api.Resp<java.util.List<cn.jmicro.api.mng.LogEntry>>) this.proxyHolder.invoke("query", queryConditions,pageSize,curPage);
  }

  public IPromise<Resp> queryDictAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "queryDict");
  }

  public Resp queryDict() {
    return (cn.jmicro.api.Resp<java.util.Map<java.lang.String,java.lang.Object>>) this.proxyHolder.invoke("queryDict");
  }

  public IPromise<Resp> getByLinkIdAsync(Long linkId) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getByLinkId", (java.lang.Object)(linkId));
  }

  public Resp getByLinkId(Long linkId) {
    return (cn.jmicro.api.Resp<cn.jmicro.api.mng.LogEntry>) this.proxyHolder.invoke("getByLinkId", (java.lang.Object)(linkId));
  }

  public IPromise<Resp> countLogAsync(Map<String, String> queryConditions) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "countLog", (java.lang.Object)(queryConditions));
  }

  public Resp countLog(Map<String, String> queryConditions) {
    return (cn.jmicro.api.Resp<java.lang.Integer>) this.proxyHolder.invoke("countLog", (java.lang.Object)(queryConditions));
  }

  public IPromise<Resp> queryLogAsync(Map<String, String> queryConditions, int pageSize,
      int curPage) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "queryLog", queryConditions,pageSize,curPage);
  }

  public Resp queryLog(Map<String, String> queryConditions, int pageSize, int curPage) {
    return (cn.jmicro.api.Resp<java.util.List<cn.jmicro.api.mng.LogItem>>) this.proxyHolder.invoke("queryLog", queryConditions,pageSize,curPage);
  }
}
