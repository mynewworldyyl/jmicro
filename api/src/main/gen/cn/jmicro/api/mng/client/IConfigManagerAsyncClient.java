package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.ConfigNode;
import cn.jmicro.api.mng.IConfigManager;
import java.lang.Boolean;
import java.lang.String;

public interface IConfigManagerAsyncClient extends IConfigManager {
  IPromise<ConfigNode[]> getChildrenAsync(String path, Boolean getAll);

  IPromise<Boolean> updateAsync(String path, String val);

  IPromise<Boolean> deleteAsync(String path);

  IPromise<Boolean> addAsync(String path, String val, Boolean isDir);
}
