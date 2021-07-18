package cn.jmicro.api.pubsub.genclient;

import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.PSDataJRso;

public interface IPubSubClientService$JMAsyncClient {
  @WithContext
  IPromise<Integer> publishMutilItemsJMAsync(PSDataJRso[] items, Object context);

  IPromise<Integer> publishMutilItemsJMAsync(PSDataJRso[] items);

  @WithContext
  IPromise<Integer> publishOneItemJMAsync(PSDataJRso item, Object context);

  IPromise<Integer> publishOneItemJMAsync(PSDataJRso item);

  int publishMutilItems(PSDataJRso[] items);
	
  int publishOneItem(PSDataJRso item);
	
}
