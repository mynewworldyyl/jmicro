package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.net.DataBlockJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;
import cn.jmicro.mng.vo.I18nJRso;

@AsyncClientProxy
public interface INI8NMngJMSrv {

	public static final  String TABLE = "t_i18n";
	
	public static final  String DEF_LAN = "zh";
	
	public static final  String DEF_CONTRIY = "CN";
	
	IPromise<RespJRso<Boolean>> add(I18nJRso vo);
	
	IPromise<RespJRso<Boolean>> delete(Long id);
	
	IPromise<RespJRso<Boolean>> update(I18nJRso vo);
	
	IPromise<RespJRso<List<I18nJRso>>> list(QueryJRso vo);
	
	IPromise<RespJRso<I18nJRso>> detail(Long id);
	
	IPromise<RespJRso<DataBlockJRso>> uploadFile(byte[] data);
}
