package cn.jmicro.gateway.sec;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.http.HttpRequest;
import cn.jmicro.api.http.HttpResponse;
import cn.jmicro.api.http.IHttpRequestHandler;
import cn.jmicro.api.http.JHttpStatus;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MT;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.gateway.fs.FsDownloadHttpHandler;
import cn.jmicro.gateway.http.HttpServiceManager;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

@Component("srvDispatcher")
@Slf4j
public class Http2ServiceAdapter implements IHttpRequestHandler {
	
	private static final String TAG = Http2ServiceAdapter.class.getSimpleName();
	
	@Inject
	private ServiceInvokeManager asrv;
	
	@Inject
	private HttpServiceManager smng;
	
	@Inject
	private ServiceInvokeManager invoke;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private AccountManager accountManager;
	
	@Inject
	private PermissionManager pm;
	
	@Inject(required=false, value="fsd")
	private FsDownloadHttpHandler fsDispatcher;

	public void jready() {}
	
	public boolean handle(HttpRequest req, HttpResponse resp) {
		ServiceMethodJRso sm = this.smng.getMethodByHttpPath(req.getPath(),req.getClient());
		
		if(sm == null) {
			return false;
		}
		
		if(!Utils.isEmpty(sm.getHttpMethod()) && !sm.getHttpMethod().equals(req.getMethod())) {
			this.resp(resp, "不支持方法: " + req.getMethod(), req.getContentType());
			return true;
		}
		
		if(!Constants.HTTP_ALL_CONTENT_TYPE.equals(sm.getHttpReqContentType())) {
			if(!Utils.isEmpty(sm.getHttpReqContentType()) && Constants.HTTP_METHOD_POST.equals(req.getMethod()) 
					&& !sm.getHttpReqContentType().equals(req.getContentType())) {
				this.resp(resp, "不支持内容类型: " + req.getContentType(), req.getContentType());
				return true;
			}
		}
		
		
		JMicroContext.configHttpProvider(req, sm);
		
		if(!checkLoginAndPermission(req, resp, sm)) {
			return true;
		}
		
		Class<?>[] psClasses = sm.getKey().getParameterClasses();
		
		String body = req.getTextBody();
		if(req.isKv()) {
			body = JsonUtils.getIns().toJson(req.getAllParam());
		}
		
		IPromise<Object> p = null;
		if(psClasses == null || psClasses.length == 0) {
			//无需参数
			p = invoke.call(sm.getKey().getSnvHash(), new Object[0]);
		} else if(sm.isHttpReqBody()) {
			//单参数请求体类型
			if(psClasses.length > 1) {
				this.resp(resp, "请求参数不匹配", req.getContentType());
				return true;
			}
			
			Class<?> paramClass = psClasses[0];
			if(paramClass == String.class) {
				p = invoke.call(sm.getKey().getSnvHash(), new Object[]{body});
			} else {
				Object arg = JsonUtils.getIns().fromJson(body,paramClass);
				p = invoke.call(sm.getKey().getSnvHash(), new Object[]{arg});
			}
		} else {
			int idx = 0;
			
			Object[] args = new Object[psClasses.length];
			Map<String,String> ps = req.getAllParam();
			for(Class<?> ac : psClasses) {
				String mn = sm.getParamNames()[idx];
				String strVal = ps.get(mn);
				if(Utils.isEmpty(strVal)) {
					args[idx] = null;
				} else {
					args[idx] = getArgVal(ac, strVal);// JsonUtils.getIns().fromJson(ps.get(mn),ac);
				}
				idx++;
			}
			
			p = invoke.call(sm.getKey().getSnvHash(), args);
		}
		
		if(p == null) {
			this.resp(resp, "无效请求: " + req.getUri(), req.getContentType());
			return true;
		}
		
		p.success((rst,cxt)->{
			this.respRpcResult(resp,req, rst,sm);
		})
		.fail((code,msg,cxt)->{
			this.resp(resp, "{\"code\":\"" + code+"\",\"msg\":\""+msg+"\"}", 
					Constants.HTTP_JSON_CONTENT_TYPE);
		});
		return true;
	}
	
