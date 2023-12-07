package cn.jmicro.ext.iot.service;

import java.util.Map;

import cn.jmicro.api.annotation.SO;
import lombok.Data;
import lombok.Serial;

@SO
@Data
@Serial
public class IotDeviceJRso {

	public static final String TABLE = "t_device";
	
	public static final Byte TYPE_LIGHT = 1;//灯
	public static final Byte TYPE_TV = 2;//电视机
	public static final Byte TYPE_FRIG = 3;//冰箱
	public static final Byte TYPE_AP = 4;//路由器
	
	public static final Byte TYPE_OTHER = 127;//其他
	
	public static Byte STATUS_INIT = 0;//未绑定
	public static Byte STATUS_BUND = 1;//已绑定Mac
	public static Byte STATUS_SYNC_INFO = 2;//已同步绑定的设备ID及账号到设备，设备可以正常使用
	public static Byte STATUS_UNBUND = 3;//已解绑
	public static final byte STATUS_FREEZONE = 4;//冻结
	
	private Integer  id;
	
	private String deviceId;
	
	private Integer productId;
	
	//设备物理地址或设备硬件ID
	private String macAddr;
	
    /*
    private String deviceIP;
	private String sdkVersion;
	private String flashSizeMap;
	private String opmode;
	private String ssid;
	private String shipId;
	private String deviceHostName;
	*/
	
	private Map<String,String> devInfo;
	
	//设置关联账号租户ID，一个租户可以关联N个设备
	private Integer srcClientId;
	
	//设备所属账号ID
	private Integer srcActId;
	
	private String grpName = "Default";
	
	private String name;
	
	private String desc;
	
	private Boolean master=false;
	
	//设备类型
	private Byte type;
	
	private int status;
	
	private long createdTime;
	
	private long updatedTime;
	
	private int createdBy;
	
	private int updatedBy;
	
}
