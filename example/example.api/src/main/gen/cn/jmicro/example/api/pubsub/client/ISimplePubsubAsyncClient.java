package cn.jmicro.example.api.pubsub.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.example.api.pubsub.ISimplePubsub;
import java.lang.Object;
import java.lang.String;
import java.lang.Void;
import java.util.Map;

public interface ISimplePubsubAsyncClient extends ISimplePubsub {
  IPromise<Void> helloTopicAsync(PSData data);

  IPromise<Void> statisAsync(PSData data);

  IPromise<Void> testTopicAsync(PSData data);

  IPromise<Void> helloTopicWithArrayArgsAsync(PSData[] data);

  IPromise<Void> notifyMessageStatuAsync(int statusCode, long msgId, Map<String, Object> cxt);
}
