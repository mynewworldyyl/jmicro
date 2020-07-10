package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.mng.IChoreographyService;
import java.lang.Boolean;
import java.lang.String;
import java.util.List;

public interface IChoreographyServiceAsyncClient extends IChoreographyService {
  IPromise<Deployment> addDeploymentAsync(Deployment dep);

  IPromise<List> getDeploymentListAsync();

  IPromise<Boolean> deleteDeploymentAsync(int id);

  IPromise<Boolean> updateDeploymentAsync(Deployment dep);

  IPromise<List> getAgentListAsync(boolean showAll);

  IPromise<List> getProcessInstanceListAsync(boolean all);

  IPromise<Boolean> stopProcessAsync(String insId);

  IPromise<String> changeAgentStateAsync(String agentId);
}
