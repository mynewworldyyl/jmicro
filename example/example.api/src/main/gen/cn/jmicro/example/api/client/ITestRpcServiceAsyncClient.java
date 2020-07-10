package cn.jmicro.example.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.test.Person;
import cn.jmicro.example.api.ITestRpcService;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.lang.Void;

public interface ITestRpcServiceAsyncClient extends ITestRpcService {
  IPromise<Person> getPersonAsync(Person p);

  IPromise<Void> pushMessageAsync(String msg);

  IPromise<String> helloAsync(String name);

  IPromise<Integer> testReturnPrimitiveResultAsync();

  IPromise<int[]> testReturnPrimitiveArrayResultAsync();

  IPromise<Boolean> testReturnBooleanResultAsync();

  IPromise<Boolean> testReturnPrimitiveBooleanResultAsync();
}
