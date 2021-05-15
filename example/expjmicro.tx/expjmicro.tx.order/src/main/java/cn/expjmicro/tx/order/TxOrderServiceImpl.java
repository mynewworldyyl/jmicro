package cn.expjmicro.tx.order;

import cn.expjmicro.example.tx.api.ITxOrderService;
import cn.expjmicro.example.tx.api.ITxPaymentService;
import cn.expjmicro.example.tx.api.entities.Good;
import cn.expjmicro.example.tx.api.entities.Order;
import cn.expjmicro.example.tx.api.entities.Payment;
import cn.expjmicro.example.tx.api.mapper.GoodMapper;
import cn.expjmicro.example.tx.api.mapper.OrderMapper;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Reference;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;

@Component
@Service(version="0.0.1",logLevel=MC.LOG_DEBUG,debugMode=1)
public class TxOrderServiceImpl implements ITxOrderService {

	@Inject
	private OrderMapper om;
	
	@Reference
	private ITxPaymentService paymentSrv;
	
	@Inject
	private GoodMapper goodMapper;
	
	@Inject
	private ComponentIdServer idServer;
	
	/**
	 * 同步下单服务， 被商店服务调用
	 * TxConstants.TYPE_TX_DISTRIBUTED 启用分布式服务
	 */
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED)
	public Resp<Boolean> takeOrder(Good g,int num) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_FAIL,false);
		
		Order o = new Order();
		o.setGoodId(g.getId());
		o.setNum(num);
		o.setAmount(o.getNum()*g.getPrice());
		o.setId(idServer.getLongId(Order.class));
		o.setTxid(JMicroContext.get().getLong(TxConstants.TX_ID, -1L));
		
		LG.log(MC.LOG_INFO, this.getClass(), "Save order");
		//保存订单
		om.saveOrder(o);
		
		Payment p = new Payment();
		p.setId(idServer.getLongId(Order.class));
		p.setAmount(o.getAmount());
		p.setOrderId(o.getId());
		p.setTxid(o.getTxid());
		
		LG.log(MC.LOG_INFO, this.getClass(), "Before invoke pay service");
		//调用支付服务
		Resp<Boolean> rr = paymentSrv.pay(p);
		LG.log(MC.LOG_INFO, this.getClass(), "After invoke pay service");
		if(!rr.getData() || rr.getCode() != 0) {
			LG.log(MC.LOG_ERROR, this.getClass(), "Pay error: "+rr.getMsg());
			r.setMsg(rr.getMsg());
			
			return r;
		}
		
		r.setCode(Resp.CODE_SUCCESS);
		r.setData(true);
		return r;
	}
	
	/**
	 * 异步下单服务， 被商店服务调用
	 * TxConstants.TYPE_TX_DISTRIBUTED 启用分布式服务
	 */
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED)
	public IPromise<Resp<Boolean>> takeOrderAsy(Good g,int num) {
		
		Order o = new Order();
		o.setGoodId(g.getId());
		o.setNum(num);
		o.setAmount(o.getNum()*g.getPrice());
		o.setId(idServer.getLongId(Order.class));
		o.setTxid(JMicroContext.get().getLong(TxConstants.TX_ID, -1L));
		
		LG.log(MC.LOG_INFO, this.getClass(), "Save order");
		//保存订单
		om.saveOrder(o);
		
		Payment p = new Payment();
		p.setId(idServer.getLongId(Order.class));
		p.setAmount(o.getAmount());
		p.setOrderId(o.getId());
		p.setTxid(o.getTxid());
		
		LG.log(MC.LOG_INFO, this.getClass(), "Before invoke pay service");
		
		//调用支付服务
		IPromise<Resp<Boolean>> pp = paymentSrv.payAsy(p);
		pp.then((rst,fail,cxt)->{
			String log = "";
			if(fail!= null) {
				log = log + ",fail:"+fail.toString();
			} else {
				log = rst.getCode()+"="+rst.getMsg()+",data: " + rst.getData();
			}
			LG.log(MC.LOG_DEBUG, this.getClass(), log);
		});
		
		return pp;
	}

}
