package cn.jmicro.gateway.pubsub;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.jmicro.api.async.IPromise;
import cn.jmicro.api.pubsub.PSData;
import cn.jmicro.api.pubsub.genclient.IPubSubClientService$JMAsyncClient;
import cn.jmicro.gateway.client.ApiGatewayClient;

public class ApiGatewayPubsubClient {

	private Map<String,Set<PSDataListener>> listeners = new HashMap<>();
	
	private ApiGatewayClient apiGc;
	
	public static String messageServiceImplName = "cn.jmicro.gateway.MessageServiceImpl";
	
	private IPubSubClientService$JMAsyncClient pcs;
	
	public ApiGatewayPubsubClient(ApiGatewayClient client) {
		this.apiGc = client;
		pcs = client.getService(IPubSubClientService$JMAsyncClient.class,ApiGatewayClient.NS_MNG, "0.0.1");
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
		final IPromise<Integer> p = this.apiGc.callService(messageServiceImplName, ApiGatewayClient.NS_MNG, "0.0.1", "subscribe", 
				Integer.class, new Object[] {topic, ctx});
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
		IPromise<Boolean> p = this.apiGc.callService(messageServiceImplName, ApiGatewayClient.NS_MNG, "0.0.1", "unsubscribe", 
				Integer.class, new Object[] {lis.getSubId()});
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
