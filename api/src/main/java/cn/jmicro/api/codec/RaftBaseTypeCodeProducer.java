package cn.jmicro.api.codec;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@Component(value="raftBaseTypeCodeProducer", level=1)
public class RaftBaseTypeCodeProducer implements ITypeCodeProducer {

	private static final Logger logger = LoggerFactory.getLogger(RaftBaseTypeCodeProducer.class);
	
	private static final String ROOT = Config.BASE_DIR + "/" + RaftBaseTypeCodeProducer.class.getSimpleName();
	
	private Map<String,Short> name2Types = new HashMap<>();
	
	private boolean enableWork = false;
	
	private String typeProducer;
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private ILockerManager lm;
	
	@Inject
	private Config cfg;
	
	private IChildrenListener typeListener = (type,parent,name,data) -> {
		if(type == IListener.ADD) {
			addType(name,data);
		}else if(type == IListener.REMOVE) {
			removeType(name);
		}
	};
	
	public void ready() {
		typeProducer = cfg.getString(Constants.TYPE_CODE_PRODUCER, null);
		if(StringUtils.isEmpty(typeProducer) || "raftBaseTypeCodeProducer".equals(typeProducer)) {
			enableWork = true;
			if(!op.exist(ROOT)) {
				op.createNodeOrSetData(ROOT, Short.MIN_VALUE+"", IDataOperator.PERSISTENT);
			}
			op.addChildrenListener(ROOT, typeListener);
		}
		TypeCoderFactory.getIns().setTypeCodeProducer(this);
	}
	
	@Override
	public short getTypeCode(String name) {
		if(!enableWork) {
			throw new CommonException("raftBaseTypeCodeProducer is disable by: "+Constants.TYPE_CODE_PRODUCER + "=" +typeProducer);
		}
		
		if(StringUtils.isEmpty(name)) {
			throw new CommonException("Type code class name cannot be null");
		}
		
		if(name2Types.containsKey(name)) {
			return name2Types.get(name);
		} else {
			return create(name);
		}
	}
	
	private void removeType(String name) {
		logger.warn("Type remove: " + name + "=" + name2Types.get(name));
		name2Types.remove(name);
	}

	private void addType(String name, String data) {
		logger.warn("Type add: " + name + "=" + data);
		name2Types.put(name, Short.parseShort(data));
	}

	private short create(String name) {
		//CountDownLatch cd = new CountDownLatch(1);
		ILockerManager.doInlocker(ROOT, lm, 1000*5, 200, ()->{
			String path = ROOT + "/" + name;
			if(op.exist(path)) {
				String data = op.getData(path);
				short type = Short.parseShort(data);
				name2Types.put(name, type);
			} else {
				short cuType = Short.parseShort(op.getData(ROOT));
				cuType++;
				op.setData(ROOT, cuType+"");
				op.createNodeOrSetData(path, cuType+"", IDataOperator.PERSISTENT);
				
				name2Types.put(name, cuType);
			}
		});
		
		return name2Types.get(name);
	}

}
