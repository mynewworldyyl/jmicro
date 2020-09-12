package cn.jmicro.mng.api;

import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface II8NService {

	Resp<Map<String,String>> keyValues(String resPath,String lang);
}
