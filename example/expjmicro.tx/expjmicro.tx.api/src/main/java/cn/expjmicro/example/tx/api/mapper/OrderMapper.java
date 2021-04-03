package cn.expjmicro.example.tx.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import cn.expjmicro.example.tx.api.entities.Order;

@Mapper
public interface OrderMapper {

	public void saveOrder(Order o);
}
