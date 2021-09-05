package cn.jmicro.mng.impl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.DataBlockJRso;
import cn.jmicro.api.net.UploadFileManager;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.ext.mongodb.CRUDService;
import cn.jmicro.mng.api.INI8NMngJMSrv;
import cn.jmicro.mng.vo.I18nJRso;
import lombok.extern.slf4j.Slf4j;

@Component
@Service(version = "0.0.1", debugMode = 1,logLevel=MC.LOG_NO,timeout=10000,
monitorEnable=0, retryCnt = 0, external=true, showFront=false)
@Slf4j
public class NI8NMngJMSrvImpl implements INI8NMngJMSrv{
	
	@Inject
	private IObjectFactory of;
	
	@Inject
	private UploadFileManager uploadFileMng;
	
	private CRUDService<I18nJRso> crudSrv = null;

	public void jready() {
		ComponentIdServer idGenerator = of.get(ComponentIdServer.class);
		IObjectStorage os = of.get(IObjectStorage.class);
		crudSrv = new CRUDService<>(os,idGenerator,INI8NMngJMSrv.TABLE);
	}
	
	@Override
	@SMethod(maxPacketSize=1024*1024*1,perType=true,needLogin=true,maxSpeed=5)
	public IPromise<RespJRso<DataBlockJRso>> uploadFile(byte[] data) {
		return new PromiseImpl<RespJRso<DataBlockJRso>>((suc,fail)->{
			RespJRso<DataBlockJRso> r = uploadFileMng.addResourceData(data, this::importData);
			suc.success(r);
		});
	}
	
	/**
	 * fileName格式
	 * name.properties  @see INI8NMngJMSrv.DEF_LAN @see INI8NMngJMSrv.DEF_CONTRIY
	 * name_zh.properties
	 * name_zh-CN.properties
	 * name_zh-HK.properties
	 * 
	 * @param fileName
	 */
	private RespJRso<DataBlockJRso> importData(DataBlockJRso db) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		BufferedReader br = null;
		RespJRso<DataBlockJRso> r = new RespJRso<>(RespJRso.CODE_FAIL);
		if(Utils.isEmpty(db.getFilePath())) {
			r.setMsg("FileName cannot be NULL");
			return r;
		}
		
		log.info("user:{} filename:{}, path: {}",ai.getId(),db.getFileName(),db.getFilePath());
		
		String lan =  INI8NMngJMSrv.DEF_LAN;
		String con =  INI8NMngJMSrv.DEF_CONTRIY;
		
		if(!Utils.isEmpty(db.getLang())) {
			lan = db.getLang();
		}
		
		if(!Utils.isEmpty(db.getCountry())) {
			con = db.getCountry();
		}
		
		/*String fn = db.getFileName();
		
		int idx = fn.indexOf("_");
		if(idx > 0) {
			fn = fn.substring(idx+1);
		}
		
		idx = fn.lastIndexOf(".");
		if(idx > 0) {
			fn = fn.substring(0,idx);
		}
		
		if(!Utils.isEmpty(fn)) {
			idx = fn.indexOf("-");
			if(idx > 0) {
				//zh-CN
				String[] na = fn.split("-");
				lan = na[0];
				con = na[1];
			} else if(!db.getFileName().startsWith(fn)) {//排除name.properties中fn==name
				//zh
				con = fn;
			}
		}*/
		
