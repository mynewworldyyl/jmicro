package cn.expjmicro.example.tx.api.genclient;

import cn.expjmicro.example.tx.api.entities.Payment;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Object;

public class TxPaymentService$JMAsyncClientImpl extends AbstractClientServiceProxyHolder implements ITxPaymentService$JMAsyncClient {
  public IPromise<Resp> payJMAsync(Payment p) {
    return  this.proxyHolder.invoke("payJMAsync", null, (java.lang.Object)(p));
  }

  public Resp pay(Payment p) {
    return (cn.jmicro.api.Resp<java.lang.Boolean>) this.proxyHolder.invoke("pay",null, (java.lang.Object)(p));
  }

  @WithContext
  public IPromise<Resp> payJMAsync(Payment p, Object context) {
    return this.proxyHolder.invoke("payJMAsync",context,(java.lang.Object)(p));
  }
}
