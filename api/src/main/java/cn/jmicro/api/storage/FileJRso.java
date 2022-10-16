package cn.jmicro.api.storage;

import java.util.Map;

import cn.jmicro.api.annotation.IDStrategy;
import lombok.Data;
import lombok.Serial;

@Serial
@IDStrategy
@Data
public class FileJRso {
	
	public static final String TABLE = "t_file";
	
	public static final byte S_ERROR = 1;
	public static final byte S_UPING = 2;
	public static final byte S_FINISH = 3;
	
	private String id;
	
	private int clientId;
	
	private int createdBy;
	
	private String type;
	
	private String name;
	
	private String localPath;
	
	private String group;
	
	private Byte status;
	
	private int downloadNum;
	
	private long lastDownloadTime;
	
	private long lastModified;

	private long size;
	
	private int finishBlockNum = 0;
	
	private int totalBlockNum = 0;
	
	private int blockSize = 0;
	
	private boolean tochar = false;
	
	private long updatedTime;
	
	private int mcode = 0;
	
	private Map<String, String> attr;
	
}
