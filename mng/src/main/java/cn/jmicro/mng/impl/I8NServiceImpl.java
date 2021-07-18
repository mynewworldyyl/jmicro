package cn.jmicro.mng.impl;

import java.util.Map;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.mng.api.II8NServiceJMSrv;
import cn.jmicro.mng.i18n.I18NManager;

@Component
@Service(external=true,showFront=false,version="0.0.1",logLevel=MC.LOG_NO)
public class I8NServiceImpl implements II8NServiceJMSrv {

	@Inject
	private I18NManager im;
	
	@Override
	@SMethod(needLogin=false)
	public RespJRso<Map<String, String>> keyValues(String resPath, String lang) {
		RespJRso<Map<String, String>> resp = new RespJRso<>();
		resp.setCode(RespJRso.CODE_SUCCESS);
		resp.setData(im.all(resPath, lang));
		return resp;
	}

}
