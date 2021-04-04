package cn.expjmicro.example.tx.api.genclient;

import cn.expjmicro.example.tx.api.ITxShopService;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import java.lang.Object;

public interface ITxShopService$Gateway$JMAsyncClient extends ITxShopService {
  @WithContext
  IPromise<Resp> buyJMAsync(int goodId, int num, Object context);

  IPromise<Resp> buyJMAsync(int goodId, int num);

  @WithContext
  IPromise<Resp> updateLocalDataJMAsync(int goodId, int num, Object context);

  IPromise<Resp> updateLocalDataJMAsync(int goodId, int num);
}
