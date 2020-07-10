package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.test.Person;
import java.lang.String;

public class SimpleRpcAsyncClientImpl extends AbstractClientServiceProxyHolder implements ISimpleRpcAsyncClient {
  public IPromise<String> helloAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "hello", name);
  }

  public String hello(String name) {
    return (java.lang.String) this.proxyHolder.invoke("hello", name);
  }

  public IPromise<String> hiAsync(Person p) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "hi", p);
  }

  public String hi(Person p) {
    return (java.lang.String) this.proxyHolder.invoke("hi", p);
  }

  public IPromise<String> linkRpcAsync(String msg) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "linkRpc", msg);
  }

  public String linkRpc(String msg) {
    return (java.lang.String) this.proxyHolder.invoke("linkRpc", msg);
  }
}
