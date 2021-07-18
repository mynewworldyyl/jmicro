package cn.jmicro.api.email.genclient;

import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.email.IEmailSenderJMSrv;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;

public interface IEmailSender$Gateway$JMAsyncClient extends IEmailSenderJMSrv {
  @WithContext
  IPromise<Boolean> sendJMAsync(String to, String title, String message, Object context);

  IPromise<Boolean> sendJMAsync(String to, String title, String message);
}
