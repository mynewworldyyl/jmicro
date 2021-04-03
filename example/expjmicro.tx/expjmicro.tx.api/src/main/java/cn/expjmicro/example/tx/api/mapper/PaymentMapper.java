package cn.expjmicro.example.tx.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import cn.expjmicro.example.tx.api.entities.Payment;

@Mapper
public interface PaymentMapper {

	public void savePayment(Payment p);
}
