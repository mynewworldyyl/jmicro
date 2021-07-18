package cn.jmicro.api.security;

import java.util.HashSet;
import java.util.Set;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;
import lombok.Data;

@SO
@Data
@IDStrategy
public class RoleJRso {

	public static final Integer ROLE_SYS_ADMIN = 1;//后台超级用户，可以部署启动系统
	
	public static final Integer ROLE_SYS_MNG = 2;//由超级用户创建的后台管理账号，可以管理系统，做日常系统运维
	
	public static final Integer ROLE_SHOP_MNG = 3;//商家管理用户
	
	public static final Integer ROLE_SHOP_MEMBER = 4;//店员，由商家管理用户分配相应权限
	
	public static final Integer ROLE_FARMER = 5;//农作物种植户
	
	public static final Integer ROLE_MAKER = 6;//产品加工制造商，中间商
	
	public static final Integer ROLE_DELIVERY = 7;//配送员
	
	public static final Integer ROLE_SYS_RESERVE = 255;//系统保留最大值
	
	private int roleId;
	
	private String name;
	
	private String desc;
	
	private Set<Integer> pers = new HashSet<>();
	
	private int clientId;
	
	private int createdBy;
	
	private int updatedBy;
	
	private long createdTime;
	
	private long updatedTime;
}
