package cn.jmicro.mng.impl;

import java.util.Map;

import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.mng.api.II8NService;
import cn.jmicro.mng.i18n.I18NManager;

@Component
@Service(external=true,showFront=true,namespace="mng",version="0.0.1")
public class I8NServiceImpl implements II8NService {

	@Inject
	private I18NManager im;
	
	@Override
	public Resp<Map<String, String>> keyValues(String resPath, String lang) {
		Resp<Map<String, String>> resp = new Resp<>();
		resp.setCode(Resp.CODE_SUCCESS);
		resp.setData(im.all(resPath, lang));
		return resp;
	}

}
