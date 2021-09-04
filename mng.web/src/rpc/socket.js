/* eslint-disable */
import {Constants,Message} from "./message"
import ApiResponse from "./response"
import ps from "./pubsub"
import  config from "./config"
import  rpc from "./rpcbase"

let listeners = {};
let waiting = [];
//let logData = null;
//let idCallback = {};
//let isInit = false;

let wxs = null
if(typeof wx == 'object') wxs = wx

let wsk = null;

export default {
    reconnect() {
        if(wsk) {
            wsk.close();
        }
        //isInit = false
    },

    __onClose (event){
        console.log("connection close");
        console.log(event);
        //isInit = false;
    },

    __onMessage(event){
        event.data.arrayBuffer().then(function(buf){
            let msg = new Message();
            msg.decode(buf);
            if(msg.type == Constants.MSG_TYPE_ASYNC_RESP) {
                ps.onMsg(msg.payload);
            } else  {
                if(msg.getDownProtocol() == Constants.PROTOCOL_BIN) {
                    let resp = new ApiResponse();
                    resp.decode(msg.payload, msg.getDownProtocol());
                    msg.payload = resp;
                }
                if(listeners[msg.msgId]) {
                    listeners[msg.msgId](msg);
                    delete listeners[msg.msgId];
                }
            }
        });
    }

    ,init(onopen) {
        //isInit = true;
        let url = config.wsProtocol + '://' +  config.ip + ':' + config.port +'/'+ config.binContext;

        if(window && window.WebSocket) {
            wsk = new WebSocket(url);  //获得WebSocket对象
            //当有消息过来的时候触发
            wsk.onmessage = this.__onMessage
            //连接关闭的时候触发
            wsk.onclose = this.__onClose
            //连接打开的时候触发
            wsk.onopen = function(event){
                console.log("connect successfully");
                console.log(event);
                if(onopen) {
                    onopen();
                }
            }
        }else if(wxs) {
            wsk = wxs.connectSocket({
                url: url,
            });

            //连接打开的时候触发
            wsk.onOpen((event)=>{
                console.log("connect successfully");
                console.log(event);
                if(onopen) {
                    onopen();
                }
            })

            //当有消息过来的时候触发
            wsk.onMessage((res)=>{
                this.__onMessage(res);
            })

            //连接关闭的时候触发
            wsk.onClose((res)=>{
                this.__onClose(res);
            })

            wsk.onError(res => {
                console.info('连接识别');
                console.error(res);

            })

        }else {
            throw "浏览器不支持WebSocket";
        }
    }

    ,send(msg,cb) {
        if(msg.isNeedResponse()) {
            //注册回调为消息监听
           listeners[msg.msgId] = cb;
        }
        //msg.setProtocol(Constants.PROTOCOL_BIN);
        let buffe = msg.encode();
        if(!!wsk && wsk.readyState == wsk.OPEN) {
            wsk.send(buffe);
        } else if(!wsk || wsk.readyState == wsk.CLOSED ||
            wsk.readyState == wsk.CLOSING) {
            this.init(function () {
                wsk.send(buffe);
                //self.wsk.send(JSON.stringify(msg));
                //self.wsk.se
                if(waiting.length > 0) {
                    for(let i = 0; i < waiting.length; i++) {
                        waiting[i]();
                    }
                    waiting = [];
                }
            });
        } else if(wsk.readyState == wsk.CONNECTING) {
            waiting.push(function(){
                wsk.send(buffe);
            })
        }
    }

    ,registListener(type,lis) {
        if(!listeners[type]) {
            listeners[type] = lis;
        } else {
            throw 'type:'+type + ' have been exists';
        }
    }

}