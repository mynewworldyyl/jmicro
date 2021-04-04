package cn.expjmicro.tx.shop;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.expjmicro.example.tx.api.ITxOrderService;
import cn.expjmicro.example.tx.api.ITxShopService;
import cn.expjmicro.example.tx.api.entities.Good;
import cn.expjmicro.example.tx.api.mapper.GoodMapper;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;

@Component
@Service(version="0.0.1",showFront=true,external=true)
public class TxShopServiceImpl implements ITxShopService {

	private final static Logger logger = LoggerFactory.getLogger(TxShopServiceImpl.class);
	
	@Reference
	private ITxOrderService orderSrv;
	
	@Inject
	private GoodMapper goodMapper;
	
	private int randnum = 0;
	private AtomicInteger successNum = new AtomicInteger(0);
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,timeout=3*60*1000,logLevel=MC.LOG_INFO)
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
	@SMethod(txType=TxConstants.TYPE_TX_LOCAL,timeout=3*60*1000,logLevel=MC.LOG_INFO)
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

}
