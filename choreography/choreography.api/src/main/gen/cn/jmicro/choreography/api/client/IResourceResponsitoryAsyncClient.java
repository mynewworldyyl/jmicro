package cn.jmicro.choreography.api.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.choreography.api.IResourceResponsitory;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.List;

public interface IResourceResponsitoryAsyncClient extends IResourceResponsitory {
  IPromise<List> getResourceListAsync(boolean onlyFinish);

  IPromise<Integer> addResourceAsync(String name, int totalSize);

  IPromise<Boolean> addResourceDataAsync(String name, byte[] data, int blockNum);

  IPromise<Boolean> deleteResourceAsync(String name);

  IPromise<byte[]> downResourceDataAsync(int downloadId, int blockNum);

  IPromise<Integer> initDownloadResourceAsync(String name);
}
