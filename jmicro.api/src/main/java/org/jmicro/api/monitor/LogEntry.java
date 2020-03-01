/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.api.monitor;

import org.jmicro.api.annotation.SO;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年2月29日
 */
@SO
public class LogEntry {

	//日志级别
	private byte level = MonitorConstant.LOG_DEBUG;
	
	private transient Throwable ex = null;
	
	private String exMsg;
	
	private String tagCls = null;
	
	private Object[] others;
	
	public LogEntry() {
		
	}
	
	public LogEntry(byte level,String tagCls) {
		this.level = level;
		this.tagCls = tagCls;
	}
	
	public LogEntry(byte level,String tagCls,Object...others) {
		this.level = level;
		this.tagCls = tagCls;
		this.others = others;
	}
	
	public LogEntry(byte level,String tagCls,Throwable ex, Object...others) {
		this(level,tagCls,others);
		this.ex = ex;
		if(ex != null) {
			this.exMsg = ex.getMessage();
		}
	}
	
	
	public byte getLevel() {
		return level;
	}
	public void setLevel(byte level) {
		this.level = level;
	}
	public Throwable getEx() {
		return ex;
	}
	public void setEx(Throwable ex) {
		this.ex = ex;
	}
	
	public String getTagCls() {
		return tagCls;
	}
	public void setTagCls(String tagCls) {
		this.tagCls = tagCls;
	}

	public Object[] getOthers() {
		return others;
	}

	public void setOthers(Object[] others) {
		this.others = others;
	}

	public String getExMsg() {
		return exMsg;
	}

	@Override
	public String toString() {
		return "LogEntry [level=" + level + ", exMsg=" + exMsg + ", tagCls=" + tagCls + ", desc=" + others + "]";
	}
	
}
