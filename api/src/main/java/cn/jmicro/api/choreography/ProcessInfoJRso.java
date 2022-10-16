package cn.jmicro.api.choreography;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.CfgMetadataJRso;
import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import lombok.Data;
import lombok.Serial;

@Serial
@IDStrategy(1)
@Data
public class ProcessInfoJRso {

	private int id;
	
	private int clientId;
	
	private String actName;
	
	private String host;
	
	private String port;
	
	private String httpPort;
	
	private String osName;
	
	private String instanceName;
	
	private String agentHost;
	
	private String agentInstanceName;
	
	private String depId;
	
	private String agentId;
	
	private String agentProcessId;
	
	private String pid;
	
	private String cmd;
	
	private String workDir;
	
	private String infoFilePath;
	
	private boolean active;
	
	private long opTime;
	
	private long timeOut;
	
	private long startTime;
	
	private boolean haEnable = false;
	
	private boolean master = false;
	
	private transient Process process;
	
	private transient ActInfoJRso ai;
	
	private boolean monitorable = false;
	
	private byte logLevel = MC.LOG_INFO;
	
	private Set<Byte> types = new HashSet<>();
	
	protected Map<String,Set<CfgMetadataJRso>> metadatas = new HashMap<>();

	public boolean isLogin() {
		return ai != null;
	}
	
	public void setAi(ActInfoJRso ai) {
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		this.ai = ai;
	}
	
	public Set<CfgMetadataJRso> setMetadatas(String key,Set<CfgMetadataJRso> ms) {
		return metadatas.put(key, ms);
	}

	public Set<CfgMetadataJRso> getMetadatas(String key) {
		return metadatas.get(key);
	}

	public void setWorkDir(String workDir) {
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		this.workDir = workDir;
	}

	public void setActive(boolean active) {
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		this.active = active;
	}

	public void setInstanceName(String instanceName) {
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		this.instanceName = instanceName;
	}

	public void setClientId(int clientId) {
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		this.clientId = clientId;
	}

	public void setActName(String actName) {
		if(!Utils.formSystemPackagePermission(3)) {
			 throw new CommonException(MC.MT_ACT_PERMISSION_REJECT,"非法操作");
		}
		this.actName = actName;
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ProcessInfoJRso)) {
			return false;
		}
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public String toString() {
		return "ProcessInfo [id=" + id + ", host=" + host + ", instanceName=" + instanceName + ", agentHost="
				+ agentHost + ", agentInstanceName=" + agentInstanceName + ", depId=" + depId + ", agentId=" + agentId
				+ ", agentProcessId=" + agentProcessId + ", pid=" + pid + ", cmd=" + cmd + ", workDataDir="
				+ workDir + ", active=" + active + "]";
	}

}
