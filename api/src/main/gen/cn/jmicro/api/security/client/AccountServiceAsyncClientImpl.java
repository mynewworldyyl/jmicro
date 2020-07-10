package cn.jmicro.api.security.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.objectfactory.AbstractClientServiceProxyHolder;
import cn.jmicro.api.security.ActInfo;
import java.lang.Boolean;
import java.lang.String;

public class AccountServiceAsyncClientImpl extends AbstractClientServiceProxyHolder implements IAccountServiceAsyncClient {
  public IPromise<ActInfo> loginAsync(String actName, String pwd) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "login", actName,pwd);
  }

  public ActInfo login(String actName, String pwd) {
    return (cn.jmicro.api.security.ActInfo) this.proxyHolder.invoke("login", actName,pwd);
  }

  public IPromise<Boolean> logoutAsync(String loginKey) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "logout", (java.lang.Object)(loginKey));
  }

  public boolean logout(String loginKey) {
    return (java.lang.Boolean) this.proxyHolder.invoke("logout", (java.lang.Object)(loginKey));
  }

  public IPromise<Boolean> isLoginAsync(String loginKey) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "isLogin", (java.lang.Object)(loginKey));
  }

  public boolean isLogin(String loginKey) {
    return (java.lang.Boolean) this.proxyHolder.invoke("isLogin", (java.lang.Object)(loginKey));
  }

  public IPromise<ActInfo> getAccountAsync(String loginKey) {
    return cn.jmicro.api.async.PromiseUtils.callService(this, "getAccount", (java.lang.Object)(loginKey));
  }

  public ActInfo getAccount(String loginKey) {
    return (cn.jmicro.api.security.ActInfo) this.proxyHolder.invoke("getAccount", (java.lang.Object)(loginKey));
  }
}
