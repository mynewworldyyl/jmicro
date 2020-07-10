package cn.jmicro.example.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.example.api.DynamicInterface;
import java.lang.String;
import java.lang.Void;

public interface DynamicInterfaceAsyncClient extends DynamicInterface {
  IPromise<Void> runAsync(String data);
}
