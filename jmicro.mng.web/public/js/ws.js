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

jm.socket = {
    listeners : {}
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

               if(self.listeners[msg.reqId]) {
                 self.listeners[msg.reqId](msg);
               }

               if(!(msg.flag & jm.Constants.STREAM)) {
                 delete self.listeners[msg.reqId];
               }
             }

            //连接关闭的时候触发
            self.wsk.onclose = function(event){
               console.log("connection close");
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
        if(!this.isInit) {
            let self = this;
            this.init(function () {
                self.wsk.send(JSON.stringify(msg));
            });
        } else {
            this.wsk.send(JSON.stringify(msg));
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

