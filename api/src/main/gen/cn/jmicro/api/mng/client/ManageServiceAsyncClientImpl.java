package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import java.lang.Boolean;
import java.util.Set;

public class ManageServiceAsyncClientImpl extends AbstractClientServiceProxyHolder implements IManageServiceAsyncClient {
  public IPromise<Set> getServicesAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getServices");
  }

  public Set getServices() {
    return (java.util.Set<cn.jmicro.api.registry.ServiceItem>) this.proxyHolder.invoke("getServices");
  }

  public IPromise<Boolean> updateMethodAsync(ServiceMethod method) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "updateMethod", (java.lang.Object)(method));
  }

  public boolean updateMethod(ServiceMethod method) {
    return (boolean) this.proxyHolder.invoke("updateMethod", (java.lang.Object)(method));
  }

  public IPromise<Boolean> updateItemAsync(ServiceItem item) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "updateItem", (java.lang.Object)(item));
  }

  public boolean updateItem(ServiceItem item) {
    return (boolean) this.proxyHolder.invoke("updateItem", (java.lang.Object)(item));
  }
}
