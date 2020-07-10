package cn.jmicro.api.pubsub.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.IInternalSubRpc;
import cn.jmicro.api.pubsub.PSData;
import java.lang.Integer;
import java.lang.String;

public interface IInternalSubRpcAsyncClient extends IInternalSubRpc {
  IPromise<Integer> publishItemAsync(PSData item);

  IPromise<Integer> publishItemsAsync(String topic, PSData[] items);

  IPromise<Integer> publishStringAsync(String topic, String content);
}
