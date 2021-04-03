package cn.expjmicro.example.tx.api.genclient;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Object;

public class TxOrderService$JMAsyncClientImpl extends AbstractClientServiceProxyHolder implements ITxOrderService$JMAsyncClient {
  public IPromise<Resp> takeOrderJMAsync(int goodId, int num) {
    return  this.proxyHolder.invoke("takeOrderJMAsync", null, goodId,num);
  }

  public Resp takeOrder(int goodId, int num) {
    return (cn.jmicro.api.Resp<java.lang.Boolean>) this.proxyHolder.invoke("takeOrder",null, goodId,num);
  }

  @WithContext
  public IPromise<Resp> takeOrderJMAsync(int goodId, int num, Object context) {
    return this.proxyHolder.invoke("takeOrderJMAsync",context, goodId,num);
  }
}
