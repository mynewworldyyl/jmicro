package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.example.api.rpc.IRpcA;
import java.lang.String;

public interface IRpcAAsyncClient extends IRpcA {
  IPromise<String> invokeRpcAAsync(String aargs);
}
