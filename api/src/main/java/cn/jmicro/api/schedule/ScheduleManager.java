package cn.jmicro.api.schedule;

import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.JMethod;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ScheduleManager {
	private final static String JOB_NAME = "JM_JOB_";

	private Scheduler scheduler = null;
	
	private Object sync = new Object();
	
	@Inject
	private ServiceInvokeManager siMng;
	
	@Inject
	private Config cfg;
	
	@Inject
	private IObjectFactory of;

	public RespJRso<Object> job(Byte opCode, ScheduleConfigJRso config) {
		switch(opCode) {
		case Constants.OP_TYPE_ADD:
			return this.createJob(config);
		case Constants.OP_TYPE_DELETE:
			return this.deleteJob(config.getId());
		case Constants.OP_TYPE_PAUSE:
			return this.pauseJob(config);
		case Constants.OP_TYPE_RESUME:
			return this.resumeJob(config);
		case Constants.OP_TYPE_START:
			return this.startJob(config);
		case Constants.OP_TYPE_UPDATE:
			return this.updateJob(config);
		case Constants.OP_TYPE_STATUS:
			return this.jobStatus(config);
		}
		return new RespJRso<>(RespJRso.CODE_FAIL,"无效操作码："+opCode + ",config: " + JsonUtils.getIns().toJson(config));
	}
	
	private RespJRso<Object> jobStatus(ScheduleConfigJRso config) {
		RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
		JobStatusJRso s = new JobStatusJRso();
		r.setData(s);
		try {
			JobKey jk = this.getJobKey(config.getId());
			s.setJobExists(this.scheduler.checkExists(jk));
			s.setJobKey(jk.toString());
			TriggerKey tk = this.getTriggerKey(config.getId());
			s.setTriggerExists(this.scheduler.checkExists(tk));
			s.setGroupName(config.getGroupName());
			TriggerState status = this.scheduler.getTriggerState(tk);
			s.setStatus(TriggerState.NORMAL.name().equals(status.name())?ScheduleConfigJRso.NORMAL:ScheduleConfigJRso.PAUSE);
			r.setCode(RespJRso.CODE_SUCCESS);
		} catch (SchedulerException e) {
			r.setMsg(e.getMessage());
			log.error("",e);
		}
		return r;
	}

	@JMethod(Constants.JMICRO_READY_METHOD_NAME)
	public void initScheduler() {
		if(!cfg.getBoolean("taskJob", false)) {
			return;
		}
		
		if(scheduler != null) return;
		log.info("初始化定时任务");
		synchronized(sync) {
			if(scheduler == null) {
				try {
					scheduler = StdSchedulerFactory.getDefaultScheduler();
					scheduler.setJobFactory(new JMicroJobFactory(of));
					//scheduler = DirectSchedulerFactory.getInstance().getScheduler();
					scheduler.start();
				} catch (SchedulerException e) {
					throw new CommonException("", e);
				}
			}
		}
		
		//系统账号登录监听
		of.addLoginStatusListener((status,ai)->{
			try {
				loadAndStartJobs();
			} catch (Throwable e) {
				log.error("加载任务数据错误，请确保任务配置服务已经启动",e.getMessage());
			}
		});
		
	}

	private void loadAndStartJobs() {
		QueryJRso qry = new QueryJRso();
		qry.setCurPage(1);
		qry.setSize(Integer.MAX_VALUE);
		qry.getPs().put("enable", true);
		qry.getPs().put("status", ScheduleConfigJRso.NORMAL);
		qry.getPs().put("clientId", Config.getClientId());
		
		//cn.jmicro.mng.api.IScheduleJobServiceJMSrv.queryJobs
		siMng.call(-1467520218, new Object[] {qry})
		.then((rst,f,cxt)->{
			if(f != null) {
				log.error("loadAndStartJobs加载任务配置错误：" + f.toString());
				return;
			}
			
			RespJRso<List<ScheduleConfigJRso>> r = (RespJRso<List<ScheduleConfigJRso>>)rst;
			if(r.getCode() != RespJRso.CODE_SUCCESS) {
				log.error("loadAndStartJobs加载任务配置失败：" + r.getMsg());
				return;
			}
			
			if(r.getData() == null || r.getData().isEmpty()) {
				log.info("loadAndStartJobs无定时任务配置");
				return;
			}
			
			r.getData().forEach(e->{
				this.createJob(e);
			});
			
		});
		
	}

	public TriggerKey getTriggerKey(Integer jobId) {
		return TriggerKey.triggerKey(JOB_NAME + jobId);
	}

	public JobKey getJobKey(Integer jobId) {
		return JobKey.jobKey(JOB_NAME + jobId);
	}

	public CronTrigger getCronTrigger(Integer jobId) {
		try {
			//initScheduler();
			CronTrigger t = (CronTrigger) scheduler.getTrigger(getTriggerKey(jobId));
			return t;
		} catch (SchedulerException e) {
			throw new CommonException("获取定时任务CronTrigger出现异常", e);
		}
	}

	public RespJRso<Object> createJob(ScheduleConfigJRso config) {
		 RespJRso<Object> r =RespJRso.d(RespJRso.CODE_FAIL,false);
		try {
			
			//initScheduler();
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl == null) {
				cl = ScheduleManager.class.getClassLoader();
			}

			@SuppressWarnings("unchecked")
			Class<? extends Job> jobClass = (Class<? extends Job>) cl.loadClass(config.getJobClassName());
			if(jobClass == null) {
				r.setMsg("定时任务类加载失败： "+config.getJobClassName());
				return r;
			}

			JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(config.getId())).build();

			CronTrigger trigger = createTrigger(config);

			jobDetail.getJobDataMap().put(ScheduleConfigJRso.PARAM_KEY, config.getExt());

			log.info("创建定时任务：" + config.getId()+",jobClass:"+config.getJobClassName());
			scheduler.scheduleJob(jobDetail, trigger);

			if(ScheduleConfigJRso.PAUSE == config.getStatus()) {
				r = pauseJob(config);
			} else {
				r.setCode(RespJRso.CODE_SUCCESS);
				r.setData(true);
			}
		} catch (SchedulerException | ClassNotFoundException e) {
			//throw new CommonException("创建定时任务失败", e);
			log.error("createJob:" + JsonUtils.getIns().toJson(config),e);
			r.setMsg(e.getMessage());
		}
		return r;
	}
	
	private CronTrigger createTrigger(ScheduleConfigJRso config) {
		CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(config.getCron())
				.withMisfireHandlingInstructionDoNothing();
		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(config.getId()))
				.withSchedule(scheduleBuilder).build();
		return trigger;
	}

	public RespJRso<Object> updateJob(ScheduleConfigJRso jobConfig) {
		RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
		try {
			//initScheduler();
			
			if(!this.scheduler.checkExists(this.getJobKey(jobConfig.getId()))) {
				return this.createJob(jobConfig);
			}
			
			TriggerKey triggerKey = getTriggerKey(jobConfig.getId());

			CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(jobConfig.getCron())
					.withMisfireHandlingInstructionDoNothing();

			CronTrigger trigger = getCronTrigger(jobConfig.getId());
			if(trigger == null) {
				trigger = createTrigger(jobConfig);
			}

			trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();

			trigger.getJobDataMap().put(ScheduleConfigJRso.PARAM_KEY, jobConfig.getExt());

			scheduler.rescheduleJob(triggerKey, trigger);
			if(ScheduleConfigJRso.PAUSE == jobConfig.getStatus()) {
				r = pauseJob(jobConfig);
			} else {
				r.setCode(RespJRso.CODE_SUCCESS);
				r.setData(true);
			}

		} catch (SchedulerException e) {
			//throw new CommonException("更新定时任务失败", e);
			log.error("updateJob"+JsonUtils.getIns().toJson(jobConfig), e);
			r.setMsg(e.getMessage());
		}
		return r;
	}

	public RespJRso<Object>  startJob(ScheduleConfigJRso jobConfig) {
		RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
		try {
			//initScheduler();
			
			if(!this.scheduler.checkExists(this.getJobKey(jobConfig.getId()))) {
				RespJRso<Object> rr = this.createJob(jobConfig);
				if(rr == null || rr.getCode() != RespJRso.CODE_SUCCESS) {
					return rr;
				}
			}
			
			JobDataMap dataMap = new JobDataMap();
			dataMap.put(ScheduleConfigJRso.PARAM_KEY, jobConfig.getExt());
			scheduler.triggerJob(getJobKey(jobConfig.getId()), dataMap);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		} catch (SchedulerException e) {
			log.error("startJob"+JsonUtils.getIns().toJson(jobConfig), e);
			r.setMsg(e.getMessage());
		}
		return r;
	}

	public RespJRso<Object>  pauseJob(ScheduleConfigJRso cfg) {
		RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
		try {
			//initScheduler();
			JobKey key = this.getJobKey(cfg.getId());
			if(!this.scheduler.checkExists(key)) {
				r.setMsg("pauseJob任务不存在：" + cfg.getId());
				r.setCode(RespJRso.CODE_SUCCESS);
				return r;
			}
			
			scheduler.pauseJob(key);
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		} catch (SchedulerException e) {
			//throw new CommonException("暂停定时任务失败", e);
			log.error("pauseJob"+cfg.getId(), e);
			r.setMsg(e.getMessage());
		}
		return r;
	}


	public RespJRso<Object>  resumeJob(ScheduleConfigJRso cfg) {
		RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
		try {
			//initScheduler();
			
			if(!this.scheduler.checkExists(this.getJobKey(cfg.getId()))) {
				return this.createJob(cfg);
			}
			
			scheduler.resumeJob(getJobKey(cfg.getId()));
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		} catch (SchedulerException e) {
			//throw new CommonException("暂停定时任务失败", e);
			log.error("resumeJob"+cfg.getId(), e);
			r.setMsg(e.getMessage());
		}
		return r;
	}

	public RespJRso<Object>  deleteJob(Integer jobId) {
		RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
		try {
			//initScheduler();
			scheduler.deleteJob(getJobKey(jobId));
			r.setCode(RespJRso.CODE_SUCCESS);
			r.setData(true);
		} catch (SchedulerException e) {
			//throw new CommonException("删除定时任务失败", e);
			log.error("resumeJob"+jobId, e);
			r.setMsg(e.getMessage());
		}
		return r;
	}
}
