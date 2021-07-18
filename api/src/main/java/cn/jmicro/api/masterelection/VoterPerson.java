package cn.jmicro.api.masterelection;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.IDStrategy;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.StringUtils;

@IDStrategy(10)
public class VoterPerson {
	
	public final static String ROOT = Config.getRaftBasePath("")+"/_ME";
	
	private final static String PREFIX_SEPERATOR = "##";

	private final static Logger logger = LoggerFactory.getLogger(VoterPerson.class);
	
	private IDataOperator op;
	
	//private Config cfg;
	
	//private ComponentIdServer idServer;
	
	private String dir;
	
	//用于区别本法人实例
	private String prefix;
	
	private boolean isMaster;
	
	private long masterSeq = Long.MAX_VALUE;
	
	private long seq = Long.MAX_VALUE;
	
	//dir创建时间
	private long electionStartTime;
	
	//统计选票前需要等待时长
	private long timeout = 5*1000;
	
	//是否进入选主状态,true:选主状态，服务不能在正常工作状态
	private boolean lockStatu = true;
	
	private Set<IMasterChangeListener> listeners = new HashSet<>();
	
	public VoterPerson(IObjectFactory of, String tag) {
		if(of == null) {
			throw new CommonException("Data operator cannot be NULL");
		}
		
		if(Utils.isEmpty(tag)) {
			throw new CommonException("Vote tag cannot be null");
		}
		
		/*
		if(StringUtils.isEmpty(tag) || StringUtils.isEmpty(tag.trim())) {
			tag = Config.getInstancePrefix();
			int idx = -1;
			for(int i = tag.length()-1; i >= 0; i--) {
				if(!Character.isDigit(tag.charAt(i))) {
					idx = i;
					break;
				}
			}
			tag = tag.substring(0,idx+1);
		}
		*/
		
		this.op = of.get(IDataOperator.class,false);
		//this.cfg = of.get(Config.class);
		//this.idServer = of.get(ComponentIdServer.class);
		//this.listener = listener;
		
		String p = ROOT + "/" + Config.getInstancePrefix();
		if(!op.exist(p)) {
			op.createNodeOrSetData(p, "", IDataOperator.PERSISTENT);
		}
		
		this.dir = p + "/" + tag;
		
		long curTime = TimeUtils.getCurTime();
		if(!op.exist(dir)) {
			//创建目录节点，并在目录上记录开始时间
			electionStartTime = curTime;
			logger.info("Create master dir node and enter election status: {}, dir: {}",electionStartTime,dir);
			op.createNodeOrSetData(this.dir, "" + electionStartTime, IDataOperator.PERSISTENT);
			lockStatu = true;
		} else {
			Set<String> chs = op.getChildren(this.dir, false);
			//目录已经存在，则取目录上的时间
			if(chs == null || chs.isEmpty()) {
				//第一个节点
				electionStartTime = curTime;
				logger.info("Enter election status time: {} dir: {}",electionStartTime,dir );
				op.setData(dir, electionStartTime + "");
				lockStatu = true;
			} else {
				//已经存在节点
				String t = op.getData(dir);
				logger.info("The starting election time:{} dir: {}",t,dir);
				electionStartTime = Long.parseLong(t);
				//服务已经进入工作状态
				lockStatu = curTime - electionStartTime < timeout;
			}
		}
		
		this.prefix = Config.getInstanceName() + PREFIX_SEPERATOR;
		
		String nodePath = this.dir +"/" + this.prefix;
		op.createNodeOrSetData(nodePath, "", IDataOperator.EPHEMERAL_SEQUENTIAL);
		//this.seq = Long.parseLong(getSeq(op.getData(nodePath)));
		
		op.addChildrenListener(this.dir, (type,parent,child)->{
			
			if(type == IListener.REMOVE) {
				long dm = getSeq(child);
				//进入待选主状态
				if(dm == masterSeq) {
					logger.info("Master [{}] offline, dir: {}", dm,dir);
					
					if(dm == seq) {
						//退位
						this.notify(IMasterChangeListener.MASTER_OFFLINE, true);
					}
					
					//主节点下线,才需要重新选举
					long t = Long.parseLong(op.getData(dir));
					if(this.electionStartTime ==  t) {
						//上一任期的时间，说明时间没有更新
						//最先收到主节点下线通知，宣告重选主节点
						electionStartTime = TimeUtils.getCurTime();
						op.setData(dir, electionStartTime + "");
						logger.info("The start election time [{}] offline, dir: {}", dm,dir);
					}
				}
				
			} else if(type == IListener.ADD) {
				//最先接收到通知的节点，不一定就是主节点，如多个系统同时增加节点1，2，3，收到通知的顺序可能是 3，1，2
				//那么首先收到通知的节点3肯定不能选为主节点
				//selectMaster();
			}
		});
		
		op.addDataListener(this.dir, (path,data) -> {
			long upTime = Long.parseLong(data);
			this.lockStatu = true;
			this.masterSeq = -1;
			if(upTime != this.electionStartTime) {
				this.electionStartTime = upTime;
				this.notify(IMasterChangeListener.MASTER_OFFLINE, false);
			}
			this.startElectionWorker();
		});
		
		Set<String> children = op.getChildren(this.dir, false);
		for(String child : children) {
			long s = getSeq(child);
			if(child.startsWith(this.prefix)) {
				this.seq = s;
				if(lockStatu) {
					break;
				}
			}
			if(!lockStatu) {
				if(s < this.masterSeq) {
					this.masterSeq = s;
				}
			}
			
		}
		
		if(lockStatu) {
			this.startElectionWorker();
		} else {
			this.isMaster = false;
		}
	}

