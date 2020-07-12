package cn.jmicro.example.api.rpc.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.example.api.rpc.IGenelizedType;

public interface IGenelizedTypeAsyncClient extends IGenelizedType {
  IPromise<byte[]> downResourceDataAsync(int downloadId, int blockNum);
}
