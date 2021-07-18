
import PSData from "@/rpc/psdata";
import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants"

let pssn = "cn.jmicro.gateway.MessageServiceImpl";
let    sn = 'cn.jmicro.api.pubsub.IPubSubClientServiceJMSrv'
let    ns = cons.NS_MNG
let   pns="pubSubServer"
let   v='0.0.1'
//let   MSG_TYPE_ASYNC_RESP =0x06

let  psListeners = {};

function _item(topic, data,persist,queue,callback,itemContext) {
    if(!rpc.isLogin()) {
        throw 'Not login';
    }
    if(!topic ||topic.length == 0) {
        throw 'Topic cannot be null';
    }
    if(!data) {
        throw 'Message body cannot be null';
    }

    let psData = new PSData(topic,data);

    if(persist) {
        psData.setPersist(persist);
    }

    if(queue) {
        psData.queue(queue);
    }

    if(callback && callback.length > 0) {
        psData.callback = callback;
        psData.callbackTopic()
    }

    psData.context = itemContext;
    psData.srcClientId = rpc.actInfo.clientId;

    return psData;
}

function _publishItem(topic, data,persist,queue,callback,itemContext) {
    let psData =_item(topic, data,persist,queue,callback,itemContext);
    return rpc.callRpcWithParams(sn,pns,v,'publishOneItem',[psData]);
}

export default {

    onMsg(msg) {
        let cbs = psListeners[msg.topic];
        for(let i = 0; i < cbs.length; i++){
            if(cbs[i]) {
                cbs[i](msg);
            }
        }
    },

    subscribe (topic,ctx,callback){

        if(psListeners[topic] && psListeners[topic].length > 0) {
            let cs = psListeners[topic];
            callback.id = 0; //已经由别的接口订阅此主题，现在只需要注入回调即可
            let flag = false;
            //排除同一个回调方法重复订阅同一主题的情况
            for(let i = 0; i < cs.length; i++) {
                if(cs[i] == callback) {
                    flag = true;
                    break;
                }
            }
            if(!flag) {
                cs.push(callback);
            }

            return new Promise(function(reso){
                reso(0);
            });
        }

        if(!psListeners[topic]) {
            psListeners[topic] = [];
        }
        psListeners[topic].push(callback);

        let self = this;
        return new Promise((reso,reje)=>{
            rpc.callRpcWithParams(pssn,ns,v,'subscribe',[topic, ctx || {}])
                .then((id)=>{
                    if( id > 0 && !!callback ) {
                        callback.id = id;
                    }else if(!(id.errorCode == 4 ||id.errorCode == 5 ||id.errorCode == 6)) {
                        self.unsubscribe(topic,callback);
                    }
                    reso(id);
                }).catch(err =>{
                self.unsubscribe(topic,callback);
                reje(err);
            });
        });
    },

    unsubscribe(topic,callback){
        let cs = this.psListeners[topic];
        if(cs && cs.length > 0) {
            let idx = -1;
            for(let i =0; i < cs.length; i++) {
                if(cs[i] == callback) {
                    idx = i;
                    break;
                }
            }
            if(idx >= 0) {
                cs.splice(idx,1);
            }
        }

        if(cs && cs.length > 0) {
            return new Promise(function(reso){
                reso(0);
            });
        }
        let self = this;
        return new Promise(function(reso,reje){
            rpc.callRpcWithParams(self.pssn, self.ns, self.v, 'unsubscribe',[callback.id])
                .then((rst)=>{
                    if(!rst) {
                        //console.log("Fail to unsubscribe topic:"+topic);
                        reje("Fail to unsubscribe topic:"+topic)
                    }else {
                        reso(rst);
                    }
                }).catch(err =>{
                reje(err);
            });
        });
    },

    //byteArray： 发送byte数组
    //persist: 指示消息服务器是否持久化消息，如果为true，则持久化到数据库存储24小时，在24小时内可以通过消息历史记录页面查询到已经发送的消息。
    //queue: 目前未使用
    //callback: 接收消息发送结果主题，需要单独订阅此主题接收结果通知
    //itemContext：每个消息都有一个上下文，有于存储消息相关的附加信息
    publishBytes(topic, byteArray,persist,queue,callback,itemContext){
        return _publishItem(topic, byteArray,persist,queue,callback,itemContext);
    },
    //发送字符串消息
    publishString(topic,content,persist,queue,callback,itemContext){
        return _publishItem(topic, content,persist,queue,callback,itemContext);
    },

    //通过消息服务器调用别外一个RPC方法，args为RPC方法的参数
    callService(topic,args,persist,queue,callback,itemContext){
        return _publishItem(topic,args,persist,queue,callback,itemContext);
    },

    //同时发送多个消息，psItems为消息数组
    publishMultiItems(psItems){
        return rpc.callRpcWithParams(sn,pns,v,'publishMutilItems',[psItems]);
    },

    //发送单个消息
    publishOneItem(psItem){
        return rpc.callRpcWithParams(sn,pns,v,'publishOneItem',[psItem]);
    },

}