package cn.jmicro.api.ds;

import cn.jmicro.api.RespJRso;
import lombok.Data;
import lombok.Serial;

@Data
@Serial
public class DsQueryCacheJRso {

	public static final String TABLE = "t_ds_req_resp";
	
	public static final String[] QRY_COLUMNS = {"resp","createdBy","createdTime","clientId","cost",
			"price","counted","req.apiId","req.reqId","req.validDays"};
	
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
