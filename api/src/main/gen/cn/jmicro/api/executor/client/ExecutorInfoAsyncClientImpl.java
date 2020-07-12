package cn.jmicro.api.executor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.executor.ExecutorInfo;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;

public class ExecutorInfoAsyncClientImpl extends AbstractClientServiceProxyHolder implements IExecutorInfoAsyncClient {
  public IPromise<ExecutorInfo> getInfoAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getInfo");
  }

  public ExecutorInfo getInfo() {
    return (cn.jmicro.api.executor.ExecutorInfo) this.proxyHolder.invoke("getInfo");
  }
}
