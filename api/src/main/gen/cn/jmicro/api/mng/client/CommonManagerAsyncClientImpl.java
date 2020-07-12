package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Boolean;
import java.lang.String;
import java.util.Map;

public class CommonManagerAsyncClientImpl extends AbstractClientServiceProxyHolder implements ICommonManagerAsyncClient {
  public IPromise<Map> getI18NValuesAsync(String lang) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getI18NValues", (java.lang.Object)(lang));
  }

  public Map getI18NValues(String lang) {
    return (java.util.Map<java.lang.String,java.lang.String>) this.proxyHolder.invoke("getI18NValues", (java.lang.Object)(lang));
  }

  public IPromise<Boolean> hasPermissionAsync(int per) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "hasPermission", (java.lang.Object)(per));
  }

  public boolean hasPermission(int per) {
    return (boolean) this.proxyHolder.invoke("hasPermission", (java.lang.Object)(per));
  }

  public IPromise<Boolean> notLoginPermissionAsync(int per) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "notLoginPermission", (java.lang.Object)(per));
  }

  public boolean notLoginPermission(int per) {
    return (boolean) this.proxyHolder.invoke("notLoginPermission", (java.lang.Object)(per));
  }

  public IPromise<Resp> getDictsAsync(String[] keys) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getDicts", (java.lang.Object)(keys));
  }

  public Resp getDicts(String[] keys) {
    return (cn.jmicro.api.Resp<java.util.Map<java.lang.String,java.lang.Object>>) this.proxyHolder.invoke("getDicts", (java.lang.Object)(keys));
  }
}
