package cn.jmicro.api.monitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class MonitorTypesManager {

	private final static Logger logger = LoggerFactory.getLogger(MonitorTypesManager.class);
	
	public static final String PATH_EXCAPE = "^$^";
	
	@Inject
	private IDataOperator op;
	
	private Set<MCConfig> configs = new HashSet<>();
	
	private boolean enable = false;
	
	public void ready() {
		if(!op.exist(Config.CurCustomTypeCodeDir)) {
			op.createNodeOrSetData(Config.CurCustomTypeCodeDir, MC.KEEP_MAX_VAL+"", 
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
		
		op.addChildrenListener(Config.MonitorTypeConfigDir, (type,parent,val,data)->{
			if(IListener.ADD == type) {
				addType0(val);
			}else if(IListener.REMOVE ==type) {
				removeType0(val);
			}
		});
	}
	
	private void insertSystemConfig() {
		Set<String> cfs = op.getChildren(Config.MonitorTypeConfigDir, false);
		Set<MCConfig> exists = new HashSet<>();
		for(String data : cfs) {
			data = data.replaceAll(PATH_EXCAPE, "/");
			MCConfig mcc = JsonUtils.getIns().fromJson(data, MCConfig.class);
			exists.add(mcc);
		}
		
		for(MCConfig mc : MC.MC_CONFIGS) {
			if(!exists.contains(mc)) {
				String data = JsonUtils.getIns().toJson(mc);
				String path = Config.MonitorTypeConfigDir + "/" + data;
				op.createNodeOrSetData(path, "", IDataOperator.PERSISTENT);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Set<MCConfig> getAll() {
		checkStatu();
		
		if(configs.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		Set<MCConfig> set = new HashSet<>();
		set.addAll(this.configs);
		return set;
	}
	
	public boolean createMConfig(MCConfig cfg) {
		checkStatu();
		
		if(StringUtils.isEmpty(cfg.getFieldName())) {
			return false;
		}
		
		MCConfig ecf = this.getByFieldName(cfg.getFieldName());
		if(ecf != null) {
			return false;
		}
		
		short type = Short.parseShort(op.getData(Config.CurCustomTypeCodeDir));
		type++;
		op.setData(Config.CurCustomTypeCodeDir, type+"");
		
		cfg.setType(type);
		
		if(StringUtils.isEmpty(cfg.getGroup())) {
			cfg.setGroup(MC.TYPE_DEF_GROUP);
		}
		
		String val = JsonUtils.getIns().toJson(cfg);
		if(val.contains("/")) {
			val = val.replaceAll("/", PATH_EXCAPE);
		}
		
		String path = Config.MonitorTypeConfigDir + "/" + val;
		op.createNodeOrSetData(path, "", IDataOperator.PERSISTENT);
		return true;
	}
	
	public MCConfig getByFieldName(String fieldName) {
		for(MCConfig m : this.configs) {
			if(fieldName.equals(m.getFieldName())) {
				return m;
			}
		}
		return null;
	}

	public boolean createMConfig(String fieldName,Short type,String label,String desc) {
		checkStatu();
		MCConfig mc = new MCConfig();
		mc.setDesc(desc);
		mc.setFieldName(fieldName);
		mc.setLabel(label);
		mc.setType(type);
		
		return createMConfig(mc);
	}
	
	public boolean updateMConfig(MCConfig cfg) {
		checkStatu();
		MCConfig emc = getByType(cfg.getType());
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
		MCConfig emc = getByType(type);
		if(emc == null) {
			logger.error("Type not exist: " + type);
			return false;
		}
		
		String ejson = JsonUtils.getIns().toJson(emc);
		if(ejson.contains("/")) {
			ejson = ejson.replaceAll("/", PATH_EXCAPE);
		}
		op.deleteNode(Config.MonitorTypeConfigDir + "/" + ejson);
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
		
		MCConfig mcc = JsonUtils.getIns().fromJson(val, MCConfig.class);
		if(configs.contains(mcc)) {
			configs.remove(mcc);
		}
	}

	private void addType0(String val) {
		if(StringUtils.isEmpty(val)) {
			return;
		}
		
		val = val.replaceAll(PATH_EXCAPE, "/");
		
		MCConfig mcc = JsonUtils.getIns().fromJson(val, MCConfig.class);
		if(!configs.add(mcc)) {
			MCConfig emc = getByType(mcc.getType());
			if(emc != null) {
				emc.setDesc(mcc.getDesc());
				emc.setFieldName(mcc.getFieldName());
				emc.setLabel(mcc.getLabel());
			} else {
				configs.add(mcc);
			}
		}
	}

	public MCConfig getByType(short type) {
		for(MCConfig c : this.configs) {
			if(c.getType() == type) {
				return c;
			}
		}
		return null;
	}
	
}
