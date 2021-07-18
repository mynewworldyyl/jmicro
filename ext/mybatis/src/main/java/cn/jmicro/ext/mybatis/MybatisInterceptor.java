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
import cn.jmicro.api.RespJRso;
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
import cn.jmicro.api.registry.ServiceMethodJRso;
import cn.jmicro.api.tx.ICurTxSessionFactory;
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
	private ICurTxSessionFactory curSqlSessionManager;
	
	@Inject(required=false)
	private ILocalTransactionResource ltr;
	
	public MybatisInterceptor() {}
	
	@Override
	public IPromise<Object> intercept(IRequestHandler handler, IRequest req) throws RpcException {
		final IPromise<Object> p;
		
		ServiceMethodJRso sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
		if(sm == null || sm.getTxType() == TxConstants.TYPE_TX_NO) {
			return handler.onRequest(req);
		}
		
		final Holder<Boolean> txOwner = new Holder<>(false);
		final Holder<Long> txid = new Holder<>(null);
		final Holder<PromiseImpl<Object>> asyP = new Holder<>(null);
		
		final SqlSession s = (SqlSession)curSqlSessionManager.curSession();
		
		boolean isDebug = LG.isLoggable(MC.LOG_DEBUG);
		long bt = TimeUtils.getCurTime(isDebug);
		try {
			s.getConnection().setTransactionIsolation(sm.getTxIsolation());
			if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				Long tid = JMicroContext.get().getLong(TxConstants.TX_ID, null);
				if(tid == null) {
					if(!ltr.begin(sm)) {
						throw new RpcException(req,"fail to create transaction context,"+
								", Method: " + sm.getKey().fullStringKey(),RespJRso.CODE_TX_FAIL);
					}
					tid = JMicroContext.get().getLong(TxConstants.TX_ID, null);
					txOwner.set(true);
				}
				
				if(tid == null) {
					throw new RpcException(req,"transaction id not found,"+
							", Method: " + sm.getKey().fullStringKey(),RespJRso.CODE_TX_FAIL);
				}
				
				if(!ltr.takePartIn(tid,sm.getTxPhase(),s)) {
					throw new RpcException(req,"fail to take part in transaction: " + tid+
							", Method: " + sm.getKey().fullStringKey(),RespJRso.CODE_TX_FAIL);
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
					if(rst != null && rst instanceof RespJRso) {
						RespJRso r = (RespJRso)rst;
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
				}
				
				p.success((rst,cxt)->{
					PromiseImpl<Object> pa0 = asyP.get();
					if(pa0 != null) {
						pa0.setContext(p.getContext());
						pa0.setResult(rst);
						pa0.setResultType(p.resultType());
					}
					
					boolean commit = true;
					if(rst != null && rst instanceof RespJRso) {
						RespJRso r = (RespJRso)rst;
						if(r.getCode() != 0) {
							commit = false;
							if(!commit && LG.isLoggable(MC.LOG_WARN)) {
								LG.log(MC.LOG_WARN, TAG, "Rollback transaction: " + txid.get()
								+",Method: " + sm.getKey().fullStringKey());
							}
							
							if(txOwner.get()) {
								asyP.get().setFail(r.getCode(), r.getMsg());
							}
							
						}
					}
					
					if(txOwner.get()) {
						//选注册事务回调再投标票，确保能收到事务提交结果通知
						if(commit) {
							//需要等待事务提交结果
							ownerEnd(sm,txid.get(),pa0);
						} else {
							//事务回滚不再需要等待
							asyP.get().done();
							ownerEnd(sm,txid.get(),null);
						}
					}
					
					if(!doVote(sm,txid.get(),commit)) {
						if(asyP.get() != null) {
							asyP.get().setFail(RespJRso.CODE_TX_FAIL, "vote fail");
							//投票失败,不再需要等事务回调
							asyP.get().done();
						}
					}
					
					if(isDebug) {
						LG.log(MC.LOG_DEBUG, TAG, "End tx: " +txid.get()+", Cost: "+ (TimeUtils.getCurTime(true) - bt));
					}
				}).fail((code,msg,cxt)->{
					String msg0 = "Rollback transaction: " + txid.get()+", Cost: "+(TimeUtils.getCurTime(true) - bt)
					+" with error "+msg+" ,Method: " + sm.getKey().fullStringKey();
					LG.log(MC.LOG_WARN, TAG, msg0);
					logger.warn(msg0);
					
					doVote(sm,txid.get(),false);
					
					if(txOwner.get()) {
						PromiseImpl<Object> pa0 = asyP.get();
						pa0.setContext(p.getContext());
						pa0.setResult(p.getResult());
						pa0.setResultType(p.resultType());
						//事务回滚不再需要等待
						pa0.setFail(code, msg);
						pa0.done();
						ownerEnd(sm,txid.get(),null);
					}
					
				});
				
				if(txOwner.get()) {
					return pa;
				}
			}
			return p;
		} catch (Throwable e) {
			String msg = "rollback transaction "+txid.get()+", Cost: "+(TimeUtils.getCurTime(true) - bt)
					+",Method: " + sm.getKey().fullStringKey();
			logger.error(msg,e);
			LG.log(MC.LOG_ERROR, TAG, msg,e);
			if(sm.getTxType() == TxConstants.TYPE_TX_LOCAL) {
				curSqlSessionManager.rollbackAndCloseCurSession();
			}else if(sm.getTxType() == TxConstants.TYPE_TX_DISTRIBUTED) {
				doVote(sm,txid.get(),false);
				if(txOwner.get()) {
					ownerEnd(sm,txid.get(),null);
					if(asyP.get() != null) {
						asyP.get().setFail(RespJRso.CODE_TX_FAIL, "vote fail");
						//投票失败,不再需要等事务回调
						asyP.get().done();
					}
				}
			}
			throw new RpcException(req,e,RespJRso.CODE_TX_FAIL);
		}finally {
			if(s != null) {
				curSqlSessionManager.remove();
			}
		}
	}
	
	private boolean doVote(ServiceMethodJRso sm,Long tid,boolean commit) {
		if(tid == null) {
			LG.log(MC.LOG_ERROR, TAG, "Transaction ID is NULL,Method: " + sm.getKey().fullStringKey());
			return false;
		}
		
		if(!ltr.vote(tid, commit)) {
			//投票失败，本地直接回滚事务
			//ltr.rollback(tid);
			LG.log(MC.LOG_ERROR, TAG, "Fail to vote txid: " + tid+",Method: " + sm.getKey().fullStringKey());
			return false;
		}
		return true;
	}
			
	
	private void ownerEnd(ServiceMethodJRso sm,Long tid,PromiseImpl<Object> asyP) {
		if(LG.isLoggable(MC.LOG_INFO)) {
			String msg = "Wait tx finish txid: " + JMicroContext.get().getLong(TxConstants.TX_ID, -1L);
			LG.log(MC.LOG_INFO, TAG, msg);
		}
		ltr.waitTxFinish(tid,asyP);
	}

}
