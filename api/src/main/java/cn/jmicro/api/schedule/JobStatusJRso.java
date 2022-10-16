package cn.jmicro.api.schedule;

import lombok.Data;
import lombok.Serial;

@Serial
@Data
public class JobStatusJRso {

	private boolean jobExists;
	
	private boolean triggerExists;
	
	private String jobKey;
	
	private String groupName;
	
	private Integer status;
}
