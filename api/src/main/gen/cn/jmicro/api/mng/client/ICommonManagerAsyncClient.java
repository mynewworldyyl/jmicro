package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.ICommonManager;
import java.lang.Boolean;
import java.lang.String;
import java.util.Map;

public interface ICommonManagerAsyncClient extends ICommonManager {
  IPromise<Map> getI18NValuesAsync(String lang);

  IPromise<Boolean> hasPermissionAsync(int per);

  IPromise<Boolean> notLoginPermissionAsync(int per);

  IPromise<Resp> getDictsAsync(String[] keys);
}
