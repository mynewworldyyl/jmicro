package cn.jmicro.mng.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.QueryJRso;
import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.persist.IObjectStorage;
import cn.jmicro.api.schedule.JobStatusJRso;
import cn.jmicro.api.schedule.ScheduleConfigJRso;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.api.security.PermissionManager;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.mng.Namespace;
import cn.jmicro.mng.api.IScheduleJobServiceJMSrv;

@Component(level=20001)
@Service(version="0.0.1", external=true, debugMode=0, showFront=false,logLevel=MC.LOG_NO,namespace=Namespace.NS)
public class ScheduleJobServiceImpl implements IScheduleJobServiceJMSrv {

	private final Logger logger = LoggerFactory.getLogger(ScheduleJobServiceImpl.class);

	@Inject
	private IObjectStorage os;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	@Inject
	private ServiceInvokeManager siMng;
	
	@Override
	public IPromise<RespJRso<JobStatusJRso>> jobStatus(Integer jid) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		return new Promise<RespJRso<JobStatusJRso>>((suc,fail)->{
			RespJRso<JobStatusJRso> r = new RespJRso<>(RespJRso.CODE_FAIL);
			ScheduleConfigJRso ejob = findJob(jid);
			if(ejob == null) {
				r.setMsg("更新任务不存在");
			}
			
			if(ejob.getClientId() != ai.getClientId() && !PermissionManager.isCurDefAdmin()) {
				r.setMsg("权限不足");
			}
			
			try {
				notifySrv(ai,Constants.OP_TYPE_STATUS,ejob, null)
				.success((rr,cxt)->{
					JobStatusJRso js = (JobStatusJRso)rr.getData();
					if(js.getStatus() == ScheduleConfigJRso.NORMAL
							&& ejob.getStatus() != ScheduleConfigJRso.NORMAL) {
						ejob.setStatus(ScheduleConfigJRso.NORMAL);
						os.updateById(ScheduleConfigJRso.TABLE, ejob, ScheduleConfigJRso.class, 
								IObjectStorage._ID, false);
					} else if(js.getStatus() == ScheduleConfigJRso.PAUSE
							&& ejob.getStatus() != ScheduleConfigJRso.PAUSE) {
						ejob.setStatus(ScheduleConfigJRso.PAUSE);
						os.updateById(ScheduleConfigJRso.TABLE, ejob, ScheduleConfigJRso.class, 
								IObjectStorage._ID, false);
					}
					//NORMAL, PAUSED
					r.setData(js);
					r.setCode(rr.getCode());
					r.setMsg(rr.getMsg());
					suc.success(r);
				})
				.fail((code,msg,cxt)->{
					r.setCode(code);
					r.setMsg(msg);
					suc.success(r);
				});
				
			}catch(Throwable e) {
				r.setMsg(e.getMessage());
				logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(ejob));
				suc.success(r);
			}
		});
	}

	@Override
	@SMethod(forType=Constants.FOR_TYPE_ALL,needLogin=true)
	public RespJRso<List<ScheduleConfigJRso>> queryJobs(QueryJRso qry) {

		RespJRso<List<ScheduleConfigJRso>> r = new RespJRso<>();
		
		Map<String,Object> filter = new HashMap<>();
		
		filter.put("deleted", false);
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		if(!PermissionManager.isCurDefAdmin()) {
			if(ai == null) {
				ai = JMicroContext.get().getSysAccount();
				if(ai != null) {
					filter.put("clientId", ai.getClientId());
				}
			} else {
				filter.put("clientId", ai.getClientId());
			}
		}else {
			String key = "clientId";
			if (qry.getPs().containsKey(key)) {
				filter.put(key, qry.getPs().get(key));
			}
		}
		
		String key = "desc";
		if (qry.getPs().containsKey(key)) {
			Map<String,Object> typeN = new HashMap<>();
			typeN.put("$regex", qry.getPs().get(key));
			filter.put("desc", typeN);
		}
		
		key = "mcode";
		if (qry.getPs().containsKey(key)) {
			filter.put(key, qry.getPs().get(key));
		}
		
		key = "jobClassName";
		if (qry.getPs().containsKey(key)) {
			filter.put(key, qry.getPs().get(key));
		}
		
		key = "status";
		if (qry.getPs().containsKey(key)) {
			filter.put(key, qry.getPs().get(key));
		}
		
		int cnt =(int) os.count(ScheduleConfigJRso.TABLE, filter);
		r.setTotal(cnt);
		
		if(cnt > 0) {
			List<ScheduleConfigJRso> list = this.os.query(ScheduleConfigJRso.TABLE, filter, ScheduleConfigJRso.class,
					qry.getSize(), qry.getCurPage()-1,null,qry.getSortName(),
					IObjectStorage.getOrderVal(qry.getOrder(), 1));
			r.setData(list);
		}
		
		r.setCode(RespJRso.CODE_SUCCESS);
		
		return r;
	
	}

	@Override
	public IPromise<RespJRso<ScheduleConfigJRso>> saveJob(ScheduleConfigJRso job) {
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		return new Promise<RespJRso<ScheduleConfigJRso>>((suc,fail)->{
			RespJRso<ScheduleConfigJRso> r = new RespJRso<ScheduleConfigJRso>(RespJRso.CODE_FAIL);
			if(Utils.isEmpty(job.getCron())) {
				r.setMsg("任务配置cron表达式不能为空");
			}
			
			if(Utils.isEmpty(job.getJobClassName())) {
				r.setMsg("目标任务类名不能为空");
			}
			
			if(job.getClientId() == null || job.getClientId() <= 0 
					|| !PermissionManager.isCurDefAdmin(Constants.FOR_TYPE_USER)) {
				job.setClientId(ai.getClientId());
			}
			
			job.setId(this.idGenerator.getIntId(ScheduleConfigJRso.class));
			job.setCreatedBy(ai.getId());
			job.setCreatedTime(TimeUtils.getCurTime());
			job.setUpdatedTime(TimeUtils.getCurTime());
			job.setUpdatedBy(ai.getId());
			job.setDeleted(false);
			job.setStatus(ScheduleConfigJRso.PAUSE);
			r.setData(job);
			
			if(os.save(ScheduleConfigJRso.TABLE, job, ScheduleConfigJRso.class, false)) {
				try {
					try {
						notifySrv(ai,Constants.OP_TYPE_ADD,job, null)
						.success((rst,cxt)->{
							RespJRso rr = (RespJRso)rst;
							r.setCode(rr.getCode());
							r.setMsg(rr.getMsg());
							suc.success(r);
						})
						.fail((code,msg,cxt)->{
							r.setCode(code);
							r.setMsg(msg);
							suc.success(r);
						});
						
					}catch(Throwable e) {
						r.setMsg(e.getMessage());
						logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(job));
						suc.success(r);
					}
					
				}catch(Throwable e) {
					r.setMsg(e.getMessage());
					logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(job));
					suc.success(r);
				}
			}else {
				r.setMsg("保存失败");
				logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(job));
				suc.success(r);
			}
		});
		
		
	}

	/*private void notifySrv(byte opTypeAdd, ScheduleConfigJRso job) {
		logger.info("通知：opTypeAdd"+opTypeAdd+", JOB: " + JsonUtils.getIns().toJson(job));
		siMng.call(job.getMcode(), new Object[] {opTypeAdd,job})
		.then((rst,f,cxt)->{
			if(f != null) {
				logger.error("通知Mcode:" + job.getMcode()+", 失败，"+ f.toString());
			}
		});
	}
*/
	@Override
	public IPromise<RespJRso<Boolean>> updateJob(ScheduleConfigJRso job) {
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
			if(Utils.isEmpty(job.getCron())) {
				r.setMsg("任务配置cron表达式不能为空");
			}
			
			if(Utils.isEmpty(job.getJobClassName())) {
				r.setMsg("目标任务类名不能为空");
			}
			
			ScheduleConfigJRso ejob = findJob(job.getId());
			if(ejob == null) {
				r.setMsg("更新任务不存在");
			}
			
			if(ejob.getClientId() != ai.getClientId() && !PermissionManager.isCurDefAdmin()) {
				r.setMsg("权限不足");
			}
			
			ejob.setUpdatedTime(TimeUtils.getCurTime());
			ejob.setUpdatedBy(ai.getId());
			ejob.setCron(job.getCron());
			ejob.setDesc(job.getDesc());
			ejob.setExt(job.getExt());
			ejob.setJobClassName(job.getJobClassName());
			ejob.setMcode(job.getMcode());
			ejob.setClientId(job.getClientId());
			//ejob.setStatus(job.getStatus());
			
			if(os.updateById(ScheduleConfigJRso.TABLE, ejob, ScheduleConfigJRso.class, IObjectStorage._ID, false)) {
				try {
					notifySrv(ai,Constants.OP_TYPE_UPDATE,job, null)
					.success((rst,cxt)->{
						RespJRso rr = (RespJRso)rst;
						r.setData(rr.getCode() == RespJRso.CODE_SUCCESS);
						r.setCode(rr.getCode());
						r.setMsg(rr.getMsg());
						suc.success(r);
					})
					.fail((code,msg,cxt)->{
						r.setData(false);
						r.setCode(code);
						r.setMsg(msg);
						suc.success(r);
					});
					
				}catch(Throwable e) {
					r.setMsg(e.getMessage());
					logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(ejob));
					suc.success(r);
				}
			} else {
				r.setMsg("更新失败");
				logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(ejob));
				suc.success(r);
			}
		});
	}

	private ScheduleConfigJRso findJob(Integer id) {
		Document f = new Document(IObjectStorage._ID,id);
		return os.getOne(ScheduleConfigJRso.TABLE, f, ScheduleConfigJRso.class);
	}

	@Override
	public IPromise<RespJRso<Boolean>> deleteJob(Integer jid) {
		
		ActInfoJRso ai = JMicroContext.get().getAccount();
		
		return new Promise<RespJRso<Boolean>>((suc,fail)->{
			RespJRso<Boolean> r = new RespJRso<>(RespJRso.CODE_FAIL);
			Document f = new Document(IObjectStorage._ID,jid);
			
			ScheduleConfigJRso ejob = findJob(jid);
			if(ejob == null) {
				r.setMsg("任务不存在");
				suc.success(r);
				return;
			}
			
			if(ejob.getClientId() != ai.getClientId() && !PermissionManager.isCurDefAdmin()) {
				suc.success(r);
				return;
			}
			
			Document up = new Document("deleted",true);
			up.put("updatedBy", ai.getId());
			up.put("updatedTime", TimeUtils.getCurTime());
			
			if(os.update(ScheduleConfigJRso.TABLE, f, new Document("$set",up), ScheduleConfigJRso.class) > 0) {
				try {
					notifySrv(ai,Constants.OP_TYPE_DELETE,ejob, null)
					.success((rst,cxt)->{
						RespJRso rr = (RespJRso)rst;
						r.setData(rr.getCode() == RespJRso.CODE_SUCCESS);
						r.setCode(rr.getCode());
						r.setMsg(rr.getMsg());
						suc.success(r);
					})
					.fail((code,msg,cxt)->{
						r.setData(false);
						r.setCode(code);
						r.setMsg(msg);
						suc.success(r);
					});
					
				}catch(Throwable e) {
					r.setMsg(e.getMessage());
					logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(ejob));
					suc.success(r);
				}
				
			}else {
				r.setMsg("更新失败");
				logger.error(r.getMsg()+": "+JsonUtils.getIns().toJson(ejob));
				suc.success(r);
			}
		});
	}

	@Override
	public IPromise<RespJRso<Object>> start(Integer jid) {
		return notifySrv(JMicroContext.get().getAccount(),Constants.OP_TYPE_START,null, jid);
	}

	@Override
	public IPromise<RespJRso<Object>> pause(Integer jid) {
		return notifySrv(JMicroContext.get().getAccount(),Constants.OP_TYPE_PAUSE,null, jid);
	}

	@Override
	public IPromise<RespJRso<Object>> resume(Integer jid) {
		return notifySrv(JMicroContext.get().getAccount(),Constants.OP_TYPE_RESUME,null, jid);
	}
	
	private IPromise<RespJRso<Object>> notifySrv(ActInfoJRso ai, byte opTypeAdd, final ScheduleConfigJRso job,
			Integer jid) {
		
		return new Promise<RespJRso<Object>>((suc,fail)->{
			RespJRso<Object> r = new RespJRso<>(RespJRso.CODE_FAIL);
			final ScheduleConfigJRso ejob;
			if(job == null) {
				ejob = findJob(jid);
			}else {
				ejob = job;
			}
			if(ejob == null) {
				r.setMsg("任务不存在");
				suc.success(r);
				return;
			}
			
			//ActInfoJRso ai = JMicroContext.get().getAccount();
			if(ejob.getClientId() != ai.getClientId() && !PermissionManager.isCurDefAdmin()) {
				r.setMsg("权限不足");
				suc.success(r);
				return;
			}
			
			boolean up = false;
			if(/*opTypeAdd == Constants.OP_TYPE_START ||*/ opTypeAdd == Constants.OP_TYPE_RESUME) {
				ejob.setStatus(ScheduleConfigJRso.NORMAL);
				up = true;
			}else if(opTypeAdd == Constants.OP_TYPE_PAUSE) {
				ejob.setStatus(ScheduleConfigJRso.PAUSE);
				up = true;
			}
			
			if(up) {
				ejob.setUpdatedTime(TimeUtils.getCurTime());
				ejob.setUpdatedBy(ai.getId());
				if(!os.updateById(ScheduleConfigJRso.TABLE, ejob, ScheduleConfigJRso.class, 
						IObjectStorage._ID, false)) {
					//在此只是更新库状态失败，但是任务已经正常启动
					logger.error("更新任务失败：" + JsonUtils.getIns().toJson(ejob));
				}
			}
			
			logger.info("通知：opTypeAdd"+opTypeAdd+", JOB: " + JsonUtils.getIns().toJson(ejob));
			try {
				siMng.call(ejob.getMcode(), new Object[] {opTypeAdd,ejob})
				.then((rst,f,cxt)->{
					if(f != null) {
						logger.error("通知Mcode:" + ejob.getMcode()+", 失败，"+ f.toString());
						//r.setData(rst.ge);
						r.setCode(f.getCode());
						r.setMsg(f.getMsg());
					} else {
						RespJRso<Object> rr = (RespJRso<Object>) rst;
						r.setData(rr.getData());
						r.setCode(rr.getCode());
					}
					suc.success(r);
				});
			}catch(Throwable e) {
				r.setMsg(e.getMessage());
				suc.success(r);
			}
			
		});
	
	}
}
