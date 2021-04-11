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
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.monitor.LG;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.net.AbstractInterceptor;
import cn.jmicro.api.net.IInterceptor;
import cn.jmicro.api.net.IRequest;
import cn.jmicro.api.net.IRequestHandler;
import cn.jmicro.api.registry.ServiceMethod;
import cn.jmicro.api.tx.TxConstants;
import cn.jmicro.api.utils.TimeUtils;
import cn.jmicro.common.Constants;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:05:30
 */
@Component(value="mybatisInterceptor",lazy=false,side=Constants.SIDE_PROVIDER)
@Interceptor
public class MybatisInterceptor extends AbstractInterceptor implements IInterceptor{

	private final static Class<?> TAG = MybatisInterceptor.class;
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
		final Holder<PromiseImpl<Object>> asyP = new Holder<>(null);
		
		final SqlSession s = curSqlSessionManager.curSession();
		
		boolean isDebug = LG.isLoggable(MC.LOG_DEBUG);
		long bt = TimeUtils.getCurTime(isDebug);
		try {
			s.getConnection().setTransactionIsolation(sm.getTxIsolation());
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
						s.commit(true);
					}else {
						s.rollback(true);
					}
					
					s.close();
					if(isDebug) {
						LG.log(MC.LOG_DEBUG, TAG, "End tx: " +txid.get()+", Cost: "+ (TimeUtils.getCurTime(true) - bt));
					}
				})
				.fail((code,msg,cxt)->{
					s.rollback(true);
					s.close();
					LG.log(MC.LOG_ERROR, TAG, "rollback tx: " +txid.get()+", Cost: "+ (TimeUtils.getCurTime(true) - bt)+", with error: " + msg);
				});
			}else if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				
				PromiseImpl<Object> pa = null;
				if(txOwner.get()) {
					pa = new PromiseImpl<>();
					asyP.set(pa);
					pa.setContext(p.getContext());
					pa.setResult(p.getResult());
					pa.setFail(p.getFailCode(), p.getFailMsg());
					pa.setResultType(p.resultType());
				}
				
				p.success((rst,cxt)->{
					boolean commit = true;
					if(rst != null && rst instanceof Resp) {
						Resp r = (Resp)rst;
						if(r.getCode() != 0) {
							commit = false;
							if(!commit && LG.isLoggable(MC.LOG_WARN)) {
								LG.log(MC.LOG_WARN, TAG, "Rollback transaction: " + txid.get()
								+",Method: " + sm.getKey().toKey(true, true, true));
							}
						}
					}
					
					if(txOwner.get()) {
						//选注册事务回调再投标票，确保能收到事务提交结果通知
						if(commit) {
							//需要等待事务提交结果
							ownerEnd(sm,txid.get(),asyP.get());
						} else {
							//事务回滚不再需要等待
							asyP.get().setFail(Resp.CODE_TX_FAIL, "tx fail");
							asyP.get().done();
							ownerEnd(sm,txid.get(),null);
						}
					}
					
					if(!doVote(sm,txid.get(),commit)) {
						if(asyP.get() != null) {
							asyP.get().setFail(Resp.CODE_TX_FAIL, "vote fail");
							//投票失败,不再需要等事务回调
							asyP.get().done();
						}
					}
					
					if(isDebug) {
						LG.log(MC.LOG_DEBUG, TAG, "End tx: " +txid.get()+", Cost: "+ (TimeUtils.getCurTime(true) - bt));
					}
				}).fail((code,msg,cxt)->{
					String msg0 = "Rollback transaction: " + txid.get()+", Cost: "+(TimeUtils.getCurTime(true) - bt)
					+" with error "+msg+" ,Method: " + sm.getKey().toKey(true, true, true);
					LG.log(MC.LOG_WARN, TAG, msg0);
					logger.warn(msg0);
					
					doVote(sm,txid.get(),false);
					
					if(txOwner.get()) {
						//事务回滚不再需要等待
						asyP.get().setFail(code, msg);
						asyP.get().done();
						ownerEnd(sm,txid.get(),null);
					}
					
				});
				
				if(txOwner.get()) {
					return pa;
				}
			}
			return p;
		} catch (Throwable e) {
			String msg = "rollback transaction "+txid.get()+", Cost: "+(TimeUtils.getCurTime(true) - bt)+",Method: " + sm.getKey().toKey(true, true, true);
			logger.error(msg,e);
			LG.log(MC.LOG_ERROR, TAG, msg,e);
			if(sm.getTxType() == TxConstants.TYPE_TX_LOCAL) {
				curSqlSessionManager.rollbackAndCloseCurSession();
			}else if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				doVote(sm,txid.get(),false);
				if(txOwner.get()) {
					ownerEnd(sm,txid.get(),null);
					if(asyP.get() != null) {
						asyP.get().setFail(Resp.CODE_TX_FAIL, "vote fail");
						//投票失败,不再需要等事务回调
						asyP.get().done();
					}
				}
			}
			throw new RpcException(req,e,Resp.CODE_TX_FAIL);
		}finally {
			if(s != null) {
				curSqlSessionManager.remove();
			}
		}
	}
	
	private boolean doVote(ServiceMethod sm,Long tid,boolean commit) {
		if(tid == null) {
			LG.log(MC.LOG_ERROR, TAG, "Transaction ID is NULL,Method: " + sm.getKey().toKey(true, true, true));
			return false;
		}
		
		if(!ltr.vote(tid, commit)) {
			//投票失败，本地直接回滚事务
			//ltr.rollback(tid);
			LG.log(MC.LOG_ERROR, TAG, "Fail to vote txid: " + tid+",Method: " + sm.getKey().toKey(true, true, true));
			return false;
		}
		return true;
	}
			
	
	private void ownerEnd(ServiceMethod sm,Long tid,PromiseImpl<Object> asyP) {
		if(LG.isLoggable(MC.LOG_INFO)) {
			String msg = "Wait tx finish txid: " + JMicroContext.get().getLong(TxConstants.TYPE_TX_KEY, -1L);
			LG.log(MC.LOG_INFO, TAG, msg);
		}
		ltr.waitTxFinish(tid,asyP);
	}

}
