package cn.jmicro.choreography.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public class ResourceResponsitoryAsyncClientImpl extends AbstractClientServiceProxyHolder implements IResourceResponsitoryAsyncClient {
  public IPromise<List> getResourceListAsync(boolean onlyFinish) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getResourceList", (java.lang.Object)(onlyFinish));
  }

  public List getResourceList(boolean onlyFinish) {
    return (java.util.List<cn.jmicro.choreography.api.PackageResource>) this.proxyHolder.invoke("getResourceList", (java.lang.Object)(onlyFinish));
  }

  public IPromise<Integer> addResourceAsync(String name, int totalSize) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "addResource", name,totalSize);
  }

  public int addResource(String name, int totalSize) {
    return (int) this.proxyHolder.invoke("addResource", name,totalSize);
  }

  public IPromise<Boolean> addResourceDataAsync(String name, byte[] data, int blockNum) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "addResourceData", name,data,blockNum);
  }

  public boolean addResourceData(String name, byte[] data, int blockNum) {
    return (boolean) this.proxyHolder.invoke("addResourceData", name,data,blockNum);
  }

  public IPromise<Boolean> deleteResourceAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "deleteResource", (java.lang.Object)(name));
  }

  public boolean deleteResource(String name) {
    return (boolean) this.proxyHolder.invoke("deleteResource", (java.lang.Object)(name));
  }

  public IPromise<byte[]> downResourceDataAsync(int downloadId, int blockNum) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "downResourceData", downloadId,blockNum);
  }

  public byte[] downResourceData(int downloadId, int blockNum) {
    return (byte[]) this.proxyHolder.invoke("downResourceData", downloadId,blockNum);
  }

  public IPromise<Integer> initDownloadResourceAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "initDownloadResource", (java.lang.Object)(name));
  }

  public int initDownloadResource(String name) {
    return (int) this.proxyHolder.invoke("initDownloadResource", (java.lang.Object)(name));
  }
}
