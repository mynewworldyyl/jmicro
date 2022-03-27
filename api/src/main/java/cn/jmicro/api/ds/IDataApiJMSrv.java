package cn.jmicro.api.ds;

import java.util.List;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IDataApiJMSrv {
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	IPromise<RespJRso<String>> getData(ApiReqJRso req);
	
	/**
	 * 
	 * @param reqId
	 * @return
	 */
	public IPromise<RespJRso<String>> testDataApi(String reqId);
	
	/**
	 * 从缓存中查询上历史请求结果
	 * 如果缓存已经清空，则返回空
	 * @param reqId
	 * @param trimSize 如果参数字符串长度大于trimSize，则截断参数，保留最大长度为trimSize个字符
	 * @return
	 */
	IPromise<RespJRso<DsQueryCacheJRso>> queryByReqId(String reqId,int trimSize);
	
	/**
	 * 
	 * @param qry
	 * @return
	 */
	IPromise<RespJRso<List<DsQueryCacheJRso>>> listHistory(QueryJRso qry);
}
