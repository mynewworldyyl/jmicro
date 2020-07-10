package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.test.Person;
import cn.jmicro.example.api.rpc.ISimpleRpc;
import java.lang.String;

public interface ISimpleRpcAsyncClient extends ISimpleRpc {
  IPromise<String> helloAsync(String name);

  IPromise<String> hiAsync(Person p);

  IPromise<String> linkRpcAsync(String msg);
}
