package cn.jmicro.api.mng;

import java.util.ArrayList;
import java.util.List;

import cn.jmicro.api.monitor.JMLogItem;

public class LogEntry {

	//client request and response item
	private JMLogItem item;
	
	private List<JMLogItem> providerItems = null;
	
	//reqId
	private String id;
	
	private LogEntry parent;
	
	private List<LogEntry> children = new ArrayList<>();

	public LogEntry() {}
	
	public LogEntry(JMLogItem item) {
		this.item = item;
	}
	
	public JMLogItem getItem() {
		return item;
	}

	public void setItem(JMLogItem item) {
		this.item = item;
	}

	public List<JMLogItem> getProviderItems() {
		return providerItems;
	}

	public void setProviderItems(List<JMLogItem> providerItems) {
		this.providerItems = providerItems;
	}

	public List<LogEntry> getChildren() {
		return children;
	}

	public void setChildren(List<LogEntry> children) {
		this.children = children;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LogEntry getParent() {
		return parent;
	}

	public void setParent(LogEntry parent) {
		this.parent = parent;
	}
	
}
