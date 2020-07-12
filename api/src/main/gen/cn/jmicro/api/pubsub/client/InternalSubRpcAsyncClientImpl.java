package cn.jmicro.api.pubsub.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.pubsub.PSData;
import java.lang.Integer;
import java.lang.String;

public class InternalSubRpcAsyncClientImpl extends AbstractClientServiceProxyHolder implements IInternalSubRpcAsyncClient {
  public IPromise<Integer> publishItemAsync(PSData item) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "publishItem", (java.lang.Object)(item));
  }

  public int publishItem(PSData item) {
    return (int) this.proxyHolder.invoke("publishItem", (java.lang.Object)(item));
  }

  public IPromise<Integer> publishItemsAsync(String topic, PSData[] items) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "publishItems", topic,items);
  }

  public int publishItems(String topic, PSData[] items) {
    return (int) this.proxyHolder.invoke("publishItems", topic,items);
  }

  public IPromise<Integer> publishStringAsync(String topic, String content) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "publishString", topic,content);
  }

  public int publishString(String topic, String content) {
    return (int) this.proxyHolder.invoke("publishString", topic,content);
  }
}