	private boolean checkLoginAndPermission(HttpRequest req, HttpResponse resp, ServiceMethodJRso sm) {
		
		if(sm.getMaxPacketSize() > 0 && req.getContentLen() > sm.getMaxPacketSize()) {
			String errMsg = "Packet too max " + req.getContentLen() + 
    				" limit size: " + sm.getMaxPacketSize() + "," + sm.getKey().getMethod();
    		LG.log(MC.LOG_ERROR, TAG,errMsg);
			MT.rpcEvent(MC.MT_PACKET_TOO_MAX,1);
			this.resp(resp, errMsg, req.getContentType());
			return false;
		}
		
		String lk = req.getHeaderParam(Message.EXTRA_KEY_LOGIN_KEY+"");
		ActInfoJRso ai = null;
		
		String slk = req.getHeaderParam(Message.EXTRA_KEY_LOGIN_SYS+"");
		ActInfoJRso sai = null;
		
		if(StringUtils.isNotEmpty(lk)) {
			ai = this.accountManager.getAccount(lk);
		}/* else if(StringUtils.isNotEmpty(req.getHeaderParam("token"))) {
			
		}*/
		
		if(StringUtils.isNotEmpty(slk)) {
			sai = this.accountManager.getAccount(slk);
		}
		
		if(sai != null) {
			JMicroContext.get().setString(JMicroContext.LOGIN_KEY_SYS, slk);
			JMicroContext.get().setSysAccount(sai);
		}
		
		if(ai != null) {
			JMicroContext.get().setString(JMicroContext.LOGIN_KEY, lk);
			JMicroContext.get().setAccount(ai);
		}

		if(ai == null && sm.isNeedLogin()) {
			String errMsg = "JRPC check invalid login key: "+req.getUri();
			LG.log(MC.LOG_ERROR, TAG, errMsg);
			MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
			this.resp(resp, errMsg, req.getContentType());
			return false;
		} 
	
		if(sai == null && sm.getForType() == Constants.FOR_TYPE_SYS) {
			String errMsg = "Invalid system login key: " + req.getUri();
			LG.log(MC.LOG_ERROR, TAG,errMsg);
			MT.rpcEvent(MC.MT_INVALID_LOGIN_INFO);
			this.resp(resp, errMsg, req.getContentType());
			return false;
		}
		
		if(sm.isPerType()) {
			RespJRso<Object> se = pm.permissionCheck(sm, sm.getKey().getUsk().getClientId());
			if(se != null) {
				this.resp(resp, se.getMsg(), req.getContentType());
				return false;
			}
		}
		
		return true;
	}
	
