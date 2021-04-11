package cn.expjmicro.example.tx.api.mapper;

import org.apache.ibatis.annotations.Mapper;

import cn.expjmicro.example.tx.api.entities.Req;

@Mapper
public interface ReqMapper {

	public void saveReq(Req p);
}
