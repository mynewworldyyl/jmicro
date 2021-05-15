package cn.expjmicro.example.tx.api.genclient;

import cn.expjmicro.example.tx.api.ITxOrderService;
import cn.expjmicro.example.tx.api.entities.Good;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import java.lang.Object;

public interface ITxOrderService$Gateway$JMAsyncClient extends ITxOrderService {
  @WithContext
  IPromise<Resp> takeOrderJMAsync(Good good, int num, Object context);

  IPromise<Resp> takeOrderJMAsync(Good good, int num);
}
