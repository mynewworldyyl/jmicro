package cn.jmicro.gateway.fs;

import com.mongodb.gridfs.GridFS;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;

@Component
public class FileManager {

	@Inject(required=false)
	private GridFS fs;
	
	public void jready() {
		
	}
	
	
	
}
