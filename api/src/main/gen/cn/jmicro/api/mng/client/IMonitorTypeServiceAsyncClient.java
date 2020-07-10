package cn.jmicro.api.mng.client;

import cn.jmicro.api.Resp;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.IMonitorTypeService;
import cn.jmicro.api.monitor.MCConfig;
import java.lang.Short;
import java.lang.String;

public interface IMonitorTypeServiceAsyncClient extends IMonitorTypeService {
  IPromise<Resp> getAllConfigsAsync();

  IPromise<Resp> updateAsync(MCConfig mc);

  IPromise<Resp> deleteAsync(short type);

  IPromise<Resp> addAsync(MCConfig mc);

  IPromise<Resp> getConfigByMonitorKeyAsync(String key);

  IPromise<Resp> updateMonitorTypesAsync(String key, Short[] adds, Short[] dels);

  IPromise<Resp> getMonitorKeyListAsync();

  IPromise<Resp> getConfigByServiceMethodKeyAsync(String key);

  IPromise<Resp> updateServiceMethodMonitorTypesAsync(String key, Short[] adds, Short[] dels);

  IPromise<Resp> getAllConfigsByGroupAsync(String[] groups);

  IPromise<Resp> addNamedTypesAsync(String name);

  IPromise<Resp> getTypesByNamedAsync(String name);

  IPromise<Resp> updateNamedTypesAsync(String name, Short[] adds, Short[] dels);

  IPromise<Resp> getNamedListAsync();
}
