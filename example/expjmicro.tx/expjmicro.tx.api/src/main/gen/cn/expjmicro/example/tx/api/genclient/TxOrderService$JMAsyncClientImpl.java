package cn.expjmicro.example.tx.api.genclient;

import cn.expjmicro.example.tx.api.entities.Good;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Object;

public class TxOrderService$JMAsyncClientImpl extends AbstractClientServiceProxyHolder implements ITxOrderService$JMAsyncClient {
  public IPromise<Resp> takeOrderJMAsync(Good good, int num) {
    return  this.proxyHolder.invoke("takeOrderJMAsync", null, good,num);
  }

  public Resp takeOrder(Good good, int num) {
    return (cn.jmicro.api.Resp<java.lang.Boolean>) this.proxyHolder.invoke("takeOrder",null, good,num);
  }

  @WithContext
  public IPromise<Resp> takeOrderJMAsync(Good good, int num, Object context) {
    return this.proxyHolder.invoke("takeOrderJMAsync",context, good,num);
  }

  public IPromise takeOrderAsy(Good good, int num) {
    return (cn.jmicro.api.async.IPromise<cn.jmicro.api.Resp<java.lang.Boolean>>) this.proxyHolder.invoke("takeOrderAsy",null, good,num);
  }

  @WithContext
  public IPromise takeOrderAsyJMAsync(Good good, int num, Object context) {
    return this.proxyHolder.invoke("takeOrderAsyJMAsync",context, good,num);
  }
}
