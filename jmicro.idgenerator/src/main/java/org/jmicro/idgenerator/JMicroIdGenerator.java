package org.jmicro.idgenerator;

import java.util.concurrent.atomic.AtomicLong;

import org.jmicro.api.IIdGenerator;
import org.jmicro.api.annotation.Component;
import org.jmicro.common.Constants;

@Component(Constants.DEFAULT_IDGENERATOR)
public class JMicroIdGenerator implements IIdGenerator {

	//private MysqlBaseIdMap mapper = new MysqlBaseIdMap();
	
	//private IIDGenerator gen = new BaseIDGenerator(mapper,"net.techgy",true);
	
	private AtomicLong idgenerator = new AtomicLong(1);
	
	@Override
	public long getLongId(Class<?> idType) {
		return idgenerator.getAndDecrement();
	}

	@Override
	public String getStringId(Class<?> idType) {
		return idgenerator.getAndDecrement()+"";
	}

}
