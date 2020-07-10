package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.example.api.rpc.IAsyncRpcCallback;
import java.lang.String;
import java.lang.Void;

public interface IAsyncRpcCallbackAsyncClient extends IAsyncRpcCallback {
  IPromise<Void> callbackAsync(String name);
}
