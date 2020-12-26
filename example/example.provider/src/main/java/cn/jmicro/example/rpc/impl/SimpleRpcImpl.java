package cn.jmicro.example.rpc.impl;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SBreakingRule;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.test.Person;
import cn.jmicro.common.Constants;
import cn.jmicro.example.api.rpc.ISimpleRpc;
import cn.jmicro.example.api.rpc.genclient.IRpcA$JMAsyncClient;

@Service(namespace="simpleRpc", version="0.0.1", monitorEnable=0, maxSpeed=-1,debugMode=1,
baseTimeUnit=Constants.TIME_SECONDS, external=true)
@Component
public class SimpleRpcImpl implements ISimpleRpc {

	private final Logger logger = LoggerFactory.getLogger(SimpleRpcImpl.class);
	
	@Reference(namespace="rpca", version="0.0.1")
	private IRpcA$JMAsyncClient rpca;
	
	private Random r = new Random(100);
	
	@Override
	@SMethod(
			//breakingRule="1S 50% 500MS",
			//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
			breakingRule = @SBreakingRule(enable=true, percent=50, checkInterval=2000),
			logLevel=MC.LOG_DEBUG,	
			testingArgs="[\"test args\"]",//测试参数
			monitorEnable=0,
			timeWindow=5*60000,//统计时间窗口5分钟
			slotInterval=100,
			checkInterval=5000,//采样周期2S
			timeout=60000,//1分钟
			retryInterval=1000,
			debugMode=1,
			maxSpeed=10,
			limitType = Constants.LIMIT_TYPE_SS,
			baseTimeUnit=Constants.TIME_MILLISECONDS,
			upSsl=false,encType=0,downSsl=false
	)
	public String hello(String name) {
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG,SimpleRpcImpl.class, name);
		}
		/*int rv = r.nextInt(100);
		if(rv < 50) {
			throw new CommonException("test breaker exception");
		}*/
		//System.out.println("Server hello: " +name);
		//logger.info("Server hello: " +name);
		return "Server say hello to: "+name+" from : " + Config.getInstanceName();
	}
	
	@Override
	@SMethod(
			//breakingRule="1S 50% 500MS",
			//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
			breakingRule = @SBreakingRule(enable=true,percent=50,checkInterval=5000),
			logLevel=MC.LOG_DEBUG,
			testingArgs="[{\"username\":\"Zhangsan\",\"id\":\"1\"}]",//测试参数
			monitorEnable=0,
			timeWindow=5*60000,//统计时间窗口5分钟
			slotInterval=100,
			checkInterval=5000,//采样周期2S
			timeout=5000,
			retryInterval=1000,
			debugMode=0,
			limitType = Constants.LIMIT_TYPE_SS,
			maxSpeed=30,
			baseTimeUnit=Constants.TIME_MILLISECONDS
	)
	public String hi(Person person) {
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG,SimpleRpcImpl.class, person.getUsername());
		}
		//System.out.println("Got: " + person.toString());
		return "Server say hello to: " + person.toString();
	}

	@Override
	public IPromise<String> linkRpc(String msg) {
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG,SimpleRpcImpl.class, "linkRpc call IRpcA with: " + msg);
		}
		System.out.println("linkRpc: " + msg);
		return this.rpca.invokeRpcAJMAsync("invokeRpcA");
	}
	
	@Override
	public IPromise<String> linkRpcAs(String msg) {
		
		PromiseImpl<String> resultPro = new PromiseImpl<>();
		
		if(LG.isLoggable(MC.LOG_DEBUG)) {
			LG.log(MC.LOG_DEBUG,SimpleRpcImpl.class, "linkRpc call IRpcA with: " + msg);
		}
		
		System.out.println("async return val: " + msg);
		resultPro.setResult("async return val: " + msg);
		resultPro.done();
		
		//IPromise<String> p = this.rpca.invokeRpcAJMAsync("invokeRpcA");
		//JMicroContext cxt = JMicroContext.get();

		/*p.success((rst,ctx0) -> {
			resultPro.setResult(rst);
			resultPro.done();
		})
		.fail((code,errMsg,cxt0)->{
			resultPro.setFail(code, errMsg);
			resultPro.done();
		});*/
		
		return resultPro;
	}
	
}
