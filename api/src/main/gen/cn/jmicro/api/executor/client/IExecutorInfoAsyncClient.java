package cn.jmicro.api.executor.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.executor.ExecutorInfo;
import cn.jmicro.api.executor.IExecutorInfo;

public interface IExecutorInfoAsyncClient extends IExecutorInfo {
  IPromise<ExecutorInfo> getInfoAsync();
}
