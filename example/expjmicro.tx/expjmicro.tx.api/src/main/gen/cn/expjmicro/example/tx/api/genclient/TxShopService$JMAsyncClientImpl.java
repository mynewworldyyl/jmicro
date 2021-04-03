package cn.expjmicro.example.tx.api.genclient;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Object;

public class TxShopService$JMAsyncClientImpl extends AbstractClientServiceProxyHolder implements ITxShopService$JMAsyncClient {
  public IPromise<Resp> buyJMAsync(int goodId, int num) {
    return  this.proxyHolder.invoke("buyJMAsync", null, goodId,num);
  }

  public Resp buy(int goodId, int num) {
    return (cn.jmicro.api.Resp<java.lang.Boolean>) this.proxyHolder.invoke("buy",null, goodId,num);
  }

  @WithContext
  public IPromise<Resp> buyJMAsync(int goodId, int num, Object context) {
    return this.proxyHolder.invoke("buyJMAsync",context, goodId,num);
  }
}
