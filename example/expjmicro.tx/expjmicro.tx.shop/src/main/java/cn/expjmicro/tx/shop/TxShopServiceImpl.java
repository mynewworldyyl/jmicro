package cn.expjmicro.tx.shop;

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

	@Reference
	private ITxOrderService orderSrv;
	
	@Inject
	private GoodMapper goodMapper;
	
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

}
