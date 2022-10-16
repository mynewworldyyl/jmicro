package cn.jmicro.api.security;

import lombok.Data;
import lombok.Serial;

@Serial
@Data
public class PermissionJRso {
	
	public static final int STATUS_APPLY = 1;   //审核中
	public static final int STATUS_REJECT = 2;  //拒绝
	public static final int STATUS_APPROVE = 3; //通过
	public static final int STATUS_REVOKE = 4;  //回收
	
	public static final int STATUS_DELETE = 5;  //删除
	
	public static final int TYPE_ROLE = 1;//授权给角色
	public static final int TYPE_ACT = 2;//授权给账号
	
	public static final String ACT_INVOKE = "Invoke";
	
	public static final String ACT_ADD = "Add";
	public static final String ACT_UPDATE = "Update";
	public static final String ACT_DELETE = "Delete";
	public static final String ACT_QUERY = "Query";
	
	//public static final String NAME_INVOKE = "invoke";
	private int haCode;
	
	//private String pid;
	
	private String methodName;
	
	private String label;
	
	private String modelLabel;
	
	private String modelName;
	
	private int status;
	
	private int type = TYPE_ACT;
	
	private String actType;
	
	//比如某个服务方法的KEY
	//private String key;
}
