package cn.jmicro.mng.api;

import java.util.List;

import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.schedule.JobStatusJRso;
import cn.jmicro.api.schedule.ScheduleConfigJRso;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IScheduleJobServiceJMSrv {

	RespJRso<List<ScheduleConfigJRso>> queryJobs(QueryJRso qry);

	IPromise<RespJRso<ScheduleConfigJRso>> saveJob(ScheduleConfigJRso job);
	
	IPromise<RespJRso<Boolean>> updateJob(ScheduleConfigJRso scheduleJob);

	IPromise<RespJRso<Boolean>> deleteJob(Integer  jid);

	IPromise<RespJRso<Object>> start(Integer jid);
	
	IPromise<RespJRso<Object>> pause(Integer jid);
	
	IPromise<RespJRso<Object>> resume(Integer jid);
	
	IPromise<RespJRso<JobStatusJRso>> jobStatus(Integer jid);
	
}
