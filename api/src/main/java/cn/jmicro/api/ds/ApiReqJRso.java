package cn.jmicro.api.ds;

import lombok.Data;
import lombok.Serial;

@Serial
@Data
public class ApiReqJRso {

	//接口标识
	private String apiId;
	
	//请求唯一标识，发果为空，由服务生成并返回给调用者，调用者可通过此接口查询对应的结果
	private String reqId;
	
	//多少天内的缓存有效
	private int validDays;
	
	//请求参数
	private String jsonParam;
		
}
