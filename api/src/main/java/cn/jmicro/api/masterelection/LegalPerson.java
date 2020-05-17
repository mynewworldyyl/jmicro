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
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.StringUtils;

@IDStrategy(10)
public class LegalPerson {
	
	public final static String ROOT = Config.BASE_DIR+"/_ME";

	private final static Logger logger = LoggerFactory.getLogger(LegalPerson.class);
	
	private IDataOperator op;
	
	private Config cfg;
	
	//private ComponentIdServer idServer;
	
	private String dir;
	
	private String prefix;
	
	private boolean isMaster;
	
	private long masterSeq = -1;
	
	private long seq = -1;
	
	//dir创建时间
	private long electionStartTime;
	
	private long timeout = 10*1000;
	
	//是否进入选主状态,true:选主状态，服务不能在正常工作状态
	private boolean lockStatu = true;
	
	private Set<IMasterChangeListener> listeners = new HashSet<>();
	
	public LegalPerson(IObjectFactory of, String tag) {
		if(of == null) {
			throw new CommonException("Data operator cannot be NULL");
		}
		
		if(StringUtils.isEmpty(tag) || StringUtils.isEmpty(tag.trim())) {
			throw new CommonException("Election tag cannot be NULL");
		}
		
		this.op = of.get(IDataOperator.class);
		this.cfg = of.get(Config.class);
		//this.idServer = of.get(ComponentIdServer.class);
		
		//this.listener = listener;
		
		this.dir = ROOT +"/"+ tag;
		
		long curTime = System.currentTimeMillis();
		if(!op.exist(dir)) {
			//创建目录节点，并在目录上记录开始时间
			electionStartTime = curTime;
			logger.info("Create master dir node and enter election status: {}, dir: {}",electionStartTime,dir);
			op.createNode(this.dir, "" + electionStartTime, IDataOperator.PERSISTENT);
			lockStatu = true;
		}else {
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
		
		this.prefix = cfg.getString(Constants.INSTANCE_NAME, null); //this.idServer.getStringId(LegalPerson.class)+"_";
		if(this.prefix == null) {
			this.prefix = Config.getInstanceName();
			if(this.prefix.length() >= 3) {
				//把后面的数字去除，默认支持两位数字，也就是最多100个实例的高可用集群
				this.prefix = this.prefix.substring(0,this.prefix.length()-2);
			} else {
				this.prefix = this.prefix.substring(0,this.prefix.length()-1);
			}
		}
		
		String nodePath = this.dir +"/" + this.prefix;
		op.createNode(nodePath, "", IDataOperator.EPHEMERAL_SEQUENTIAL);
		//this.seq = Long.parseLong(getSeq(op.getData(nodePath)));
		
		op.addChildrenListener(this.dir, (type,parent,child,data)->{
			
			if(seq == -1 && child.startsWith(this.prefix)) {
				this.seq = getSeq(child);
			}
			
			if(type == IListener.REMOVE) {
				long dm = getSeq(child);
				if(masterSeq != -1 && dm == masterSeq) {
					lockStatu = true;
					logger.info("Master [{}] offline, dir: {}", dm,dir);
					//主节点下线
					notify(IMasterChangeListener.MASTER_OFFLINE, this.isMaster);
					//进入待选主状态
					long cTime = System.currentTimeMillis();
					long t = Long.parseLong(op.getData(dir));
					if(cTime - t > timeout) {
						//上一任期的时间，说明时间没有更新
						//最先收到主节点下线通知，宣告重选主节点
						electionStartTime = System.currentTimeMillis();
						op.setData(dir, electionStartTime + "");
						logger.info("The start election time [{}] offline, dir: {}", dm,dir);
					}
					//startElectionWorker();
				}
			} else if(type == IListener.ADD) {
				//最先接收到通知的节点，不一定就是主节点，如多个系统同时增加节点1，2，3，收到通知的顺序可能是 3，1，2
				//那么首先收到通知的节点3肯定不能选为主节点
				//selectMaster();
			}
		});
		
		op.addDataListener(this.dir, (path,data) -> {
			this.electionStartTime = Long.parseLong(data);
			lockStatu = true;
			this.masterSeq = -1;
			notify(IMasterChangeListener.MASTER_OFFLINE, false);
			startElectionWorker();
		});
		
		startElectionWorker();
	}

	private void notify(int masterOffline, boolean b) {
		for(IMasterChangeListener l : this.listeners) {
			new Thread(()-> {
				l.masterChange(masterOffline, b);
			}).start();
		}
	}

	private long getSeq(String child) {
		return Long.parseLong(child.substring(this.prefix.length()));
	}

	private void startElectionWorker() {
		logger.info("Enter election status: " + this.dir);
		new Thread(()->{
			while(!doWorker()) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private boolean doWorker() {
		//等待指定时间后工始计票
		while(System.currentTimeMillis() - this.electionStartTime < timeout) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return selectMaster();
		
	}

	private boolean selectMaster() {
		long curTime = System.currentTimeMillis();
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
		}else {
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
