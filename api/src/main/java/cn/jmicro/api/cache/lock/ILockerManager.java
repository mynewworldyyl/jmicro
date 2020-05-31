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
package cn.jmicro.api.cache.lock;

import cn.jmicro.common.CommonException;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月12日
 */
public interface ILockerManager {
	ILocker getLocker(String resource);
	
	public static void doInlocker(String path, ILockerManager lockMgn,Runnable r) {
		ILockerManager.doInlocker(path, lockMgn, 30*1000, r);
	}
	
	public static void doInlocker(String path, ILockerManager lockMgn,long timeoutInMillis, Runnable r) {
		ILockerManager.doInlocker(path, lockMgn, timeoutInMillis,500, r);
	}
	
	public static void doInlocker(String path, ILockerManager lockMgn,long timeoutInMillis,
			int checkInterval,Runnable r) {
		
		if(StringUtils.isEmpty(path)) {
			throw new CommonException("Resource path cannot be NULL");
		}
		
		ILocker locker = null;
		boolean success = false;
		try {
			locker = lockMgn.getLocker(path);
			if(success = locker.tryLock(checkInterval,timeoutInMillis)) {
				r.run();
			} else {
				throw new CommonException("Fail to get locker:" + path);
			}
		}finally {
			if(locker != null && success) {
				locker.unLock();
			}
		}
	}
	
}
