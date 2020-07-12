package cn.jmicro.example.api.pubsub.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.pubsub.PSData;
import java.lang.Object;
import java.lang.String;
import java.lang.Void;
import java.util.Map;

public class SimplePubsubAsyncClientImpl extends AbstractClientServiceProxyHolder implements ISimplePubsubAsyncClient {
  public IPromise<Void> helloTopicAsync(PSData data) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "helloTopic", (java.lang.Object)(data));
  }

  public void helloTopic(PSData data) {
    this.proxyHolder.invoke("helloTopic", (java.lang.Object)(data));
  }

  public IPromise<Void> statisAsync(PSData data) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "statis", (java.lang.Object)(data));
  }

  public void statis(PSData data) {
    this.proxyHolder.invoke("statis", (java.lang.Object)(data));
  }

  public IPromise<Void> testTopicAsync(PSData data) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "testTopic", (java.lang.Object)(data));
  }

  public void testTopic(PSData data) {
    this.proxyHolder.invoke("testTopic", (java.lang.Object)(data));
  }

  public IPromise<Void> helloTopicWithArrayArgsAsync(PSData[] data) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "helloTopicWithArrayArgs", (java.lang.Object)(data));
  }

  public void helloTopicWithArrayArgs(PSData[] data) {
    this.proxyHolder.invoke("helloTopicWithArrayArgs", (java.lang.Object)(data));
  }

  public IPromise<Void> notifyMessageStatuAsync(int statusCode, long msgId,
      Map<String, Object> cxt) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "notifyMessageStatu", statusCode,msgId,cxt);
  }

  public void notifyMessageStatu(int statusCode, long msgId, Map<String, Object> cxt) {
    this.proxyHolder.invoke("notifyMessageStatu", statusCode,msgId,cxt);
  }
}
