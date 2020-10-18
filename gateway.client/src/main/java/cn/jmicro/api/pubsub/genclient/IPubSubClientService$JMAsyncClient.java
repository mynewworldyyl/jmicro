package cn.jmicro.api.pubsub.genclient;

import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.PSData;

public interface IPubSubClientService$JMAsyncClient {
  @WithContext
  IPromise<Integer> publishMutilItemsJMAsync(PSData[] items, Object context);

  IPromise<Integer> publishMutilItemsJMAsync(PSData[] items);

  @WithContext
  IPromise<Integer> publishOneItemJMAsync(PSData item, Object context);

  IPromise<Integer> publishOneItemJMAsync(PSData item);

  int publishMutilItems(PSData[] items);
	
  int publishOneItem(PSData item);
	
}
