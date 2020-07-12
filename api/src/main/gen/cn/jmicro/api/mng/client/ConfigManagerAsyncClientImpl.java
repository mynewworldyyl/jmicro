package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.ConfigNode;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Boolean;
import java.lang.String;

public class ConfigManagerAsyncClientImpl extends AbstractClientServiceProxyHolder implements IConfigManagerAsyncClient {
  public IPromise<ConfigNode[]> getChildrenAsync(String path, Boolean getAll) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getChildren", path,getAll);
  }

  public ConfigNode[] getChildren(String path, Boolean getAll) {
    return (cn.jmicro.api.mng.ConfigNode[]) this.proxyHolder.invoke("getChildren", path,getAll);
  }

  public IPromise<Boolean> updateAsync(String path, String val) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "update", path,val);
  }

  public boolean update(String path, String val) {
    return (boolean) this.proxyHolder.invoke("update", path,val);
  }

  public IPromise<Boolean> deleteAsync(String path) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "delete", (java.lang.Object)(path));
  }

  public boolean delete(String path) {
    return (boolean) this.proxyHolder.invoke("delete", (java.lang.Object)(path));
  }

  public IPromise<Boolean> addAsync(String path, String val, Boolean isDir) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "add", path,val,isDir);
  }

  public boolean add(String path, String val, Boolean isDir) {
    return (boolean) this.proxyHolder.invoke("add", path,val,isDir);
  }
}
