package cn.expjmicro.tx.order;

import cn.expjmicro.example.tx.api.ITxOrderService;
import cn.expjmicro.example.tx.api.ITxPaymentService;
import cn.expjmicro.example.tx.api.entities.Good;
import cn.expjmicro.example.tx.api.entities.Order;
import cn.expjmicro.example.tx.api.entities.Payment;
import cn.expjmicro.example.tx.api.mapper.GoodMapper;
import cn.expjmicro.example.tx.api.mapper.OrderMapper;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;

@Component
@Service(version="0.0.1")
public class TxOrderServiceImpl implements ITxOrderService {

	@Inject
	private OrderMapper om;
	
	@Reference
	private ITxPaymentService paymentSrv;
	
	@Inject
	private GoodMapper goodMapper;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,logLevel=MC.LOG_INFO)
	public Resp<Boolean> takeOrder(int goodId,int num) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		
		Good g = goodMapper.selectById(goodId);
		
		Order o = new Order();
		o.setGoodId(goodId);
		o.setNum(num);
		o.setAmount(o.getNum()*g.getPrice());
		o.setId(idServer.getLongId(Order.class));
		
		om.saveOrder(o);
		
		Payment p = new Payment();
		p.setId(idServer.getLongId(Order.class));
		p.setAmount(o.getAmount());
		p.setOrderId(o.getId());
		Resp<Boolean> rr = paymentSrv.pay(p);
		if(!rr.getData() || rr.getCode() != 0) {
			LG.log(MC.LOG_ERROR, this.getClass(), "Pay error: "+rr.getMsg());
			r.setMsg(rr.getMsg());
			return r;
		}
		
		r.setCode(Resp.CODE_SUCCESS);
		r.setData(true);
		return r;
	}

}
