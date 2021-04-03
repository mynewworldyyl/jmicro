package cn.expjmicro.example.tx.api.genclient;

import cn.jmicro.api.registry.ServiceItem;

public interface ITxOrderService$JMAsyncClient extends ITxOrderService$Gateway$JMAsyncClient {
  boolean isReady();

  int clientId();

  ServiceItem getItem();
}
