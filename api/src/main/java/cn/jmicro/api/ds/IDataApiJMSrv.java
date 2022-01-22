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
	 *    从缓存中查询上历史请求结果
	 *    如果缓存已经清空，则返回空
	 * @param reqId
	 * @return
	 */
	IPromise<RespJRso<String>> queryByReqId(String reqId);
	
	IPromise<RespJRso<List<DsQueryCacheJRso>>> listHistory(QueryJRso qry);
}
