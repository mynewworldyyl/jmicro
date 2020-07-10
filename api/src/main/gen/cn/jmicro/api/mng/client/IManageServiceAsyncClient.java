package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.mng.IManageService;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.api.registry.ServiceMethod;
import java.lang.Boolean;
import java.util.Set;

public interface IManageServiceAsyncClient extends IManageService {
  IPromise<Set> getServicesAsync();

  IPromise<Boolean> updateMethodAsync(ServiceMethod method);

  IPromise<Boolean> updateItemAsync(ServiceItem item);
}
