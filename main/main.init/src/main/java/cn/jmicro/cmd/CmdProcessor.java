package cn.jmicro.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.config.Config;
import cn.jmicro.api.idgenerator.ComponentIdServer;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.security.AccountManager;
import cn.jmicro.api.security.ActInfoJRso;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.common.util.StringUtils;

@Component
public class CmdProcessor {

	@Inject
	private IDataOperator op;
	
	@Inject
	private Config cfg;
	
	@Inject
	private ComponentIdServer idServer;
	
	@Inject
	private AccountManager am;
	
	public void cmdLoop(boolean inMain) {
		if(inMain) {
			this.run();
		}else {
			new Thread(this::run).start();
		}
	}
	
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		while(true) {
			try {
				System.out.println(CmdConstants.ECHO_TITLE);
				//System.out.println("\033[47;31mhello world\033[5m");
				line = br.readLine();
				if(StringUtils.isEmpty(line)) {
					continue;
				} else {
					exe(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void exe(String line) {
		
		Cmd c = parseCmd(line);
		
		if(!c.isSuccess()) {
			System.out.println(c.getResult());
		}
		
		switch(c.getCmd()) {
		case CmdConstants.CMD_ADD:
			handleAdd(c);
			break;
			
		case CmdConstants.CMD_HELP:
			handleHelp(c);
			break;
			
		case CmdConstants.CMD_EXIT:
		case CmdConstants.CMD_QUIT:
			System.out.println("Bye");
			System.exit(0);
			break;
		}
		
		if(!c.isSuccess()) {
			System.out.println(c.getResult());
		}
	}

	private Cmd parseCmd(String line) {
		Cmd c = new Cmd();
		c.setCmdStr(line);
		c.setSuccess(true);
		
		line = line.trim();
		String[] cs = line.split("\\s+");
		if(cs.length < 1) {
			c.setSuccess(false);
			c.setResult("Invalid command!");
			return c;
		}
		c.setCmd(cs[0]);
		
		if(cs.length < 2) {
			if(!validSingleCommand(c.getCmd())) {
				c.setSuccess(false);
				c.setResult("Invalid module name!");
			}
			return c;
		}
		
		if(!cs[1].startsWith(CmdConstants.OPT_PREFIX)) {
			c.setModule(cs[1]);
		}
		
		int oidx = 2;
		if(cs[1].startsWith(CmdConstants.OPT_PREFIX)) {
			oidx = 1;
		}
		
		Map<String,String> options = new HashMap<>();
		
		for(; oidx < cs.length; oidx++ ) {
			
			String opt = cs[oidx];
			if(!opt.startsWith(CmdConstants.OPT_PREFIX)) {
				c.setResult("Invalid option format: " + cs[oidx]);
				c.setSuccess(false);
				return c;
			}
			
			opt = opt.substring(CmdConstants.OPT_PREFIX.length());
			String[] os = opt.split("\\s*=\\s*");
			if(os == null || os.length == 0) {
				c.setResult("Invalid option format: " + cs[oidx]);
				c.setSuccess(false);
				return c;
			}
			
			if(os.length == 1) {
				options.put(os[0], null);
			}else {
				options.put(os[0], os[1]);
			}
			
		}
		
		c.setOptions(options);
		
		return c;
	}
	
	private boolean validSingleCommand(String cmd) {
		return cmd.equals(CmdConstants.CMD_HELP) || 
				cmd.equals(CmdConstants.CMD_QUIT) ||
				cmd.equals(CmdConstants.CMD_EXIT);
	}

	private void handleHelp(Cmd c) {
		
	}

	private void handleAdd(Cmd c) {
		switch(c.getModule()) {
		case CmdConstants.CMD_MODULE_PERMISSION:
			handleAddPermission(c);
			break;		
		default:
			System.out.println("Not support module: " + c.getModule());
		}
	}

	private void handleAddPermission(Cmd c) {
		String actName = c.getOptions().get("actName");
		if(StringUtils.isEmpty(actName)) {
			c.setResult("Account name cannot be NULL!");
			c.setSuccess(false);
			return;
		}
		
		String permission = c.getOptions().get("permission");
		if(StringUtils.isEmpty(permission)) {
			c.setResult("Permission to add cannot be NULL!");
			c.setSuccess(false);
			return;
		}
		
		Integer hCode= Integer.parseInt(permission);
		
		ActInfoJRso ai = this.am.getAccountFromZK(actName);
		if(ai == null) {
			c.setResult("Account with name " + actName +" not found!");
			c.setSuccess(false);
			return;
		}

		ai.getPers().add(hCode);
		String p = AccountManager.ActDir +"/"+ ai.getActName();
		op.createNodeOrSetData(p, JsonUtils.getIns().toJson(ai), IDataOperator.PERSISTENT);
	
	}
	
	public static final class Cmd{
		private String cmdStr;
		private String result;
		private boolean success;
		private String cmd;
		private String module;
		private Map<String,String> options = new HashMap<>();
		
		public String getModule() {
			return module;
		}
		public void setModule(String module) {
			this.module = module;
		}
		
		public String getCmdStr() {
			return cmdStr;
		}
		public void setCmdStr(String cmdStr) {
			this.cmdStr = cmdStr;
		}
		public String getResult() {
			return result;
		}
		public void setResult(String result) {
			this.result = result;
		}
		public boolean isSuccess() {
			return success;
		}
		public void setSuccess(boolean success) {
			this.success = success;
		}
		public String getCmd() {
			return cmd;
		}
		public void setCmd(String cmd) {
			this.cmd = cmd;
		}
		public Map<String, String> getOptions() {
			return options;
		}
		public void setOptions(Map<String, String> options) {
			this.options = options;
		}
		
	}
}
