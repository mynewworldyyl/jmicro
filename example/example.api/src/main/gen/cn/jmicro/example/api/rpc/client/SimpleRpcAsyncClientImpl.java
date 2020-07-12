package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.test.Person;
import java.lang.String;

public class SimpleRpcAsyncClientImpl extends AbstractClientServiceProxyHolder implements ISimpleRpcAsyncClient {
  public IPromise<String> helloAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "hello", (java.lang.Object)(name));
  }

  public String hello(String name) {
    return (java.lang.String) this.proxyHolder.invoke("hello", (java.lang.Object)(name));
  }

  public IPromise<String> hiAsync(Person p) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "hi", (java.lang.Object)(p));
  }

  public String hi(Person p) {
    return (java.lang.String) this.proxyHolder.invoke("hi", (java.lang.Object)(p));
  }

  public IPromise<String> linkRpcAsync(String msg) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "linkRpc", (java.lang.Object)(msg));
  }

  public String linkRpc(String msg) {
    return (java.lang.String) this.proxyHolder.invoke("linkRpc", (java.lang.Object)(msg));
  }
}
