package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.ILogService;
import java.lang.Long;
import java.lang.String;
import java.util.Map;

public interface ILogServiceAsyncClient extends ILogService {
  IPromise<Resp> countAsync(Map<String, String> queryConditions);

  IPromise<Resp> queryAsync(Map<String, String> queryConditions, int pageSize, int curPage);

  IPromise<Resp> queryDictAsync();

  IPromise<Resp> getByLinkIdAsync(Long linkId);

  IPromise<Resp> countLogAsync(Map<String, String> queryConditions);

  IPromise<Resp> queryLogAsync(Map<String, String> queryConditions, int pageSize, int curPage);
}
