import PSData from './psdata';
import JDataInput from './datainput';
import rpc from './rpcbase';
import cons from './constants';
import auth from '../rpc/auth';

import { Constants } from './message';

const pssn = 'cn.jmicro.gateway.MessageServiceImpl';
const sn = 'cn.jmicro.api.pubsub.IPubSubClientServiceJMSrv';

//基于账号的消息前缀
const TOPIC_PREFIX = "/__act/msg/";
const MSG_TYPE = "__msgType";

const ns = cons.NS_MNG;
const pns = 'pubSubServer';
const v = '0.0.1'; //let   MSG_TYPE_ASYNC_RESP =0x06

const psListeners = {};

const tml = {};

let actTopic = null

const MSG_TYPE_CHAT_CODE = 5556;//聊天消息
const MSG_TYPE_SENDER_CODE = 5555;//配送员端订单消息
const MSG_TYPE_CUST_CODE = 5557;//配送服务客户端消息
	
const MSG_OP_CODE_SUBSCRIBE = 1;//订阅消息
const MSG_OP_CODE_UNSUBSCRIBE = 2;//取消订阅消息
const MSG_OP_CODE_FORWARD = 3;//转发消息
const MSG_OP_CODE_FORWARD_BY_TOPIC = 4;//转发消息

const MAX_INTERVAL = 2*60*1000 //2分钟没收到消息，就注册消息监听
let lastMT = new Date().getTime()//最后一次收到下发消息时间

function _item(topic, data,to, itemContext, persist, queue, callback) {
    if (!auth.isLogin()) {
        throw 'Not login';
    }

    if (!topic || topic.length == 0) {
        throw 'Topic cannot be null';
    }

    if (!data) {
        throw 'Message body cannot be null';
    }

    let psData = new PSData(topic, data);

    if(persist) {
        psData.setPersist(persist);
    }

    if(queue) {
        psData.queue(queue);
    }

    if(callback && callback.length > 0) {
        psData.callback = callback;
        psData.callbackTopic();
    }

    psData.cxt = itemContext;
    psData.srcClientId = auth.actInfo.clientId;
	psData.to = to
    return psData;
}

/**
 * 发消息
 * @param {Object} topic
 * @param {Object} data
 * @param {Object} to
 * @param {Object} persist
 * @param {Object} queue
 * @param {Object} callback
 * @param {Object} itemContext
 */
function _publishItem(topic, data, to, itemContext, persist, queue, callback) {
    let psData = _item(topic, data, to, itemContext, persist, queue, callback);
	//1028786913  publishOneItem
    return rpc.invokeByCode(1028786913, [psData]);
}

