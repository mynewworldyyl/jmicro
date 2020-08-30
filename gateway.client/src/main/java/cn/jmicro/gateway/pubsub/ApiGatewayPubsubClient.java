package cn.jmicro.gateway.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.async.AsyncFailResult;
import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.client.IAsyncCallback;
import cn.jmicro.api.internal.async.PromiseImpl;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.genclient.IPubSubClientService$JMAsyncClient;
import cn.jmicro.gateway.client.ApiGatewayClient;

public class ApiGatewayPubsubClient {

	private Map<String,Set<PSDataListener>> listeners = new HashMap<>();
	
	private ApiGatewayClient apiGc;
	
	private String messageServiceImplName = "cn.jmicro.gateway.MessageServiceImpl";
	
	private IPubSubClientService$JMAsyncClient pcs;
	
	public ApiGatewayPubsubClient(ApiGatewayClient client) {
		this.apiGc = client;
		pcs = client.getService(IPubSubClientService$JMAsyncClient.class,"mng", "0.0.1");
	}

	public int callService(String topic, Object[] args) {
		return pcs.callService(topic, args);
	}

	public int publishString(Map<String, Object> itemContext, String topic, String content) {
		return pcs.publishString(itemContext, topic, content);
	}

	public int publishBytes(Map<String, Object> itemContext, String topic, byte[] content) {
		return pcs.publishBytes(itemContext, topic, content);
	}

	public int publishMutilItems(PSData[] items) {
		return pcs.publishMutilItems(items);
	}

	public int publishOneItem(PSData item) {
		return pcs.publishOneItem(item);
	}
	
	public IPromise<Integer> callServiceJMAsync(Map<String, Object> context, String topic, Object[] args) {
		return pcs.callServiceJMAsync(context, topic, args);
	}

	public IPromise<Integer> publishStringJMAsync(Map<String, Object> context, Map<String, Object> itemContext,
			String topic, String content) {
		return pcs.publishStringJMAsync(context, itemContext, topic, content);
	}

	public IPromise<Integer> publishBytesJMAsync(Map<String, Object> context, Map<String, Object> itemContext,
			String topic, byte[] content) {
		return pcs.publishBytesJMAsync(context, itemContext, topic, content);
	}

	public IPromise<Integer> publishMutilItemsJMAsync(Map<String, Object> context, PSData[] items) {
		return pcs.publishMutilItemsJMAsync(context, items);
	}

	public IPromise<Integer> publishOneItemJMAsync(Map<String, Object> context, PSData item) {
		return pcs.publishOneItemJMAsync(context, item);
	}
	
	public IPromise<Integer> subscribeJMAsync(String topic,Map<String, Object> ctx, PSDataListener lis) {
		final PromiseImpl<Integer> p = new PromiseImpl<>();
		IAsyncCallback<Integer> cb = new IAsyncCallback<Integer>() {
			@Override
			public void onResult(Integer val, AsyncFailResult fail,Map<String,Object> ctx) {
				if(fail == null) {
					Set<PSDataListener> ls = listeners.get(topic);
					if(ls == null) {
						ls = new HashSet<>();
						listeners.put(topic, ls);
					}
					lis.setSubId(val);
					ls.add(lis);
				} else {
					p.setFail(fail);
				}
				p.done();
			}
		};
		this.apiGc.callService(messageServiceImplName, "mng", "0.0.1", "subscribe", 
				Integer.class, new Object[] {topic, ctx}, cb);
		return p;
	}
	
	public IPromise<Boolean>  unsubscribeJMAsync(String topic, PSDataListener lis) {
		final PromiseImpl<Boolean> p = new PromiseImpl<>();
		IAsyncCallback<Boolean> cb = new IAsyncCallback<Boolean>() {
			@Override
			public void onResult(Boolean val, AsyncFailResult fail,Map<String,Object> ctx) {
				if(fail == null) {
					Set<PSDataListener> ls = listeners.get(topic);
					if(ls != null && !ls.isEmpty()) {
						ls.remove(lis);
					}
					if(ls.isEmpty()) {
						listeners.remove(topic);
					}
					p.setResult(val);
				} else {
					p.setFail(fail);
				}
				p.done();
			}
		};
		this.apiGc.callService(messageServiceImplName, "mng", "0.0.1", "unsubscribe", 
				Integer.class, new Object[] {lis.getSubId()}, cb);
		return p;
	}

	public void onMsg(PSData item) {
		if(this.listeners.containsKey(item.getTopic())) {
			Set<PSDataListener> ls = this.listeners.get(item.getTopic());
			if(ls != null && !ls.isEmpty()) {
				for(PSDataListener l : ls) {
					l.onMsg(item);
				}
			}
		}
	}
	
}
