package cn.expjmicro.tx.payment;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.expjmicro.example.tx.api.ITxPaymentService;
import cn.expjmicro.example.tx.api.entities.Payment;
import cn.expjmicro.example.tx.api.mapper.PaymentMapper;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.CommonException;

@Component
@Service(version="0.0.1")
public class TxPaymentServiceImpl implements ITxPaymentService {

	private final static Logger logger = LoggerFactory.getLogger(TxPaymentServiceImpl.class);
	 
	@Inject
	private PaymentMapper pm;
	
	private int exCnt = 0;
	
	private Random ran = new Random(System.currentTimeMillis()/100);
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,logLevel=MC.LOG_INFO)
	public Resp<Boolean> pay(Payment p) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_SUCCESS,true);
		/*if(p.getId() % 3 == 0) {
			if(++exCnt % 1 == 0) {
				r.setCode(Resp.CODE_FAIL);//模拟支付失败
				r.setData(false);
				r.setMsg("模拟支付失败"+", txid:" + JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L));
				LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
				logger.debug(r.getMsg());
			} else {
				String msg = "余额不足"+", txid:" + JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				logger.debug(msg);
				throw new CommonException(msg);
			}
			return r;
		}*/
		
		if(p.getId() % 3 == 0) {
			if(++exCnt % 1 == 0) {
				r.setCode(Resp.CODE_FAIL);//模拟支付失败
				r.setData(false);
				r.setMsg("payId: "+p.getId()+",模拟支付失败"+", txid:" + JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L));
				LG.log(MC.LOG_ERROR, this.getClass(), r.getMsg());
				logger.debug(r.getMsg());
			} else {
				String msg = "payId: "+p.getId()+",余额不足"+", txid:" + JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L);
				LG.log(MC.LOG_ERROR, this.getClass(), msg);
				logger.debug(msg);
				throw new CommonException(msg);
			}
			return r;
		}
		
		LG.log(MC.LOG_INFO, this.getClass(), "Save payment");
		
		pm.savePayment(p);
		return r;
	}
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,logLevel=MC.LOG_INFO)
	public IPromise<Resp<Boolean>> payAsy(Payment p) {
		Resp<Boolean> r = pay(p);
		PromiseImpl<Resp<Boolean>> pr = new PromiseImpl<>();
		pr.setResult(r);
		pr.done();
		return pr;
	}

}
