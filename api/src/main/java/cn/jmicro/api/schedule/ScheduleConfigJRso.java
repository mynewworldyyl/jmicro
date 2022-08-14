package cn.jmicro.api.schedule;

import lombok.Data;

@Data
public class ScheduleConfigJRso {
	
	public static final String TABLE = "t_schedule_job_cfg";
	
	//不实际存在任务
	//public static final int INIT = 1;
	
	//任务可以运行，或等待运行
	public static final int NORMAL = 1;
	
	//任务暂停
	public static final int PAUSE = 2;
	
	//enable=true时，任务销毁
	//public static final int DISTRORY = 3;
	
	//任务参数KEY
    public static final String PARAM_KEY = "JPARAM_KEY";

	private Integer id;
	
	private Integer clientId;

	private String jobClassName;
	
	private String groupName;

	private String ext;
	
	private String cron;

	private Integer status;
	
	//private Boolean enable;
	
	//定时任务接收方法
	private Integer mcode;

	private String desc;
	
	private boolean deleted;

	private long createdTime;

	private long updatedTime;
	
	private int createdBy;

	private int updatedBy;

	
}
