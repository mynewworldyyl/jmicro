package cn.expjmicro.example.tx.api.genclient;

import cn.jmicro.api.registry.ServiceItem;

public interface ITxShopService$JMAsyncClient extends ITxShopService$Gateway$JMAsyncClient {
  boolean isReady();

  int clientId();

  ServiceItem getItem();
}
