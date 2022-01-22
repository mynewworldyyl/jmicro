package cn.jmicro.api.ds;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.SO;
import lombok.Data;

@Data
@SO
public class DsQueryCacheJRso {

	public static final String TABLE = "t_ds_req_resp";
	
	private RespJRso<String> resp;
	
	private ApiReqJRso req;
	
	private int createdBy;
	
	private long createdTime;
	
	private int clientId;
	
	//耗时，毫秒
	private long cost;
	
	private Double price = 0D;
	
	private boolean counted = false;
	
}
