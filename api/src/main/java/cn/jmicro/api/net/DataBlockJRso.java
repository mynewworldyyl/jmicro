package cn.jmicro.api.net;

import java.util.concurrent.atomic.AtomicInteger;

import cn.jmicro.api.utils.TimeUtils;
import lombok.Data;

@Data
public class DataBlockJRso {
	
	public static final int INIT_BLOCK = 0;
	
	//public static final byte PHASE_INIT = 1;
	
	public static final byte PHASE_UPDATE_DATA = 2;
	
	public static final byte PHASE_PROCESS = 3;
	
	public static final byte PHASE_FINISH = 4;
	
	//所处阶段
	private byte phase = PHASE_UPDATE_DATA;
	
	private int id;
	
	private int blockNum;
	
	private AtomicInteger finishBlockNum = new AtomicInteger();
	
	private int blockSize;
	
	private long totalLen;
	
	private String filePath;
	
	private String fileName;
	
	private String extParams;
	
	private long lastUseTime = TimeUtils.getCurTime();
	
	public int addFinishBlockNum() {
		return finishBlockNum.incrementAndGet();
	}
}
