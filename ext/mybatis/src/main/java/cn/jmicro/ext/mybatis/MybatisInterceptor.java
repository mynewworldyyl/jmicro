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
package cn.jmicro.ext.mybatis;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.Holder;
import cn.jmicro.api.JMicroContext;
import cn.jmicro.api.Resp;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.Interceptor;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.exception.RpcException;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:30
 */
@Component(value="mybatisInterceptor",lazy=false,side=Constants.SIDE_PROVIDER)
@Interceptor
public class MybatisInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Logger logger = LoggerFactory.getLogger(MybatisInterceptor.class);
	
	@Inject
	private CurSqlSessionFactory curSqlSessionManager;
	
	@Inject(required=false)
	private ILocalTransactionResource ltr;
	
	public MybatisInterceptor() {}
	
	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest req) throws RpcException {
		IPromise<Object> p = null;
		
		ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm == null || sm.getTxType() == TxConstants.TYPE_TX_NO) {
			return handler.onRequest(req);
		}
		
		final Holder<Boolean> txOwner = new Holder<>(false);
		final Holder<Long> txid = new Holder<>(null);
		final SqlSession s = curSqlSessionManager.curSession() ;
		try {
			if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				Long tid = JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, null);
				if(tid == null) {
					if(!ltr.begin(sm)) {
						throw new RpcException(req,"fail to create transaction context,"+
								", Method: " + sm.getKey().toKey(true, true, true),Resp.CODE_TX_FAIL);
					}
					tid = JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, null);
					txOwner.set(true);
				}
				
				if(tid == null) {
					throw new RpcException(req,"transaction id not found,"+
							", Method: " + sm.getKey().toKey(true, true, true),Resp.CODE_TX_FAIL);
				}
				
				if(!ltr.takePartIn(tid,s)) {
					throw new RpcException(req,"fail to take part in transaction: " + tid+
							", Method: " + sm.getKey().toKey(true, true, true),Resp.CODE_TX_FAIL);
				}
				txid.set(tid);
			} else {
				txid.set(null);
				txOwner.set(false);
			}
			
			p = handler.onRequest(req);
			
			if(sm.getTxType() == TxConstants.TYPE_TX_LOCAL) {
				p.success((rst,cxt)->{
					boolean commit = true;
					if(rst != null && rst instanceof Resp) {
						Resp r = (Resp)rst;
						if(r.getCode() != 0) {
							commit = false;
						}
					}
					
					if(commit) {
						s.commit();
					}else {
						s.rollback();
					}
					
					s.close();
				})
				.fail((code,msg,cxt)->{
					s.rollback(true);
					s.close();
				});
			}else if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				p.success((rst,cxt)->{
					boolean commit = true;
					if(rst != null && rst instanceof Resp) {
						Resp r = (Resp)rst;
						if(r.getCode() != 0) {
							commit = false;
							if(!commit && LG.isLoggable(MC.LOG_WARN)) {
								LG.log(MC.LOG_WARN, this.getClass(), "Rollback transaction: " + txid.get()
								+",Method: " + sm.getKey().toKey(true, true, true));
							}
						}
					}
					
					if(!finishDistributedTransaction(sm,txid.get(),txOwner.get(),commit)) {
						LG.log(MC.LOG_ERROR, this.getClass(), "Fail to commit success transaction: " + txid.get()
						+",Method: " + sm.getKey().toKey(true, true, true));
					}
				}).fail((code,msg,cxt)->{
					if(LG.isLoggable(MC.LOG_WARN)) {
						LG.log(MC.LOG_WARN, this.getClass(), "Rollback transaction: " + txid.get()
						+" with error "+msg+" ,Method: " + sm.getKey().toKey(true, true, true));
					}
					finishDistributedTransaction(sm,txid.get(),txOwner.get(),false);
				});
			}
			return p;
		} catch (Throwable e) {
			LG.log(MC.LOG_ERROR, this.getClass(), "rollback transaction "+txid.get()+",Method: " + sm.getKey().toKey(true, true, true),e);
			if(sm.getTxType() == TxConstants.TYPE_TX_LOCAL) {
				curSqlSessionManager.rollbackAndCloseCurSession();
			}else if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				finishDistributedTransaction(sm,txid.get(),txOwner.get(),false);
			}
			throw e;
		}finally {
			if(s != null) {
				curSqlSessionManager.remove();
			}
		}
	}
	
	private boolean finishDistributedTransaction(ServiceMethod sm,Long tid,boolean txOwner, boolean commit) {
		
		if(tid == null) {
			LG.log(MC.LOG_ERROR, this.getClass(), "Transaction ID is NULL,Method: " + sm.getKey().toKey(true, true, true));
			return false;
		}
		
		if(!ltr.vote(tid, commit)) {
			ltr.rollback(tid);//投票失败，本地直接回滚事务
			LG.log(MC.LOG_ERROR, this.getClass(), "Fail to vote txid: " + tid+",Method: " + sm.getKey().toKey(true, true, true));
			return false;
		}
		
		if(txOwner) {
			if(!ltr.end(tid)) {
				ltr.rollback(tid);//本地直接回滚事务
				LG.log(MC.LOG_ERROR, this.getClass(), "Fail to commit txid: " + tid+",Method: " + sm.getKey().toKey(true, true, true));
				return false;
			}
		}
		
		return true;
	}

}
