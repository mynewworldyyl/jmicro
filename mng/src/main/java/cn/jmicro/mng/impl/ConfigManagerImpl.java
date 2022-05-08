package cn.jmicro.mng.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.mng.ConfigNodeJRso;
import cn.jmicro.api.mng.IConfigManagerJMSrv;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.common.util.StringUtils;
import cn.jmicro.mng.Namespace;

@Component
@Service(version="0.0.1",retryCnt=0,external=true,timeout=10000,debugMode=1,
showFront=false,logLevel=MC.LOG_NO,namespace=Namespace.NS)
public class ConfigManagerImpl implements IConfigManagerJMSrv {

	private final static Logger logger = LoggerFactory.getLogger(ConfigManagerImpl.class);
	
	private final String CFG_PREFIX_PATTERN = Config.BASE_DIR+"\\d+/config/[a-zA-Z][a-zA-Z0-9_]*/";
	
	private final String GCFG_PREFIX_PATTERN = Config.BASE_DIR+"\\d+/grobalConfig/";
	
	private final String[] DISABLE_VIEWS = new String[] {
			"ZKRegistry","mongodb","gatewayModel","NettyClientSessionManager/waitBeforeCloseSession"
			,"nettyHttpPort"
	};
	
	private final String[] GDISABLE_VIEWS = new String[] {
			"ComponentIdServer","binaryWebsocketContextPath","OnePrefixTypeEncoder"
	};
	
	private final String[] DISABLE_UPDATES = new String[] {
			"/NettyHttpServerHandler"
	};
	
	private final String[] DISABLE_DELETES = new String[] {
			"/NettyHttpServerHandler"
	};
	
	private Pattern[] DISABLE_VIEWS_PTNS = new Pattern[0];
	
	@Inject
	private IDataOperator op;
	
	public void init() {
		DISABLE_VIEWS_PTNS = new Pattern[DISABLE_VIEWS.length+GDISABLE_VIEWS.length];
		for(int i = 0; i < DISABLE_VIEWS.length; i++) {
			DISABLE_VIEWS_PTNS[i] = Pattern.compile(CFG_PREFIX_PATTERN + DISABLE_VIEWS[i]);
		}
		
		for(int i = 0 ; i < GDISABLE_VIEWS.length; i++) {
			DISABLE_VIEWS_PTNS[i+DISABLE_VIEWS.length] = Pattern.compile(GCFG_PREFIX_PATTERN + GDISABLE_VIEWS[i]);
		}
	}
	
	@Override
	@SMethod(perType=true,needLogin=true,maxSpeed=10,maxPacketSize=512,logLevel=MC.LOG_NO)
	public RespJRso<ConfigNodeJRso[]> getChildren(String path,Boolean getAll) {
		RespJRso<ConfigNodeJRso[]> r = new RespJRso<>();
		if(!PermissionManager.isCurAdmin(Config.getClientId())) {
			path = Config.getRaftBasePathByClientId(JMicroContext.get().getAccount().getClientId(),"");
		}
		ConfigNodeJRso[] rst = getChildren0(path,getAll,PermissionManager.isCurAdmin(Config.getClientId()));
		r.setCode(RespJRso.CODE_SUCCESS);
		r.setData(rst);
		return r;
	}
	
	public ConfigNodeJRso[] getChildren0(String path,Boolean getAll,boolean isAdmin) {
		Set<String> clist = op.getChildren(path, false);
		if(clist == null || clist.isEmpty()) {
			return null;
		}
		
		List<ConfigNodeJRso> l = new ArrayList<>();
		
		for(String p : clist) {
			
			String fp ="/".equals(path) ? "/"+p : path+"/"+p;
			if(!isAdmin) {
				if(!canView(fp)) {
					continue;
				}
			}
			
			String val = op.getData(fp);
			if(val == null) {
				val = "";
			}
			
			ConfigNodeJRso cn = new ConfigNodeJRso(fp,val,p);
			
			if(getAll) {
				cn.setChildren(this.getChildren0(fp,getAll,isAdmin));
			}
			
			l.add(cn);
		}
		
		ConfigNodeJRso[] children = new ConfigNodeJRso[l.size()];
		l.toArray(children);
		
		return children;
	}

	private boolean canView(String fp) {
		/*if(fp.endsWith("ZKRegistry")) {
			logger.info("Debug: " + fp);
		}*/
		for(Pattern p : DISABLE_VIEWS_PTNS) {
			//String pp = parent+p;
			if(p.matcher(fp).find()) {
				return false;
			}
		}
		return true;
	}

	@Override
	@SMethod(perType=true,maxSpeed=10,maxPacketSize=1024)
	public boolean update(String path, String val) {
		try {
			if(!PermissionManager.isCurAdmin(Config.getClientId())) {
				if(!canView(path)) {
					LG.log(MC.LOG_ERROR, this.getClass(), "Permission reject to update ["+path + "="+val+"] by account: "+JMicroContext.get().getAccount().getActName());
					return false;
				}
			}
			op.setData(path, val);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
	}

	@Override
	@SMethod(perType=true,maxSpeed=10,maxPacketSize=256)
	public boolean delete(String path) {
		try {
			if(!PermissionManager.isCurAdmin(Config.getClientId())) {
				if(!canView(path)) {
					LG.log(MC.LOG_ERROR, this.getClass(), "Permission reject to delete ["+path + "] by account: "+JMicroContext.get().getAccount().getActName());
					return false;
				}
			}
			op.deleteNode(path);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	@SMethod(perType=true,maxSpeed=10,maxPacketSize=2048)
	public boolean add(String path, String val,Boolean isDir) {
		try {
			if(!PermissionManager.isCurAdmin(Config.getClientId())) {
				if(!canView(path)) {
					LG.log(MC.LOG_ERROR, this.getClass(), "Permission reject to add ["+path +"="+ val+"] by account: "+JMicroContext.get().getAccount().getActName());
					return false;
				}
			}
			
			if(isDir && val == null) {
				val = "";
			}else if(StringUtils.isEmpty(val)) {
				return false;
			}
			
			op.createNodeOrSetData(path, val, false);
			if(isDir) {
				//有子结点才是目录结点，否则作为叶子结点造成不能再往里增加子结点
				op.createNodeOrSetData(path+"/ip", Config.getExportSocketHost(), false);
			}
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	
}
