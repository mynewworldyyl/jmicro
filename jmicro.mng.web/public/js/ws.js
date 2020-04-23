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
var jm = jm || {};

/**
 * 0 ：对应常量CONNECTING (numeric value 0)，
 正在建立连接连接，还没有完成。The connection has not yet been established.
 1 ：对应常量OPEN (numeric value 1)，
 连接成功建立，可以进行通信。The WebSocket connection is established and communication is possible.
 2 ：对应常量CLOSING (numeric value 2)
 连接正在进行关闭握手，即将关闭。The connection is going through the closing handshake.
 3 : 对应常量CLOSED (numeric value 3)
 连接已经关闭或者根本没有建立。The connection has been closed or could not be opened.

 * @type {{listeners: {}, logData: null, idCallback: {}, isInit: boolean, init: jm.socket.init, send: jm.socket.send, registListener: jm.socket.registListener}}
 */
jm.socket = {
    listeners : {},
    waiting:[]
    ,logData:null
    ,idCallback:{}
    , isInit:false
    ,init : function(onopen) {
            this.isInit = true;
            var url = 'ws://' + jm.config.ip + ':' + jm.config.port +'/'+ jm.config.wsContext;
            var self = this;
            if(window.WebSocket){
              self.wsk = new WebSocket(url);  //获得WebSocket对象

             //当有消息过来的时候触发
             self.wsk.onmessage = function(event){
               //console.log(event.data);
               var msg = JSON.parse(event.data);
               msg.payload = JSON.parse(msg.payload);

               if(msg.type == jm.mng.ps.MSG_TYPE_ASYNC_RESP) {
                   jm.mng.ps.onMsg(msg.payload);
               } else {
                   if(self.listeners[msg.reqId]) {
                       self.listeners[msg.reqId](msg);
                       delete self.listeners[msg.reqId];
                   }
               }
             }

            //连接关闭的时候触发
            self.wsk.onclose = function(event){
               console.log("connection close");
                this.isInit = false;
            }

            //连接打开的时候触发
            self.wsk.onopen = function(event){
              console.log("connect successfully");
              if(onopen) {
                onopen();
              }
            }
        }else{
          alert("浏览器不支持WebSocket");
        }
    }

    ,send : function(msg,cb) {
        if(msg.isNeedResponse()) {
            //注册回调为消息监听
            this.listeners[msg.reqId] = cb;
        }
        let self = this;
        if(!!self.wsk && self.wsk.readyState == WebSocket.OPEN) {
            this.wsk.send(JSON.stringify(msg));
        } else if(!self.wsk || self.wsk.readyState == WebSocket.CLOSED ||
            self.wsk.readyState == WebSocket.CLOSING) {
            this.init(function () {
                self.wsk.send(JSON.stringify(msg));
                if(self.waiting.length > 0) {
                    for(let i = 0; i < self.waiting.length; i++) {
                        self.waiting[i]();
                    }
                    self.waiting = [];
                }
            });
        } else if(self.wsk.readyState == WebSocket.CONNECTING) {
            self.waiting.push(function(){
                self.wsk.send(JSON.stringify(msg));
            })
        }
    }

   ,registListener : function(type,lis) {
      if(!this.listeners[type]) {
          this.listeners[type] = lis;
      } else {
          throw 'type:'+type + ' have been exists';
      }
   }

}

