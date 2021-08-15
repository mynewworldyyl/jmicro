import {Constants,Message} from "@/rpc/message"
import ApiResponse from "@/rpc/response"
import ps from "@/rpc/pubsub"
import  config from "@/rpc/config"

let listeners = {};
let waiting = [];
//let logData = null;
//let idCallback = {};
//let isInit = false;

let wsk = null;

export default {
    reconnect() {
        if(wsk) {
            wsk.close();
        }
        //isInit = false
    }

    ,init(onopen) {
        //isInit = true;
        let url = config.wsProtocol + '://' + config.ip + ':' + config.port +'/'+ config.binContext;
        if(window.WebSocket){
            wsk = new WebSocket(url);  //获得WebSocket对象

            //当有消息过来的时候触发
            wsk.onmessage = function(event){
                //console.log(event.data);
                //var msg = JSON.parse(event.data);
                //msg.payload = JSON.parse(msg.payload);
                event.data.arrayBuffer().then(function(buf){
                    let msg = new Message();
                    msg.decode(buf);

                    if(msg.type == Constants.MSG_TYPE_ASYNC_RESP) {
                        /* if(!msg.success) {
                             throw 'fail:' + msg.payload;
                         }*/
                        /*let dataInput = new jm.utils.JDataInput( msg.payload);
                        let byteArray = [];
                        let len = dataInput.remaining();
                        for(let i = 0; i < len; i++) {
                            byteArray.push(dataInput.getUByte());
                        }
                        let jsonStr = jm.utils.fromUTF8Array(byteArray);
                        msg.payload = JSON.parse(jsonStr);*/
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

            //连接关闭的时候触发
            wsk.onclose = function(event){
                console.log("connection close");
                console.log(event);
                //isInit = false;
            }

            //连接打开的时候触发
            wsk.onopen = function(event){
                console.log("connect successfully");
                console.log(event);
                if(onopen) {
                    onopen();
                }
            }
        }else{
            alert("浏览器不支持WebSocket");
        }
    }

    ,send(msg,cb) {
        if(msg.isNeedResponse()) {
            //注册回调为消息监听
           listeners[msg.msgId] = cb;
        }
        //msg.setProtocol(Constants.PROTOCOL_BIN);
        let buffe = msg.encode();
        if(!!wsk && wsk.readyState == WebSocket.OPEN) {
            wsk.send(buffe);
        } else if(!wsk || wsk.readyState == WebSocket.CLOSED ||
            wsk.readyState == WebSocket.CLOSING) {
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
        } else if(wsk.readyState == WebSocket.CONNECTING) {
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