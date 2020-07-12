package cn.jmicro.example.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.test.Person;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.lang.Void;

public class TestRpcServiceAsyncClientImpl extends AbstractClientServiceProxyHolder implements ITestRpcServiceAsyncClient {
  public IPromise<Person> getPersonAsync(Person p) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getPerson", (java.lang.Object)(p));
  }

  public Person getPerson(Person p) {
    return (cn.jmicro.api.test.Person) this.proxyHolder.invoke("getPerson", (java.lang.Object)(p));
  }

  public IPromise<Void> pushMessageAsync(String msg) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "pushMessage", (java.lang.Object)(msg));
  }

  public void pushMessage(String msg) {
    this.proxyHolder.invoke("pushMessage", (java.lang.Object)(msg));
  }

  public IPromise<String> helloAsync(String name) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "hello", (java.lang.Object)(name));
  }

  public String hello(String name) {
    return (java.lang.String) this.proxyHolder.invoke("hello", (java.lang.Object)(name));
  }

  public IPromise<Integer> testReturnPrimitiveResultAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "testReturnPrimitiveResult");
  }

  public int testReturnPrimitiveResult() {
    return (int) this.proxyHolder.invoke("testReturnPrimitiveResult");
  }

  public IPromise<int[]> testReturnPrimitiveArrayResultAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "testReturnPrimitiveArrayResult");
  }

  public int[] testReturnPrimitiveArrayResult() {
    return (int[]) this.proxyHolder.invoke("testReturnPrimitiveArrayResult");
  }

  public IPromise<Boolean> testReturnBooleanResultAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "testReturnBooleanResult");
  }

  public Boolean testReturnBooleanResult() {
    return (java.lang.Boolean) this.proxyHolder.invoke("testReturnBooleanResult");
  }

  public IPromise<Boolean> testReturnPrimitiveBooleanResultAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "testReturnPrimitiveBooleanResult");
  }

  public boolean testReturnPrimitiveBooleanResult() {
    return (boolean) this.proxyHolder.invoke("testReturnPrimitiveBooleanResult");
  }
}
