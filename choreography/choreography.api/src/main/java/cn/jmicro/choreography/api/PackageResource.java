package cn.jmicro.choreography.api;

import java.util.Set;

import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.annotation.SO;

@SO
@IDStrategy
public class PackageResource {
	
	//public static final String ID2NAME_SEP = "#$#";
	
	public static final String TABLE_NAME = "t_resources";
	
	public static final String DEP_FILE = "jmicro_deps.txt";
	
	public static final int STATUS_UPLOADING = 1;//上传中
	
	public static final int STATUS_READY = 2;//待启用
	
	public static final int STATUS_ENABLE = 3;//可以使用
	
	public static final int STATUS_ERROR = 4;//错误
	
	public static final int STATUS_WAITING = 5; //等待依赖下载完成
	
	public static final int STATUS_CHECK_FOR_DOWNLOAD = 6;//待下载，从第3方服务器下载，如maven仓库
	
	public static final int STATUS_CHECK_SECURITY = 7;//系统检测包存在安全问题

	private int id;
	
	private int clientId;
	
	private int createdBy;
	
	private String resExtType;
	
	private String name;
	
	private String group;
	
	private String artifactId;
	
	private String version;
	
	private Set<Integer> depIds;
	
	private Set<Integer> waitingRes;
	
	private boolean main;
	
	private int resVer;
	
	private int status;
	
	private int downloadNum;
	
	private long lastDownloadTime;
	
	private long uploadTime;
	
	private long modifiedTime;

	private long size;
	
	private int finishBlockNum = 0;
	
	private int totalBlockNum = 0;
	
	private int blockSize = 0;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public int getFinishBlockNum() {
		return finishBlockNum;
	}

	public boolean isMain() {
		return main;
	}

	public void setMain(boolean main) {
		this.main = main;
	}

	public void setFinishBlockNum(int finishBlockNum) {
		this.finishBlockNum = finishBlockNum;
	}

	public int getTotalBlockNum() {
		return totalBlockNum;
	}

	public void setTotalBlockNum(int totalBlockNum) {
		this.totalBlockNum = totalBlockNum;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
	}

	public int getResVer() {
		return resVer;
	}

	public void setResVer(int resVer) {
		this.resVer = resVer;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode():0;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}

	public int getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(int createdBy) {
		this.createdBy = createdBy;
	}

	public String getResExtType() {
		return resExtType;
	}

	public void setResExtType(String resExtType) {
		this.resExtType = resExtType;
	}

	public int getDownloadNum() {
		return downloadNum;
	}

	public void setDownloadNum(int downloadNum) {
		this.downloadNum = downloadNum;
	}

	public long getLastDownloadTime() {
		return lastDownloadTime;
	}

	public void setLastDownloadTime(long lastDownloadTime) {
		this.lastDownloadTime = lastDownloadTime;
	}

	public long getUploadTime() {
		return uploadTime;
	}

	public void setUploadTime(long uploadTime) {
		this.uploadTime = uploadTime;
	}

	public long getModifiedTime() {
		return modifiedTime;
	}

	public void setModifiedTime(long modifiedTime) {
		this.modifiedTime = modifiedTime;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getGroup() {
		return group;
	}

	public Set<Integer> getWaitingRes() {
		return waitingRes;
	}

	public void setWaitingRes(Set<Integer> waitingRes) {
		this.waitingRes = waitingRes;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Set<Integer> getDepIds() {
		return depIds;
	}

	public void setDepIds(Set<Integer> depIds) {
		this.depIds = depIds;
	}

	@Override
	public String toString() {
		return "PackageResource [id=" + id + ", clientId=" + clientId + ", createdBy=" + createdBy + ", resExtType="
				+ resExtType + ", name=" + name + ", group=" + group + ", artifactId=" + artifactId + ", version="
				+ version + ", status=" + status + ", downloadNum=" + downloadNum
				+ ", lastDownloadTime=" + lastDownloadTime + ", uploadTime=" + uploadTime + ", updateTime=" + modifiedTime
				+ ", size=" + size + ", finishBlockNum=" + finishBlockNum + ", totalBlockNum=" + totalBlockNum
				+ ", blockSize=" + blockSize + "]";
	}
	
	
}
