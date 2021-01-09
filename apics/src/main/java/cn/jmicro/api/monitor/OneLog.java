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
package cn.jmicro.api.monitor;

import cn.jmicro.api.annotation.SO;
import cn.jmicro.api.utils.TimeUtils;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年4月4日
 */
@SO
public class OneLog {
	
	private long time = 0;
	
	private short type = 0;
	
	private byte level = MC.LOG_NO;
	
	private String tag = null;
	
	private String desc = null;
	
	private int lineNo = -1;
	
	private String fileName;
	
	private String ex = null;
	
	public OneLog() {}
	
	public OneLog(byte level,String tag,String desc,String ex) {
		this(level, tag, desc);
		this.ex = ex;
	}
	
	public OneLog(byte level,String tag,String desc) {
		this.tag = tag;
		this.desc = desc;
		this.time = TimeUtils.getCurTime();
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public byte getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public String getEx() {
		return ex;
	}

	public void setEx(String ex) {
		this.ex = ex;
	}

	public int getLineNo() {
		return lineNo;
	}

	public void setLineNo(int lineNo) {
		this.lineNo = lineNo;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "OneLog [time=" + time + ", type=" + type + ", level=" + level + ", tag=" + tag + ", desc=" + desc
				+ ", lineNo=" + lineNo + ", fileName=" + fileName + ", ex=" + ex + "]";
	}

}
