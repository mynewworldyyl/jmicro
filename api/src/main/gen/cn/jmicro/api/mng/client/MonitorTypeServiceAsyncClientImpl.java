package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.monitor.MCConfig;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Short;
import java.lang.String;

public class MonitorTypeServiceAsyncClientImpl extends AbstractClientServiceProxyHolder implements IMonitorTypeServiceAsyncClient {
  public IPromise<Resp> getAllConfigsAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getAllConfigs");
  }

  public Resp getAllConfigs() {
    return (cn.jmicro.api.Resp<java.util.List<cn.jmicro.api.monitor.MCConfig>>) this.proxyHolder.invoke("getAllConfigs");
  }

  public IPromise<Resp> updateAsync(MCConfig mc) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "update", (java.lang.Object)(mc));
  }

  public Resp update(MCConfig mc) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("update", (java.lang.Object)(mc));
  }

  public IPromise<Resp> deleteAsync(short type) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "delete", (java.lang.Object)(type));
  }

  public Resp delete(short type) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("delete", (java.lang.Object)(type));
  }

  public IPromise<Resp> addAsync(MCConfig mc) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "add", (java.lang.Object)(mc));
  }

  public Resp add(MCConfig mc) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("add", (java.lang.Object)(mc));
  }

  public IPromise<Resp> getConfigByMonitorKeyAsync(String key) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getConfigByMonitorKey", (java.lang.Object)(key));
  }

  public Resp getConfigByMonitorKey(String key) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("getConfigByMonitorKey", (java.lang.Object)(key));
  }

  public IPromise<Resp> updateMonitorTypesAsync(String key, Short[] adds, Short[] dels) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "updateMonitorTypes", key,adds,dels);
  }

  public Resp updateMonitorTypes(String key, Short[] adds, Short[] dels) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("updateMonitorTypes", key,adds,dels);
  }

  public IPromise<Resp> getMonitorKeyListAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getMonitorKeyList");
  }

  public Resp getMonitorKeyList() {
    return (cn.jmicro.api.Resp<java.util.Map<java.lang.String,java.lang.String>>) this.proxyHolder.invoke("getMonitorKeyList");
  }

  public IPromise<Resp> getConfigByServiceMethodKeyAsync(String key) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getConfigByServiceMethodKey", (java.lang.Object)(key));
  }

  public Resp getConfigByServiceMethodKey(String key) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("getConfigByServiceMethodKey", (java.lang.Object)(key));
  }

  public IPromise<Resp> updateServiceMethodMonitorTypesAsync(String key, Short[] adds,
      Short[] dels) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "updateServiceMethodMonitorTypes", key,adds,dels);
  }

  public Resp updateServiceMethodMonitorTypes(String key, Short[] adds, Short[] dels) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("updateServiceMethodMonitorTypes", key,adds,dels);
  }

  public IPromise<Resp> getAllConfigsByGroupAsync(String[] groups) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getAllConfigsByGroup", (java.lang.Object)(groups));
  }

  public Resp getAllConfigsByGroup(String[] groups) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("getAllConfigsByGroup", (java.lang.Object)(groups));
  }

  public IPromise<Resp> addNamedTypesAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "addNamedTypes", (java.lang.Object)(name));
  }

  public Resp addNamedTypes(String name) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("addNamedTypes", (java.lang.Object)(name));
  }

  public IPromise<Resp> getTypesByNamedAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getTypesByNamed", (java.lang.Object)(name));
  }

  public Resp getTypesByNamed(String name) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("getTypesByNamed", (java.lang.Object)(name));
  }

  public IPromise<Resp> updateNamedTypesAsync(String name, Short[] adds, Short[] dels) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "updateNamedTypes", name,adds,dels);
  }

  public Resp updateNamedTypes(String name, Short[] adds, Short[] dels) {
    return (cn.jmicro.api.Resp) this.proxyHolder.invoke("updateNamedTypes", name,adds,dels);
  }

  public IPromise<Resp> getNamedListAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getNamedList");
  }

  public Resp getNamedList() {
    return (cn.jmicro.api.Resp<java.util.List<java.lang.String>>) this.proxyHolder.invoke("getNamedList");
  }
}
