package cn.jmicro.choreography.cmd;

/**
 * 代表一个运行指令，由Controller分配，Agent负责执行
 * 
 * @author Yulei Ye
 * @date 2019年1月24日 上午9:32:29
 */
public class Cmd {

	private String cmdName;
	
	private String cmd;
	
	private String jvmOpts;
	
	private String mainClass;
	
	private String[] args;
	
	private String instancePrefix;
	
	private int count;

	public String getCmdName() {
		return cmdName;
	}

	public void setCmdName(String cmdName) {
		this.cmdName = cmdName;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getJvmOpts() {
		return jvmOpts;
	}

	public void setJvmOpts(String jvmOpts) {
		this.jvmOpts = jvmOpts;
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}

	public String getInstancePrefix() {
		return instancePrefix;
	}

	public void setInstancePrefix(String instancePrefix) {
		this.instancePrefix = instancePrefix;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
}
