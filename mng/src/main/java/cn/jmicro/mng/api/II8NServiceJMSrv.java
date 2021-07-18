package cn.jmicro.mng.api;

import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface II8NServiceJMSrv {

	RespJRso<Map<String,String>> keyValues(String resPath,String lang);
}
