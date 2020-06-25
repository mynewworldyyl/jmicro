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

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年4月4日
 */
@SO
public class OneItem {

	private short type = 0;
	
	private int num = 1;
	private long val = 0;
	
	private String desc=null;
	private long time = 0;
	
	private String tag = null;
	private byte level = MC.LOG_NO;
	
	private String ex = null;
	
	public OneItem() {}
	
	public OneItem(short type) {
		this.type = type;
		this.time = System.currentTimeMillis();
	}
	
	public OneItem(short type,String tag,String desc) {
		this.type = type;
		this.tag = tag;
		this.desc = desc;
		this.time = System.currentTimeMillis();
	}
	
	public void doAdd(int val,double num) {
		this.num += num;
		this.val += val;
	}
	
	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
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

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public long getVal() {
		return val;
	}

	public void setVal(long val) {
		this.val = val;
	}

	public String getEx() {
		return ex;
	}

	public void setEx(String ex) {
		this.ex = ex;
	}

}
