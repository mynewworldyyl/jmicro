package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;

public class GenelizedTypeAsyncClientImpl extends AbstractClientServiceProxyHolder implements IGenelizedTypeAsyncClient {
  public IPromise<byte[]> downResourceDataAsync(int downloadId, int blockNum) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "downResourceData", downloadId,blockNum);
  }

  public byte[] downResourceData(int downloadId, int blockNum) {
    return (byte[]) this.proxyHolder.invoke("downResourceData", downloadId,blockNum);
  }
}
