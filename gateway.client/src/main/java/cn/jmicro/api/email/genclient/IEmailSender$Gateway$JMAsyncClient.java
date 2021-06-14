package cn.jmicro.api.email.genclient;

import cn.jmicro.api.annotation.WithContext;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.email.IEmailSender;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.String;

public interface IEmailSender$Gateway$JMAsyncClient extends IEmailSender {
  @WithContext
  IPromise<Boolean> sendJMAsync(String to, String title, String message, Object context);

  IPromise<Boolean> sendJMAsync(String to, String title, String message);
}
