package cn.expjmicro.example.tx.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import cn.expjmicro.example.tx.api.entities.Good;

@Mapper
public interface GoodMapper {

	public void saveGood(Good g);
	
	@Update({ "update t_good set usable_cnt = usable_cnt - #{num} where id = #{goodId}" })
	public int decGoodNum(@Param("goodId")int goodId,@Param("num") int num);
	
	@Results({
		@Result(property = "id", column = "id"),
		@Result(property = "name", column = "name"),
		@Result(property = "total", column = "total"),
		@Result(property = "usableCnt", column = "usable_cnt"),
		@Result(property = "price", column = "price") })
	@Select({ "Select * from t_good where id = #{goodId}" })
	public Good selectById(int goodId);
}
