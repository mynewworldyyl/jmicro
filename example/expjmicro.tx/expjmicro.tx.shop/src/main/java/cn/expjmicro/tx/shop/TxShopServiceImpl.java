package cn.expjmicro.tx.shop;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.expjmicro.example.tx.api.ITxShopService;
import cn.expjmicro.example.tx.api.entities.Good;
import cn.expjmicro.example.tx.api.entities.Req;
import cn.expjmicro.example.tx.api.genclient.ITxOrderService$Gateway$JMAsyncClient;
import cn.expjmicro.example.tx.api.mapper.GoodMapper;
import cn.expjmicro.example.tx.api.mapper.ReqMapper;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;

@Component
@Service(version="0.0.1",showFront=true,external=true,logLevel=MC.LOG_DEBUG)
public class TxShopServiceImpl implements ITxShopService {

	private final static Logger logger = LoggerFactory.getLogger(TxShopServiceImpl.class);
	
	@Reference
	private ITxOrderService$Gateway$JMAsyncClient orderSrv;
	
	@Inject
	private GoodMapper goodMapper;
	
	@Inject
	private ReqMapper reqMapper;
	
	private int randnum = 0;
	private AtomicInteger successNum = new AtomicInteger(0);
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,timeout=3*60*1000,txPhase=TxConstants.TX_3PC)
	public Resp<Boolean> buy(int goodId,int num) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		
		Good g = goodMapper.selectById(goodId);
		if(g == null) {
			r.setMsg("Good not found!");
			return r;
		}
		
		if(g.getUsableCnt() < num) {
			r.setMsg("Good num not egnogh!");
			return r;
		}
		
		int un = goodMapper.decGoodNum(goodId, num);
		if(un != 1) {
			r.setMsg("fail to update good num!");
			return r;
		}
		
		r = orderSrv.takeOrder(goodId,num);
		
		return r;
	}
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_LOCAL,timeout=3*60*1000)
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
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,timeout=3*60*1000,
	txIsolation=Connection.TRANSACTION_READ_COMMITTED,txPhase=TxConstants.TX_3PC)
	public IPromise<Resp<Boolean>> buyAsy(int goodId,int num) {
		
		PromiseImpl<Resp<Boolean>> p = new PromiseImpl<>();
		
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		p.setResult(r);
		
		Good g = goodMapper.selectById(goodId);
		if(g == null) {
			r.setMsg("Good not found!");
			p.done();
			return p;
		}
		
		if(g.getUsableCnt() < num) {
			r.setMsg("Good num not egnogh!");
			p.done();
			return p;
		}
		
		if(LG.isLoggable(MC.LOG_INFO)) {
			String msg = "decGoodNum cur: "+g.getUsableCnt()+" txid: " + 
					(JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L));
			LG.log(MC.LOG_INFO, TxShopServiceImpl.class, msg);
			logger.info(msg);
		}
		
		int un = goodMapper.decGoodNum(goodId, num);
		if(un < 1) {
			r.setMsg("fail to update good num!");
			LG.log(MC.LOG_ERROR, TxShopServiceImpl.class, "Fail to dec good num cur:" + g.getUsableCnt());
			p.done();
			return p;
		}
		
		Req req = new Req();
		req.setGoodId(goodId);
		req.setNum(num);
		req.setTxid(JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L));
		reqMapper.saveReq(req);
		
		if(LG.isLoggable(MC.LOG_INFO)) {
			String msg = "Invoke order service takeOrderAsy txid: " + 
					(JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L));
			LG.log(MC.LOG_INFO, TxShopServiceImpl.class, msg);
			logger.info(msg);
		}
		return orderSrv.takeOrderAsy(g,num);
	}

}
