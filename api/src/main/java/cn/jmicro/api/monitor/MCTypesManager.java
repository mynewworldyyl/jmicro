package cn.jmicro.api.monitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class MCTypesManager {

	private final static Logger logger = LoggerFactory.getLogger(MCTypesManager.class);
	
	@Inject
	private IDataOperator op;
	
	private Set<MCConfigJRso> configs = new HashSet<>();
	
	private boolean enable = false;
	
	public void jready() {
		if(!op.exist(Config.getRaftBasePath(Config.CurCustomTypeCodeDir))) {
			op.createNodeOrSetData(Config.getRaftBasePath(Config.CurCustomTypeCodeDir), MC.KEEP_MAX_VAL+"", 
					IDataOperator.PERSISTENT);
		}
	}
	
	public void enable() {
		if(enable) {
			logger.warn("MonitorTypesManager has been enable");
			return;
		}
		enable = true;
		
		insertSystemConfig();
		
		op.addChildrenListener(Config.getRaftBasePath(Config.MonitorTypeConfigDir), (type,parent,val)->{
			if(IListener.ADD == type) {
				addType0(val);
			}else if(IListener.REMOVE ==type) {
				removeType0(val);
			}
		});
	}
	
	private void insertSystemConfig() {
		Set<String> cfs = op.getChildren(Config.getRaftBasePath(Config.MonitorTypeConfigDir), false);
		Set<MCConfigJRso> exists = new HashSet<>();
		for(String data : cfs) {
			data = data.replaceAll(Constants.PATH_EXCAPE, "/");
			MCConfigJRso mcc = JsonUtils.getIns().fromJson(data, MCConfigJRso.class);
			exists.add(mcc);
		}
		
		for(MCConfigJRso mc : MC.MC_CONFIGS) {
			if(!exists.contains(mc)) {
				String data = JsonUtils.getIns().toJson(mc);
				String path = Config.getRaftBasePath(Config.MonitorTypeConfigDir) + "/" + data;
				op.createNodeOrSetData(path, "", IDataOperator.PERSISTENT);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Set<MCConfigJRso> getAll() {
		checkStatu();
		
		if(configs.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		Set<MCConfigJRso> set = new HashSet<>();
		set.addAll(this.configs);
		return set;
	}
	
	public boolean createMConfig(MCConfigJRso cfg) {
		checkStatu();
		
		if(StringUtils.isEmpty(cfg.getFieldName())) {
			return false;
		}
		
		MCConfigJRso ecf = this.getByFieldName(cfg.getFieldName());
		if(ecf != null) {
			return false;
		}
		
		short type = Short.parseShort(op.getData(Config.getRaftBasePath(Config.CurCustomTypeCodeDir)));
		type++;
		op.setData(Config.getRaftBasePath(Config.CurCustomTypeCodeDir), type+"");
		
		cfg.setType(type);
		
		if(StringUtils.isEmpty(cfg.getGroup())) {
			cfg.setGroup(MC.TYPE_DEF_GROUP);
		}
		
		String val = JsonUtils.getIns().toJson(cfg);
		if(val.contains("/")) {
			val = val.replaceAll("/", Constants.PATH_EXCAPE);
		}
		
		String path = Config.getRaftBasePath(Config.MonitorTypeConfigDir) + "/" + val;
		op.createNodeOrSetData(path, "", IDataOperator.PERSISTENT);
		return true;
	}
	
	public MCConfigJRso getByFieldName(String fieldName) {
		for(MCConfigJRso m : this.configs) {
			if(fieldName.equals(m.getFieldName())) {
				return m;
			}
		}
		return null;
	}

	public boolean createMConfig(String fieldName,Short type,String label,String desc) {
		checkStatu();
		MCConfigJRso mc = new MCConfigJRso();
		mc.setDesc(desc);
		mc.setFieldName(fieldName);
		mc.setLabel(label);
		mc.setType(type);
		
		return createMConfig(mc);
	}
	
	public boolean updateMConfig(MCConfigJRso cfg) {
		checkStatu();
		MCConfigJRso emc = getByType(cfg.getType());
		if(emc == null) {
			logger.error("Type not exist when do update: " + cfg.getType());
			return false;
		}
		deleteType(cfg.getType());
		
		emc.setDesc(cfg.getDesc());
		emc.setFieldName(cfg.getFieldName());
		emc.setLabel(cfg.getLabel());
		
		return createMConfig(emc);
	}
	
	public boolean deleteType(Short type) {
		checkStatu();
		MCConfigJRso emc = getByType(type);
		if(emc == null) {
			logger.error("Type not exist: " + type);
			return false;
		}
		
		String ejson = JsonUtils.getIns().toJson(emc);
		if(ejson.contains("/")) {
			ejson = ejson.replaceAll("/", Constants.PATH_EXCAPE);
		}
		op.deleteNode(Config.getRaftBasePath(Config.MonitorTypeConfigDir) + "/" + ejson);
		return true;
	}

    private void checkStatu() {
    	if(!enable) {
			throw new CommonException("MonitorTypesManager is disalbe");
		}
    }

	private void removeType0(String val) {
		if(StringUtils.isEmpty(val)) {
			return;
		}
		
		MCConfigJRso mcc = JsonUtils.getIns().fromJson(val, MCConfigJRso.class);
		if(configs.contains(mcc)) {
			configs.remove(mcc);
		}
	}

	private void addType0(String val) {
		if(StringUtils.isEmpty(val)) {
			return;
		}
		
		val = val.replaceAll(Constants.PATH_EXCAPE, "/");
		
		MCConfigJRso mcc = JsonUtils.getIns().fromJson(val, MCConfigJRso.class);
		if(!configs.add(mcc)) {
			MCConfigJRso emc = getByType(mcc.getType());
			if(emc != null) {
				emc.setDesc(mcc.getDesc());
				emc.setFieldName(mcc.getFieldName());
				emc.setLabel(mcc.getLabel());
			} else {
				configs.add(mcc);
			}
		}
	}

	public MCConfigJRso getByType(short type) {
		for(MCConfigJRso c : this.configs) {
			if(c.getType() == type) {
				return c;
			}
		}
		return null;
	}
	
}
