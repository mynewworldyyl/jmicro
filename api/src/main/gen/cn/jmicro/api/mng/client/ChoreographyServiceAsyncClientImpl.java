package cn.jmicro.api.mng.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.choreography.Deployment;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import java.lang.Boolean;
import java.lang.String;
import java.util.List;

public class ChoreographyServiceAsyncClientImpl extends AbstractClientServiceProxyHolder implements IChoreographyServiceAsyncClient {
  public IPromise<Deployment> addDeploymentAsync(Deployment dep) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "addDeployment", (java.lang.Object)(dep));
  }

  public Deployment addDeployment(Deployment dep) {
    return (cn.jmicro.api.choreography.Deployment) this.proxyHolder.invoke("addDeployment", (java.lang.Object)(dep));
  }

  public IPromise<List> getDeploymentListAsync() {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getDeploymentList");
  }

  public List getDeploymentList() {
    return (java.util.List<cn.jmicro.api.choreography.Deployment>) this.proxyHolder.invoke("getDeploymentList");
  }

  public IPromise<Boolean> deleteDeploymentAsync(int id) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "deleteDeployment", (java.lang.Object)(id));
  }

  public boolean deleteDeployment(int id) {
    return (boolean) this.proxyHolder.invoke("deleteDeployment", (java.lang.Object)(id));
  }

  public IPromise<Boolean> updateDeploymentAsync(Deployment dep) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "updateDeployment", (java.lang.Object)(dep));
  }

  public boolean updateDeployment(Deployment dep) {
    return (boolean) this.proxyHolder.invoke("updateDeployment", (java.lang.Object)(dep));
  }

  public IPromise<List> getAgentListAsync(boolean showAll) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getAgentList", (java.lang.Object)(showAll));
  }

  public List getAgentList(boolean showAll) {
    return (java.util.List<cn.jmicro.api.choreography.AgentInfoVo>) this.proxyHolder.invoke("getAgentList", (java.lang.Object)(showAll));
  }

  public IPromise<List> getProcessInstanceListAsync(boolean all) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getProcessInstanceList", (java.lang.Object)(all));
  }

  public List getProcessInstanceList(boolean all) {
    return (java.util.List<cn.jmicro.api.choreography.ProcessInfo>) this.proxyHolder.invoke("getProcessInstanceList", (java.lang.Object)(all));
  }

  public IPromise<Boolean> stopProcessAsync(String insId) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "stopProcess", (java.lang.Object)(insId));
  }

  public boolean stopProcess(String insId) {
    return (boolean) this.proxyHolder.invoke("stopProcess", (java.lang.Object)(insId));
  }

  public IPromise<String> changeAgentStateAsync(String agentId) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "changeAgentState", (java.lang.Object)(agentId));
  }

  public String changeAgentState(String agentId) {
    return (java.lang.String) this.proxyHolder.invoke("changeAgentState", (java.lang.Object)(agentId));
  }
}