		try {
			
			lan = lan.trim().toLowerCase();
			con = con.trim().toLowerCase();
			
			StringBuffer sb = new StringBuffer();
			Map<String,Object> filter = new HashMap<>();
			filter.put("lan", lan);
			filter.put("country", con);
			filter.put("clientId", db.getClientId());
			filter.put("mod", db.getMod());
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(db.getFilePath()),Constants.CHARSET));
			String l = null;
			while((l = br.readLine()) != null) {
				l = l.trim();
				if("".equals(l)) continue;
				String de = "";
				if(l.startsWith("#")) {
					//注释作为下一行备注
					StringBuffer desc = new StringBuffer(l);
					while((l = br.readLine()) != null) {
						if("".equals(l)) continue;
						if(l.startsWith("#")) {//多行注释
							desc.append("\n").append(l);
						} else {
							break;
						}
					}
					
					if(l == null) {
						break;//行尾结束
					}
					de = desc.toString();
				}
				
				String[] ar = l.split("=");
				if(ar == null || ar.length != 2 || Utils.isEmpty(ar[1])) {
					log.error("Invalid format: {}",l);
					sb.append(l).append("无效格式\n");
					continue;
				}
				
				String k = ar[0];
				if(Utils.isEmpty(k)) {
					sb.append(l).append("Key空值\n");
					continue;
				}
				
				k = k.trim();
				
				filter.put("key", k);
				
				I18nJRso v = crudSrv.getOne(I18nJRso.class, filter);
				if(v != null) {
					if(!v.getVal().equals(ar[1]) || !de.equals(v.getDesc())) {
						v.setVal(ar[1]);
						v.setUpdatedBy(ai.getId());
						v.setDesc(de);
						crudSrv.updateById(I18nJRso.class, v);
					}
				} else {
					v = new I18nJRso();
					v.setDesc(de);
					v.setClientId(db.getClientId());
					v.setCountry(con);
					v.setCreatedBy(ai.getId());
					v.setKey(k);
					v.setLan(lan);
					v.setUpdatedBy(ai.getId());
					v.setVal(ar[1]);
					v.setMod(db.getMod());
					crudSrv.add(I18nJRso.class, v);
				}
			}
			r.setMsg(sb.toString());
			r.setCode(RespJRso.CODE_SUCCESS);
			
		}catch(Throwable e) {
			log.info("",e);
			r.setMsg(e.getMessage());
			return r;
		}finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return r;
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public IPromise<RespJRso<Boolean>> add(I18nJRso vo) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		return new PromiseImpl<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
			
			RespJRso<Boolean> rr =  checkPermission(vo);
			if(rr != null) {
				suc.success(rr);
				return;
			}
			
			vo.setCreatedBy(ai.getId());
			vo.setUpdatedBy(ai.getId());
			
			if(Utils.isEmpty(vo.getLan())) {
				r.setMsg("语言选项缺失");
				log.error(r.getMsg()+" : " + JsonUtils.getIns().toJson(vo));
				suc.success(r);
				return;
			}
			
			if(Utils.isEmpty(vo.getCountry())) {
				r.setMsg("国别选项缺失");
				log.error(r.getMsg()+" : " + JsonUtils.getIns().toJson(vo));
				suc.success(r);
				return;
			}
			
			vo.setLan(vo.getLan().trim().toLowerCase());
			vo.setCountry(vo.getCountry().trim().toLowerCase());
			vo.setKey(vo.getKey().trim());
			
			boolean s = crudSrv.add(I18nJRso.class, vo);
			if(!s) {
				r.setMsg("新增失败");
				log.error(r.getMsg()+" : " + JsonUtils.getIns().toJson(vo));
				suc.success(r);
				return;
			}
			
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(s);
			suc.success(r);
		});
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public IPromise<RespJRso<Boolean>> delete(Long id) {
		return new PromiseImpl<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
			
			I18nJRso vo = crudSrv.getById(I18nJRso.class, id);
			if(vo == null) {
				r.setMsg("Data not found");
				suc.success(r);
				return;
			}
			
			RespJRso<Boolean> rr =  checkPermission(vo);
			if(rr != null) {
				suc.success(rr);
				return;
			}
			
			boolean s = crudSrv.deleteById(I18nJRso.class, id);
			r.setData(s);
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
		});
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public IPromise<RespJRso<Boolean>> update(I18nJRso vo) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		return new PromiseImpl<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
			RespJRso<Boolean> rr =  checkPermission(vo);
			if(rr != null) {
				suc.success(rr);
				return;
			}
			
			vo.setUpdatedBy(ai.getId());
			boolean s = crudSrv.updateById(I18nJRso.class, vo);
			r.setData(s);
			r.setCode(RespJRso.CODE_SUCCESS);
			suc.success(r);
		});
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public IPromise<RespJRso<List<I18nJRso>>> list(QueryJRso qry) {
		return new PromiseImpl<RespJRso<List<I18nJRso>>>((suc,fail)->{
			RespJRso<List<I18nJRso>> r = crudSrv.query(I18nJRso.class, qry);
			suc.success(r);
		});
	}

	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=5,maxPacketSize=1024)
	public IPromise<RespJRso<I18nJRso>> detail(Long id) {
		return new PromiseImpl<RespJRso<I18nJRso>>((suc,fail)->{
			RespJRso<I18nJRso> r = new RespJRso<>(RespJRso.CODE_SUCCESS);
			I18nJRso o = crudSrv.getById(I18nJRso.class, id);
			r.setData(o);
			suc.success(r);
		});
	}
	
	
	private  RespJRso<Boolean> checkPermission(I18nJRso vo) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
		if(vo.getClientId()<= 0) {
			r.setMsg("无效clientId");
			log.error(r.getMsg()+" : " + JsonUtils.getIns().toJson(vo));
			return r;
		}
		
		if(vo.getClientId() != ai.getClientId()) {
			if(!PermissionManager.isCurAdmin()) {
				r.setMsg("语言选项缺失");
				log.error(r.getMsg()+" : " + JsonUtils.getIns().toJson(vo));
				return r;
			}
		}
		
		return null;
		
	}

}