	private void notify(int masterOffline, boolean b) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		new Thread(()-> {
			if(cl != null) Thread.currentThread().setContextClassLoader(cl);
			for(IMasterChangeListener l : this.listeners) {
				l.masterChange(masterOffline, b);
			}
			
		}).start();
	}

	private long getSeq(String child) {
		return Long.parseLong(child.split(PREFIX_SEPERATOR)[1]);
	}

	private void startElectionWorker() {
		logger.info("Enter election status: " + this.dir);
		//new Thread(()->{
			while(!doWorker()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		//}).start();
	}
	
	private boolean doWorker() {
		//等待指定时间后工始计票
		long besTime = TimeUtils.getCurTime() - this.electionStartTime;
		while( besTime < timeout) {
			try {
				logger.debug("Need wait: [" + (besTime / 1000) + "] seconds.");
				Thread.sleep(1000);
				besTime = TimeUtils.getCurTime() - this.electionStartTime;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return selectMaster();
		
	}

	private boolean selectMaster() {
		long curTime = TimeUtils.getCurTime();
		if(curTime - this.electionStartTime < this.timeout) {
			//等一段时间，全部节点上线后才开始选
			doWorker();
		}
		
		Set<String> children = op.getChildren(dir, false);
		if(children != null && !children.isEmpty()) {
			try {
				long mq = Long.MAX_VALUE;
				for(String c : children) {
					long id = getSeq(c);
					if(id < mq) {
						mq = id;//值最小者为主节点
					}
				}
				
				logger.info("Seq: {}, Master [{}] , got master: {}, dir: {}", this.seq,mq, mq == this.seq, dir);
				
				//进入工作状态
				lockStatu = false;
				this.masterSeq = mq;
				this.isMaster = mq == this.seq;
				notify(IMasterChangeListener.MASTER_ONLINE, this.isMaster);
				return true;
			}catch(Throwable e) {
				//进入下一轮继续选
				return false;
			}
		} else {
			//进入下一轮继续选
			return false;
		}
	}
	
	public void addListener(IMasterChangeListener l) {
		if(!this.listeners.contains(l)) {
			this.listeners.add(l);
			if(this.isMaster) {
				//已经是主状态
				l.masterChange(IMasterChangeListener.MASTER_ONLINE, this.isMaster);
			}
		} else {
			throw new CommonException("Save listenre exist");
		}
	}
	
	public void removeListener(IMasterChangeListener l) {
		if(!this.listeners.contains(l)) {
			this.listeners.remove(l);
		}
	}


	public boolean isMaster() {
		return isMaster;
	}

	public boolean isLockStatu() {
		return lockStatu;
	}
	
}
