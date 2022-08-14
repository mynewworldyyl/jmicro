package cn.jmicro.api.schedule;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import cn.jmicro.api.objectfactory.IObjectFactory;

public class JMicroJobFactory implements JobFactory{

	private IObjectFactory of;
	
	public JMicroJobFactory(IObjectFactory f) {
		this.of = f;
	}
	@Override
	public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
		JobDetail jd = bundle.getJobDetail();
		Class<? extends Job> jclass = jd.getJobClass();
		Job j = of.get(jclass);
		return j;
	}

}
