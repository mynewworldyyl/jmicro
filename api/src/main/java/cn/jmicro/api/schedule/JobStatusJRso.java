package cn.jmicro.api.schedule;

import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
public class JobStatusJRso {

	private boolean jobExists;
	
	private boolean triggerExists;
	
	private String jobKey;
	
	private String groupName;
	
	private Integer status;
}
