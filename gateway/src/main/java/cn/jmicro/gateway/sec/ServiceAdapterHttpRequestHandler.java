package cn.jmicro.gateway.sec;

import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.http.HttpRequest;
import cn.jmicro.api.http.HttpResponse;
import cn.jmicro.api.http.IHttpRequestHandler;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.common.Utils;
import lombok.extern.slf4j.Slf4j;

@Component("__srvAdap__")
@Slf4j
public class ServiceAdapterHttpRequestHandler implements IHttpRequestHandler {
	
	@Inject
	private ServiceInvokeManager asrv;
	//private AccountServiceJMSrv$JMAsyncClientImpl d;
	
	/*@Cfg(value = "/httpWhiteSmCodes", changeListener="")
	private String whiteSmCodes = "955538674,";*/
	
	public void jready() {
		
	}
	
	@Override
	public void handler(HttpRequest req, HttpResponse resp) {
		Map<String,String> ps = req.getAllParam();
		String smCodeStr = ps.get("__smc__");
		
		if(Utils.isEmpty(smCodeStr)) {
			resp(resp,"404无效请求");
			return;
		}
		
		try {
			Integer code = Integer.parseInt(smCodeStr);
			if(code == 1222260450) {
				//账号激活
				String token = ps.get("t");
				asrv.call(1222260450, new Object[] {token})
				.then((rst,fail,cxt)->{
					RespJRso<Boolean> r = (RespJRso<Boolean>)rst;
					if(r.getCode() == RespJRso.CODE_SUCCESS) {
						resp(resp,"激活成功");
					}else {
						resp(resp,r.getMsg());
					}
				});
			} else {
				resp(resp,"404无效请求");
				return;
			}
		} catch (Throwable e) {
			log.error(req.getUri(),e);
			resp(resp,"404无效请求");
			return;
		}
		
	}
	
	//application/json;charset:utf-8
	private void resp(HttpResponse resp,String content) {
		resp.contentType("text/html;charset:utf-8");
		resp.write(content);
	}

	@Override
	public boolean match(HttpRequest req) {
		return false;//不匹配任务路径
	}

	public void valueChange() {
		
	}
}