export default {
	MSG_TYPE_CHAT_CODE,
	MSG_TYPE_SENDER_CODE,
	MSG_TYPE_CUST_CODE,
	
	init() {
		auth.addActListener((type, act)=>{
			if(type == Constants.LOGIN) {
				actTopic = TOPIC_PREFIX + auth.getActId()
				this.subscribe(actTopic,{},this.onTypeMsg)
			}else if(type == Constants.LOGOUT) {
				this.unsubscribe(actTopic,this.onTypeMsg)
			}
		})
	},
	
	check() {
		if(new Date().getTime() - this.lastMT < MAX_INTERVAL ) {
			//console.log("pubsub no check")
			return
		}
		
		this.lastMT = new Date().getTime()
		
		if(actTopic) {
			//console.log("do act topic sub")
			this.doSub(actTopic)
		}
		
	},
	
	onTypeMsg(msg) {
		if(msg.cxt && tml[msg.cxt[MSG_TYPE]]) {
			tml[msg.cxt[MSG_TYPE]](msg)
		} else {
			console.log("No listener for msg type: " + msg[MSG_TYPE])
		}
	},
	
	subTypeMsg(type,cb) {
		if(!tml[type]) {
			tml[type]=cb
		} else {
			console.log("Exist msg type: " + type)
		}
	},
	
	unsubTypeMsg(type) {
		if(tml[type]) {
			delete tml[type]
		}
	},
	
    onMsg(msg) {
		this.lastMT = new Date().getTime()//更新收到消息时间
		
		let psitem = msg.payload
		if(psitem instanceof Array) {
			let psData = new PSData()
			psData.decode(new JDataInput(msg.payload))
			psitem = psData
		}
		
		let msgId = msg.getExtra(Constants.EXTRA_KEY_SMSG_ID)
		if(msgId) {
			psitem.msgId = msgId
		} else {
			psitem.msgId = msg.msgId
		}
		
        let cbs = psListeners[psitem.topic];
		if(cbs) {
			for (let i = 0; i < cbs.length; i++) {
			    if (cbs[i]) {
			        cbs[i](psitem);
			    }
			}
		} else {
			console.log("Listener not found: "+psitem.topic);
		}
    },
	
   async subscribe(topic, ctx, callback) {
        if(psListeners[topic] && psListeners[topic].length > 0) {
            let cs = psListeners[topic];
            callback.id = 0; //已经由别的接口订阅此主题，现在只需要注入回调即可

            let flag = false; //排除同一个回调方法重复订阅同一主题的情况

            for (let i = 0; i < cs.length; i++) {
                if (cs[i] == callback) {
                    flag = true;//消息监听器已经存在
                    break;
                }
            }

            if (!flag) {
                cs.push(callback);
            }

            return 0
        }

        if (!psListeners[topic]) {
            psListeners[topic] = [];
        }

        psListeners[topic].push(callback);
        let self = this;
		
		let subId = await this.doSub(topic);
		
		if(subId && subId > 0) {
			 callback.id = subId;
		} else {
			self.unsubscribe(topic, callback);
		}
			
    },
	
	async doSub(topic) {
		if(!auth.isLogin()) {
			console.log("Not login")
			return
		}
		let ps = [{k:Constants.EXTRA_KEY_PS_OP_CODE, v:MSG_OP_CODE_SUBSCRIBE, t:Constants.PREFIX_TYPE_BYTE},
			{k:Constants.EXTRA_KEY_PS_ARGS, v:topic, t:Constants.PREFIX_TYPE_STRINGG}]
		let subId = await rpc.sendMessage("", Constants.MSG_TYPE_PUBSUB, ps);
		return subId;
	},
	
    unsubscribe(topic, callback) {
		if(!callback.id) {
			return
		}
		
        let cs = psListeners[topic];

        if (cs && cs.length > 0) {
            let idx = -1;

            for (let i = 0; i < cs.length; i++) {
                if (cs[i] == callback) {
                    idx = i;
                    break;
                }
            }

            if (idx >= 0) {
                cs.splice(idx, 1);
            }
        }

        if (cs && cs.length > 0) {
            return 0
        }
		
		let ps = [{k:Constants.EXTRA_KEY_PS_OP_CODE, v:MSG_OP_CODE_UNSUBSCRIBE, t:Constants.PREFIX_TYPE_BYTE},
			{k:Constants.EXTRA_KEY_PS_ARGS, v:callback.id, t:Constants.PREFIX_TYPE_INT}]
		
		return rpc.sendMessage("", Constants.MSG_TYPE_PUBSUB,ps);
		
		//let ps = {subId : callback.id, op : 2};
		//return rpc.sendMessage(ps,Constants.MSG_TYPE_PUBSUB);
    },
	
	itemString(topic, stringData, to,type) {
		let it = _item(topic, stringData,to, {}, false, true, null)
		it.type = type
		return it;
	},
	
	//客户端对客户端消息，服务器不解析消息内容
	sendDirectMessage(content, targetId,upp,downp) {
		let ps = [{k:Constants.EXTRA_KEY_PS_OP_CODE, v:MSG_OP_CODE_FORWARD, t:Constants.PREFIX_TYPE_BYTE},
			{k:Constants.EXTRA_KEY_PS_ARGS, v:targetId, t:Constants.PREFIX_TYPE_INT}]
		return rpc.sendMessage(content, Constants.MSG_TYPE_PUBSUB,ps,upp,downp);
	},
	
	sendDirectMessageByTopic(content, topic,upp,downp) {
		let ps = [{k:Constants.EXTRA_KEY_PS_OP_CODE, v:MSG_OP_CODE_FORWARD_BY_TOPIC, t:Constants.PREFIX_TYPE_BYTE},
			{k:Constants.EXTRA_KEY_PS_ARGS, v:topic, t:Constants.PREFIX_TYPE_STRINGG}]
		return rpc.sendMessage(content, Constants.MSG_TYPE_PUBSUB,ps,upp,downp);
	},
	
    //byteArray： 发送byte数组
    //persist: 指示消息服务器是否持久化消息，如果为true，则持久化到数据库存储24小时，在24小时内可以通过消息历史记录页面查询到已经发送的消息。
    //queue: 目前未使用
    //callback: 接收消息发送结果主题，需要单独订阅此主题接收结果通知
    //itemContext：每个消息都有一个上下文，有于存储消息相关的附加信息
    publishBytes(topic, byteArray,to,itemContext, persist, queue, callback) {
        return _publishItem(topic, byteArray,to,itemContext, persist, queue, callback);
    },
	
    //发送字符串消息
    publishString(topic, content,to, itemContext, persist, queue, callback) {
		if(typeof content != 'string') {
			content = JSON.stringify(content)
		}
        return _publishItem(topic, content,to,itemContext, persist, queue, callback);
    },
	
    //通过消息服务器调用别外一个RPC方法，args为RPC方法的参数
    callService(topic, args,to,itemContext, persist, queue, callback) {
        return _publishItem(topic, args,to,itemContext, persist, queue, callback);
    },
	
    //同时发送多个消息，psItems为消息数组publishMutilItems
    publishMultiItems(psItems) {
        return rpc.invokeByCode(1288935099, [psItems]);
    },
	
    //发送单个消息publishOneItem
    publishOneItem(psItem) {
        return rpc.invokeByCode(1028786913, [psItem]);
    },
	
	//psItem: _publishItem,
	psItem:_item,
};
