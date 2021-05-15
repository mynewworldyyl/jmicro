package cn.expjmicro.tx.shop;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.expjmicro.example.tx.api.ITxShopService;
import cn.expjmicro.example.tx.api.entities.Good;
import cn.expjmicro.example.tx.api.genclient.ITxOrderService$Gateway$JMAsyncClient;
import cn.expjmicro.example.tx.api.mapper.GoodMapper;
import cn.expjmicro.example.tx.api.mapper.ReqMapper;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.cache.ICache;
import cn.jmicro.api.cache.lock.ILocker;
import cn.jmicro.api.cache.lock.ILockerManager;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.ITxListenerManager;
import cn.jmicro.api.tx.TxConstants;

/**
 * showFront=true 可以通过后台管理页面查看并调用服务信息
 * external=true 可以通过服务网关调用此服务
 * logLevel=MC.LOG_DEBUG 默认日志级别的Debug
 * 
 * @author Yulei Ye
 * @date 2021年4月18日 上午10:43:23
 */
@Component
@Service(version="0.0.1",showFront=true,external=true,logLevel=MC.LOG_DEBUG,debugMode=1)
public class TxShopServiceImpl implements ITxShopService {

	private final static Logger logger = LoggerFactory.getLogger(TxShopServiceImpl.class);
	
	@Reference
	private ITxOrderService$Gateway$JMAsyncClient orderSrv;
	
	@Inject
	private ITxListenerManager txMng;
	
	@Inject
	private GoodMapper goodMapper;
	
	@Inject
	private ReqMapper reqMapper;
	
	@Inject
	private ICache cache;
	
	@Inject
	private ILockerManager lockMng;
	
	private int randnum = 0;
	private AtomicInteger successNum = new AtomicInteger(0);
	
	public void ready() {}
	
	@Override
	@SMethod(needResponse=false)
	public void resetGoodCache(int goodId) {
		Good g = goodMapper.selectById(goodId);
		cache.put("/good/num/"+goodId, g.getUsableCnt());
	}

	/**
	 * 同步实现下单服务
	 * @see txType=TxConstants.TYPE_TX_DISTRIBUTED 启用分布式服务
	 * @see txPhase=TxConstants.TX_3PC 3PC事务提交方案
	 * 
	 */
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,timeout=3*60*1000,txPhase=TxConstants.TX_3PC)
	public Resp<Boolean> buy(int goodId,int num) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		
		String key = "/good/num/"+goodId;
		Integer goodNum = cache.get(key);
		
		Good g = goodMapper.selectById(goodId);

		if(g == null) {
			r.setMsg("Good not found!");
			return r;
		}
		
		if(goodNum == null) {
			ILocker lock = lockMng.getLocker(key);
			if(lock.tryLock(2000)) {
				r.setMsg("Fail go good locker");
				return r;
			} 
			
			try {
				goodNum = cache.get(key);
				if(goodNum == null) {
					cache.put("/good/num/"+goodId, g.getUsableCnt());
				}
				if(goodNum < num) {
					r.setMsg("Good num not egnogh!");
					return r;
				}
				
				cache.put(key, goodNum-num);
				
			}finally {
				lock.unLock();
			}
		}
		
		/*int un = goodMapper.decGoodNum(goodId, num);
		if(un != 1) {
			r.setMsg("fail to update good num!");
			return r;
		}*/
		
		try {
			r = orderSrv.takeOrder(g,num);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int un = goodMapper.decGoodNum(goodId, num);
		if(un != 1) {
			r.setMsg("fail to update good num!");
			return r;
		}
		
		return r;
	}
	
	/**
	 * 同步实现下单服务
	 * @see txType=TxConstants.TYPE_TX_LOCAL 只启用本地事务（非分布式事务）
	 */
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,timeout=3*60*1000)
	public Resp<Boolean> updateLocalData(int goodId,int num) {
		
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		int un = goodMapper.decGoodNum(goodId, num);
		if(un != 1) {
			r.setMsg("fail to update good num!");
			return r;
		}
		
		if(++randnum%3!=0) {
			logger.info("successNum: "+successNum.incrementAndGet());
			r.setCode(Resp.CODE_SUCCESS);
			r.setData(true);
		}
		
		return r;
	}
	
	/**
	 * 异步实现下单服务
	 * @see txType=TxConstants.TYPE_TX_DISTRIBUTED 启用分布式服务
	 * @see txPhase=TxConstants.TX_3PC 3PC事务提交方案
	 * @see txIsolation=Connection.TRANSACTION_READ_COMMITTED 事务隔离级别为读提交
	 * 
	 */
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,timeout=3*60*1000,
	txIsolation=Connection.TRANSACTION_READ_COMMITTED,txPhase=TxConstants.TX_3PC)
	public IPromise<Resp<Boolean>> buyAsy(int goodId,int num) {
		
		PromiseImpl<Resp<Boolean>> p = new PromiseImpl<>();
		
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		p.setResult(r);
		
		String key = "/good/num/"+goodId;
		Integer goodNum = cache.get(key);
		
		Good g = goodMapper.selectById(goodId);

		if(g == null) {
			r.setMsg("Good not found!");
			p.done();
			return p;
		}

		ILocker lock = lockMng.getLocker(key);
		try {
			
			if(!lock.tryLock(2000)) {
				r.setMsg("Fail go good locker");
				p.done();
				return p;
			} 
			
			//goodNum = cache.get(key);
			if(goodNum == null) {
				goodNum = g.getUsableCnt();
			}
			
			if(goodNum < num) {
				r.setMsg("Good num not egnogh!");
				p.done();
				return p;
			}
			
			cache.put(key, goodNum-num);
			
			txMng.addTxListener((commit,txid)->{
				if(!commit) {
					Integer gn = cache.get(key);
					cache.put(key, gn+num);
				}
			});
			
		}finally {
			lock.unLock();
		}
		
		IPromise<Resp<Boolean>> pr = orderSrv.takeOrderAsy(g,num);
		
		//Holder<Integer> n = new Holder<>(goodNum);
		pr.success((rst,cxt)->{
			if(rst.getCode() != Resp.CODE_SUCCESS) {
				r.setMsg(rst.getMsg());
				r.setCode(rst.getCode());
			}else {
				int un = goodMapper.decGoodNum(goodId, num);
				if(un == 1) {
					r.setCode(Resp.CODE_SUCCESS);
					r.setData(true);
				} else {
					r.setMsg("扣减库存失败");
					r.setCode(Resp.CODE_FAIL);
				}
			}
			p.done();
		})
		.fail((code,msg,cxt)->{
			r.setMsg(msg);
			r.setCode(code);
			p.done();
		});
		
		return p;
	}

}