	private Object getArgVal(Class<?> valCls, String strVal) {

		if(valCls == byte.class || valCls == Byte.TYPE || valCls == Byte.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Byte.parseByte(strVal);
			}
		}else if(valCls == short.class || valCls == Short.TYPE || valCls == Short.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Short.parseShort(strVal);
			}
		}else if(valCls == int.class || valCls == Integer.TYPE || valCls == Integer.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Integer.parseInt(strVal);
			}
		}else if(valCls == long.class || valCls == Long.TYPE || valCls == Long.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Long.parseLong(strVal);
			}
		}else if(valCls == float.class || valCls == Float.TYPE || valCls == Float.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Float.parseFloat(strVal);
			}
		}else if(valCls == double.class || valCls == Double.TYPE || valCls == Double.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Double.parseDouble(strVal);
			}
		}else if(valCls == boolean.class || valCls == Boolean.TYPE || valCls == Boolean.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return Boolean.parseBoolean(strVal);
			}
		}else if(valCls == char.class || valCls == Character.TYPE || valCls == Character.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return strVal.charAt(0);
			}
		}else if(valCls == String.class ) {
			return strVal;
		}else if(valCls == Date.class ) {
			if(Utils.isEmpty(strVal)) {
				return null;
			}else {
				return new Date(Long.parseLong(strVal));
			}
		} else {
			if(Utils.isEmpty(strVal)) {
				return null;
			} else {
				//默认为JSON格式参数
				return JsonUtils.getIns().fromJson(strVal, valCls);
			}
		}
	}

	//application/json;charset:utf-8
	private void resp(HttpResponse resp, String content, String respType) {
		//"text/html;charset:utf-8"
		log.info(content);	
		try {
			if(Utils.isEmpty(respType)) {
				respType = Constants.HTTP_JSON_CONTENT_TYPE;
			}
			resp.setHeader("Content-Type", respType);
			byte[] data = content.getBytes(Constants.CHARSET);
			resp.setHeader("Content-Length",data.length);
			resp.write(data);
		} catch (UnsupportedEncodingException e) {
			log.error(content,e);
		}
	}
	
	//application/json;charset:utf-8
	private void respRpcResult(HttpResponse resp,HttpRequest req, Object rst,ServiceMethodJRso sm) {
		
		if(rst instanceof RespJRso) {
			RespJRso rr = (RespJRso)rst;
			int code = rr.getCode();
			if(!(code == RespJRso.CODE_SUCCESS || JHttpStatus.HTTP_OK == code)) {
				if(JHttpStatus.HTTP_MOVED_TEMP == code || JHttpStatus.HTTP_MOVED_PERM == code ||
						JHttpStatus.HTTP_SEE_OTHER == code) {
					//处理302重定向
					//重定向
					resp.redirect(code,rr.getData().toString());
					return;
				} else {
					HttpResponseStatus s = HttpResponseStatus.valueOf(code);
					if(s != null) {
						resp.setStatusCode(rr.getCode());
					}else {
						resp.setStatusCode(JHttpStatus.HTTP_NOT_FOUND);
					}
					this.resp(resp, JsonUtils.getIns().toJson(rst), req.getContentType());
				}
			}
		}
		
		if(sm.getHttpRespType() == Constants.HTTP_RESP_TYPE_RESTFULL) {
			this.resp(resp, JsonUtils.getIns().toJson(rst), req.getContentType());
		}else if(sm.getHttpRespType() == Constants.HTTP_RESP_TYPE_VIEW) {
			//通过模板文件返回动态内容
			respView(resp,req,rst,sm);
		}else if(sm.getHttpRespType() == Constants.HTTP_RESP_TYPE_STREAM) {
			//返回文件流
			if(rst instanceof RespJRso) {
				RespJRso rr = (RespJRso)rst;
				if(rr.getData() != null) {
					fsDispatcher.downloadFile(req, resp, rr.getData().toString(),true);
				} else {
					resp.setStatusCode(JHttpStatus.HTTP_NOT_FOUND);
					this.resp(resp, JsonUtils.getIns().toJson(rst), req.getContentType());
				}
			} else {
				//内容即为文件的ID
				fsDispatcher.downloadFile(req, resp, rst.toString(),true);
			}
		}else if(sm.getHttpRespType() == Constants.HTTP_RESP_TYPE_ORIGIN) {
			//剥离RespJRso实例数据，只返回data的值，如果rst非RespJRso实例,则直接返回rst
			if(rst instanceof RespJRso) {
				Object obj = ((RespJRso) rst).getData();
				if(obj instanceof String) {
					this.resp(resp, (String)obj, req.getContentType());
				}else {
					this.resp(resp, JsonUtils.getIns().toJson(obj), req.getContentType());
				}
			} else {
				//内容即为文件的ID
				if(rst instanceof String) {
					this.resp(resp, (String)rst, req.getContentType());
				}else {
					this.resp(resp, JsonUtils.getIns().toJson(rst), req.getContentType());
				}
			}
		}
	}

	/**
	 * 通过模板文件返回动态内容
	 * @param resp
	 * @param req
	 * @param rst 
	 * @param sm
	 */
	private void respView(HttpResponse resp, HttpRequest req, Object rst, ServiceMethodJRso sm) {
		
	}

	@Override
	public boolean match(HttpRequest req) {
		return false;//不匹配任务路径
	}

}
