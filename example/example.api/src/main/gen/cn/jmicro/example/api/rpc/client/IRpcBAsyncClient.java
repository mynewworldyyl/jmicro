package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.example.api.rpc.IRpcB;
import java.lang.String;

public interface IRpcBAsyncClient extends IRpcB {
  IPromise<String> invokeRpcBAsync(String bargs);
}
