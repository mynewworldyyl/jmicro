package cn.jmicro.resource;

import java.util.Map;

import cn.jmicro.api.monitor.AbstractResource;
import cn.jmicro.api.monitor.IResource;
import cn.jmicro.api.monitor.ResourceDataJRso;

public class RedisResource extends AbstractResource  implements IResource {

	@Override
	public ResourceDataJRso getResource(Map<String,Object> params,String expStr) {
		
		return null;
	}

}
