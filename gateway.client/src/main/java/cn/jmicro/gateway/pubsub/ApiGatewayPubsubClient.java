package cn.jmicro.gateway.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.PSDataJRso;
import cn.jmicro.api.pubsub.genclient.IPubSubClientService$JMAsyncClient;
import cn.jmicro.common.Constants;
import cn.jmicro.gateway.client.ApiGatewayClient;

public class ApiGatewayPubsubClient {

	private Map<String,Set<PSDataListener>> listeners = new HashMap<>();
	
	private ApiGatewayClient apiGc;
	
	//public static String messageServiceImplName = "cn.jmicro.gateway.MessageServiceImpl";
	
	private IPubSubClientService$JMAsyncClient pcs;
	
	public ApiGatewayPubsubClient(ApiGatewayClient client) {
		this.apiGc = client;
		pcs = client.getService(IPubSubClientService$JMAsyncClient.class,ApiGatewayClient.NS_PUBSUB, "0.0.1");
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

	public int publishMutilItems(PSDataJRso[] items) {
		return pcs.publishMutilItems(items);
	}

	public int publishOneItem(PSDataJRso item) {
		return pcs.publishOneItem(item);
	}
	
	private PSDataJRso item(String topic, Object data,byte flag,Map<String, Object> itemContext) {
		PSDataJRso item = new PSDataJRso();
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

	public IPromise<Integer> publishMutilItemsJMAsync(PSDataJRso[] items,Object context) {
		return pcs.publishMutilItemsJMAsync(items,context);
	}

	public IPromise<Integer> publishOneItemJMAsync( PSDataJRso item,Object context) {
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

	public IPromise<Integer> publishMutilItemsJMAsync(PSDataJRso[] items) {
		return pcs.publishMutilItemsJMAsync(items);
	}

	public IPromise<Integer> publishOneItemJMAsync(PSDataJRso item) {
		return pcs.publishOneItemJMAsync(item);
	}
	
	public IPromise<Integer> subscribeJMAsync(String topic,Map<String, Object> ctx, PSDataListener lis) {
		
		Map<String,Object> params = ctx;
		if(params == null) {
			params = new HashMap<>();
		}
		
		params.put("topic", topic);
		params.put("op", 1);
		
		final IPromise<Integer> p = this.apiGc.sendMessage(Constants.MSG_TYPE_PUBSUB, params, Integer.class);
		p.success((rst,cxt0)->{
			Set<PSDataListener> ls = listeners.get(topic);
			if(ls == null) {
				ls = new HashSet<>();
				listeners.put(topic, ls);
			}
			lis.setSubId(rst);
			ls.add(lis);
		})
		.fail((code,err,cxt0)->{
			System.out.println("code:" + code+", err: " + err);
		});
		
		return p;
	}
	
	public IPromise<Boolean>  unsubscribeJMAsync(String topic, PSDataListener lis) {
		Map<String,Object> params = new HashMap<>();;
		params.put("subId", lis.getSubId());
		params.put("op", 2);
		
		final IPromise<Boolean> p = this.apiGc.sendMessage(Constants.MSG_TYPE_PUBSUB, params, Boolean.class);
		
		p.success((rst,cxt0)->{
			Set<PSDataListener> ls = listeners.get(topic);
			if(ls != null && !ls.isEmpty()) {
				ls.remove(lis);
			}
			if(ls.isEmpty()) {
				listeners.remove(topic);
			}
		})
		.fail((code,err,cxt0)->{
			System.out.println("code:" + code+", err: " + err);
		});
		return p;
	}

	public void onMsg(PSDataJRso item) {
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
