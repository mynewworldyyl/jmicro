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
package org.jmicro.limit;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmicro.common.CommonException;
import org.jmicro.common.util.StringUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-上午11:48:43
 */
public class TokenBucket implements ITokenBucket{

	/**
	 * qps
	 */
	private int speed;
	
	/**
	 * 当前令牌总数
	 */
	private AtomicInteger currTokenNum = new AtomicInteger(0);
	
	/**
	 * 桶的大小
	 */
	private int maxToken = Integer.MAX_VALUE-1;
	
	private Timer timer = new Timer();
	
	private Queue<Integer> applyQueue = new ConcurrentLinkedQueue<>();
	
	private int maxQueueSize = 6;
	
	private String speedUnit="ms";
	
	public TokenBucket(String su) {this.speedUnit=su;}
	
	public TokenBucket(String su,int speed) {
		this.speedUnit=su;
		this.updateSpeed(speed);
	}
	
	@Override
	public int applyToken(int permits) {
		
		if(this.checkPermits(permits)){
			this.get(permits);
			//当前桶中有足够的令牌，不用等待直接返回
			return 0;
		}
		
		if(this.applyQueue.size() > maxQueueSize){
			//拒绝请求
			return -1;
		}
		
		int waitTime = howLong(permits);
		
		Integer p = new Integer(permits);
		applyQueue.offer(p);
		
		//System.out.println("Queue Size: "+ this.applyQueue.size());
		
		synchronized(p){
			try {
				p.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return waitTime;
	}

	@Override
	public int howLong(int permits) {
		return permits/this.speed;
	}

	@Override
	public void updateSpeed(int s) {
		if(this.speed == s) {
			//无更新
			return;
		}
		this.maxToken = s*5;//最大存储5秒钟内的令牌，超过的直接丢弃
		this.speed = s;
		
		timer.scheduleAtFixedRate(new TimerTask(){
			public void run() {
				doAdd();
			}
		}, 0, toMilliseconds(1));//每秒做一次增加操作
	}
	
	private int toMilliseconds(int s) {
		if(StringUtils.isEmpty(this.speedUnit)){
			return s;
		}
		switch(this.speedUnit.toLowerCase()){
		case "ms":
			return s;
		case "ns":
			return s/1000;
		case "s":
			return s*1000;
		case "m":
			return s*1000*60;
		case "h":
			return s*1000*60*60;
		}
		throw new CommonException("Invalid speed unit: "+ this.speedUnit);
	}

	private void doAdd() {
		if(currTokenNum.get() + speed <= maxToken){
			currTokenNum.getAndAdd(speed);
		}
		
		while(!applyQueue.isEmpty()){
			Integer ap = applyQueue.peek();
			if(checkPermits(ap)) {
				get(ap);
				synchronized(ap){
					applyQueue.poll();
					ap.notify();
				}
			} else {
				break;
			}
		}
	}

	private boolean checkPermits(int permits){
		if(permits <=0) {
			throw new CommonException("Invalid permits :"+ permits);
		}
		return currTokenNum.get() >= permits;
	}
	
	private void get(int permits){
		currTokenNum.addAndGet(-permits);
	}
}
