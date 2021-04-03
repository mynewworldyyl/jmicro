package cn.expjmicro.tx.payment;

import cn.expjmicro.example.tx.api.ITxPaymentService;
import cn.expjmicro.example.tx.api.entities.Payment;
import cn.expjmicro.example.tx.api.mapper.PaymentMapper;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.CommonException;

@Component
@Service(version="0.0.1")
public class TxPaymentServiceImpl implements ITxPaymentService {

	@Inject
	private PaymentMapper pm;
	
	private int exCnt = 0;
	
	@Override
	@SMethod(txType=TxConstants.TYPE_TX_DISTRIBUTED,logLevel=MC.LOG_INFO)
	public Resp<Boolean> pay(Payment p) {
		Resp<Boolean> r = new Resp<>(Resp.CODE_SUCCESS,true);
		if(p.getId() % 3 == 0) {
			if(++exCnt % 1 == 0) {
				r.setCode(Resp.CODE_FAIL);//模拟支付失败
				r.setData(false);
				LG.log(MC.LOG_ERROR, this.getClass(), "模拟支付失败");
			}else {
				LG.log(MC.LOG_ERROR, this.getClass(), "模拟支付余额不足失败");
				throw new CommonException("余额不足");
			}
			return r;
		}
		pm.savePayment(p);
		return r;
	}

}
