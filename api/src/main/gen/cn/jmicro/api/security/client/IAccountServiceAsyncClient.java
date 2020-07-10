package cn.jmicro.api.security.client;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.security.ActInfo;
import cn.jmicro.api.security.IAccountService;
import java.lang.Boolean;
import java.lang.String;

public interface IAccountServiceAsyncClient extends IAccountService {
  IPromise<ActInfo> loginAsync(String actName, String pwd);

  IPromise<Boolean> logoutAsync(String loginKey);

  IPromise<Boolean> isLoginAsync(String loginKey);

  IPromise<ActInfo> getAccountAsync(String loginKey);
}
