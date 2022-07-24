
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
package cn.jmicro.gateway.fs;

import java.io.File;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.RespJRso;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.codec.ICodecFactory;
import cn.jmicro.api.internal.async.Promise;
import cn.jmicro.api.net.IMessageHandler;
import cn.jmicro.api.net.ISession;
import cn.jmicro.api.net.Message;
import cn.jmicro.api.objectfactory.IObjectFactory;
import cn.jmicro.api.service.ServiceInvokeManager;
import cn.jmicro.api.storage.FileJRso;
import cn.jmicro.api.storage.IFileStorage;
import cn.jmicro.common.Constants;

/**
 * 基于二进制流的大对象存储，如文件
 * 将数据存储到文件中，最终以文件为参数调用特定的服务方法。
 * @author Yulei Ye
 * @date 2022年7月16日
 */
@Component(side=Constants.SIDE_PROVIDER)
public class ObjectStorageMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ObjectStorageMessageHandler.class);
	
	public static final String TAG = ObjectStorageMessageHandler.class.getName();
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private FileUploadManager fileMng;
	
	@Inject
	private ServiceInvokeManager srvInvoke;
	
	@Inject
	private IObjectFactory of;
	
	@Override
	public boolean onMessage(ISession session, Message msg) {
		int ph = msg.getExtra(Message.EXTRA_KEY_EXT0);
		ByteBuffer data = null;
		boolean async = false;
		try {
			msg.setType(Constants.MSG_TYPE_OBJECT_STORAGE_RESP);
			if(IFileStorage.TYPE_INIT == ph) {
				FileJRso fr = ICodecFactory.decode(codecFactory, msg.getPayload(), FileJRso.class, msg.getUpProtocol());
				RespJRso<FileJRso> resp = fileMng.addFile(fr);
				msg.putExtra(Message.EXTRA_KEY_EXT1, fr.getId());
				data = ICodecFactory.encode(this.codecFactory, resp, msg.getDownProtocol());
			}else if(IFileStorage.TYPE_NEXT == ph) {
				String fid = msg.getExtra(Message.EXTRA_KEY_EXT1);
				Integer blockNum = msg.getExtra(Message.EXTRA_KEY_EXT2);
				byte[] ud = ((ByteBuffer)msg.getPayload()).array();
				RespJRso<FileJRso> r = fileMng.addFileData(fid, ud, blockNum);
				r.setData(null);
				data = ICodecFactory.encode(this.codecFactory, r, msg.getDownProtocol());
			}else if(IFileStorage.TYPE_FINISH == ph) {
				String fid = msg.getExtra(Message.EXTRA_KEY_EXT1);
				Integer blockNum = msg.getExtra(Message.EXTRA_KEY_EXT2);
				byte[] ud = ((ByteBuffer)msg.getPayload()).array();
				final RespJRso<FileJRso> r = fileMng.addFileData(fid, ud, blockNum);
				try{
					RespJRso<String> rr = null;
					if(r.getCode() == RespJRso.CODE_SUCCESS) {
						 async = true;
						 notifyService(r.getData())
						 .success((rst,cxt)->{
							 try {
								 ByteBuffer ds = ICodecFactory.encode(this.codecFactory, rst, msg.getDownProtocol());
								 msg.setPayload(ds);
								 session.write(msg);
							} catch (Throwable e) {
								 logger.error("FileId:"+fid,e);
								 RespJRso<String> er = new RespJRso<>(RespJRso.CODE_FAIL,fid);
								 ByteBuffer ds = ICodecFactory.encode(this.codecFactory, er, msg.getDownProtocol());
								 msg.setPayload(ds);
								 session.write(msg);
							}finally {
								 File f = new File(r.getData().getLocalPath());
								 if(f.exists()) f.delete();
							}
						 });
					} else {
						rr = new RespJRso<String>();
						rr.setCode(r.getCode());
						data = ICodecFactory.encode(this.codecFactory, rr, msg.getDownProtocol());
					}
				} finally {
					if(!async && r.getData() != null && r.getData().getLocalPath() != null) {
						//删除临时文件
						File f = new File(r.getData().getLocalPath());
						if(f.exists()) f.delete();
					}
				}
			}
		} catch (Exception e) {
			 logger.error(msg.toString(),e);
			 RespJRso<?> rr = new RespJRso<>(RespJRso.CODE_FAIL,e.getMessage());
			 data = ICodecFactory.encode(this.codecFactory, rr, Message.PROTOCOL_BIN);
		}
		
		if(!async) {
			 msg.setPayload(data);
			 session.write(msg);
		}
		
		return true;
	}

	private IPromise<RespJRso<String>> notifyService(FileJRso file) {
		Integer smCode = file.getMcode();
		if(smCode == null || smCode == 0) {
			//只做文件上传
			RespJRso<String> rr = new RespJRso<>(RespJRso.CODE_SUCCESS,file.getId());
			rr.setMsg("服务编码不能为空");
			//logger.error(rr.getMsg());
			return new Promise<RespJRso<String>>((suc,fail)->{
				suc.success(rr);
			});
		}else {
			return srvInvoke.call(smCode, new Object[] {file});
		}
	}

	@Override
	public Byte type() {
		return Constants.MSG_TYPE_OBJECT_STORAGE;
	}
	
}
