package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.QryDefJRso;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.ext.mongodb.CRUDService;
import cn.jmicro.mng.api.II8NServiceJMSrv;
import cn.jmicro.mng.api.INI8NMngJMSrv;
import cn.jmicro.mng.i18n.I18NManager;
import cn.jmicro.mng.vo.I18nJRso;

@Component
@Service(external=true,showFront=false,version="0.0.1",logLevel=MC.LOG_NO)
public class I8NServiceImpl implements II8NServiceJMSrv {

	@Inject
	private I18NManager im;
	
	@Inject
	private IObjectFactory of;
	
	private CRUDService<I18nJRso> crudSrv = null;
	
	public void jready() {
		ComponentIdServer idGenerator = of.get(ComponentIdServer.class);
		IObjectStorage os = of.get(IObjectStorage.class);
		crudSrv = new CRUDService<>(os,idGenerator,INI8NMngJMSrv.TABLE);
	}
	
	@Override
	@SMethod(needLogin=false)
	public RespJRso<Map<String, String>> keyValues(String module, String lang,int clientId) {
		RespJRso<Map<String, String>> resp = new RespJRso<>();
		resp.setCode(RespJRso.CODE_SUCCESS);
		QueryJRso qry = new QueryJRso();
		qry.setCurPage(0);
		qry.setPageSize(Integer.MAX_VALUE);
		
		QryDefJRso mf = new QryDefJRso();
		mf.setFn("mod");
		mf.setOpType(QryDefJRso.OP_EQ);
		mf.setV(module);
		qry.getPs().add(mf);
		
		String[] ar = lang.split("-");
		
		mf = new QryDefJRso();
		mf.setFn("lan");
		mf.setOpType(QryDefJRso.OP_EQ);
		mf.setV(ar[0].trim().toLowerCase());
		qry.getPs().add(mf);
		
		if(ar.length >=2) {
			mf = new QryDefJRso();
			mf.setFn("country");
			mf.setOpType(QryDefJRso.OP_EQ);
			mf.setV(ar[1].trim().toLowerCase());
			qry.getPs().add(mf);
		}
		
		mf = new QryDefJRso();
		mf.setFn("clientId");
		mf.setOpType(QryDefJRso.OP_EQ);
		mf.setV(clientId);
		qry.getPs().add(mf);
		
		RespJRso<List<I18nJRso>> rr = crudSrv.query(I18nJRso.class, qry);
		
		if(rr.getCode() != RespJRso.CODE_SUCCESS) {
			resp.setCode(rr.getCode());
			resp.setMsg(rr.getMsg());
			return resp;
		}
		
		Map<String, String> ps = new HashMap<>();
		resp.setData(ps);
		resp.setCode(RespJRso.CODE_SUCCESS);
		
		if(rr.getData() != null) {
			for(I18nJRso i8 : rr.getData()) {
				ps.put(i8.getKey().toLowerCase(), i8.getVal());
			}
		}
		
		return resp;
	}

}
