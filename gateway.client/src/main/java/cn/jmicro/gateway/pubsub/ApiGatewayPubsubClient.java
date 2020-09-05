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

	public int callService(String topic, Object[] args,byte flag,Map<String,Object> itemContext) {
		return publishOneItem(item(topic,args,flag,itemContext));
	}

	public int publishString(String topic, String content,byte flag,Map<String, Object> itemContext) {
		return publishOneItem(item(topic,content,flag,itemContext));
	}

	public int publishBytes(String topic, byte[] content,byte flag,Map<String, Object> itemContext) {
		return publishOneItem(item(topic,content,flag,itemContext));
	}

	public int publishMutilItems(PSData[] items) {
		return pcs.publishMutilItems(items);
	}

	public int publishOneItem(PSData item) {
		return pcs.publishOneItem(item);
	}
	
	private PSData item(String topic, Object data,byte flag,Map<String, Object> itemContext) {
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(data);
		item.setContext(itemContext);
		item.setFlag(flag);
		return item;
	}
	
	public IPromise<Integer> callServiceJMAsync(String topic, Object[] args,byte flag,
			Map<String, Object> itemContext) {
		return pcs.publishOneItemJMAsync(item(topic,args,flag,itemContext));
	}

	public IPromise<Integer> publishStringJMAsync(
			String topic, String content,byte flag, Map<String, Object> itemContext,Object context) {
		return pcs.publishOneItemJMAsync(item(topic,content,flag,itemContext),context);
	}

	public IPromise<Integer> publishBytesJMAsync(
			String topic, byte[] content,byte flag,Map<String, Object> itemContext,Object context) {
		return pcs.publishOneItemJMAsync(item(topic,content,flag,itemContext),context);
	}

	public IPromise<Integer> publishMutilItemsJMAsync(PSData[] items,Object context) {
		return pcs.publishMutilItemsJMAsync(items,context);
	}

	public IPromise<Integer> publishOneItemJMAsync( PSData item,Object context) {
		return pcs.publishOneItemJMAsync(item,context);
	}
	
	public IPromise<Integer> callServiceJMAsync(String topic, Object[] args,byte flag,
			Map<String, Object> itemContext ,Object context) {
		return pcs.publishOneItemJMAsync(item(topic,args,flag,itemContext),context);
	}

	public IPromise<Integer> publishStringJMAsync(
			String topic, String content,byte flag,Map<String, Object> itemContext) {
		return pcs.publishOneItemJMAsync(item(topic,content,flag,itemContext));
	}

	public IPromise<Integer> publishBytesJMAsync(
			String topic, byte[] content,byte flag,Map<String, Object> itemContext) {
		return pcs.publishOneItemJMAsync(item(topic,content,flag,itemContext));
	}

	public IPromise<Integer> publishMutilItemsJMAsync(PSData[] items) {
		return pcs.publishMutilItemsJMAsync(items);
	}

	public IPromise<Integer> publishOneItemJMAsync(PSData item) {
		return pcs.publishOneItemJMAsync(item);
	}
	
	public IPromise<Integer> subscribeJMAsync(String topic,Map<String, Object> ctx, PSDataListener lis) {
		final PromiseImpl<Integer> p = new PromiseImpl<>();
		IAsyncCallback<Integer> cb = new IAsyncCallback<Integer>() {
			@Override
			public void onResult(Integer val, AsyncFailResult fail,Object ctx) {
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
			public void onResult(Boolean val, AsyncFailResult fail,Object ctx) {
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
