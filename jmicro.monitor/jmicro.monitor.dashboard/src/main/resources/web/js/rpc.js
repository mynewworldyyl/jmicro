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
var jmicro = jmicro || {};

jmicro.config ={
  ip:"192.168.3.3",
  port:'9992',
  context:'ws',
}

jmicro.Constants = {
  MessageCls:'org.jmicro.api.net.Message',
  IRequestCls:'org.jmicro.api.server.IRequest',
  ServiceStatisCls:'org.jmicro.api.monitor.ServiceStatis',
  ISessionCls:'org.jmicro.api.net.ISession',
  PROTOCOL_BIN:1,
  PROTOCOL_JSON:2,
  Integer:3,
  LOng:4,
  String:5,
}

jmicro.rpc = {
  idCache:{},
  getId : function(idClazz){
    var self = this;
    return new Promise(function(reso1,reje){
      var cacheId = self.idCache[idClazz];
      if(!!cacheId && cacheId.index < cacheId.ids.length){
        reso1(cacheId.ids[cacheId.index++]);
      } else {
        if(!cacheId){
          cacheId = {ids:[],index:0};
          self.idCache[idClazz] = cacheId;
        }

        var msg = new jmicro.rpc.Message();
        msg.type=0x7FFA;

        var req = new jmicro.rpc.IdRequest();
        req.type = jmicro.Constants.LOng;
        req.clazz = jmicro.Constants.MessageCls;
        req.num = 1;
        msg.payload = JSON.stringify(req);

        jmicro.socket.send(msg,function(rstMsg,err){
          if(err){
            reje(err);
            return;
          }
          if(!!rstMsg.payload){
            cacheId.ids =  rstMsg.payload;
            cacheId.index = 0;
            var i = cacheId.ids[cacheId.index++]
            reso1(i);
          } else {
            reje(rstMsg);
          }
        });
      }
    });
  },

  callRpc : function(req){
    var self = this;
    return new Promise(function(reso,reje){
      var msg = new jmicro.rpc.Message();
      msg.payload =  JSON.stringify(req);
      msg.type = 0x7FF8;
      msg.protocol = jmicro.Constants.PROTOCOL_JSON;
      msg.reqId = req.reqid;

      self.getId(jmicro.Constants.MessageCls)
        .then(function(id){
          msg.msgId = id;
          self.getId(jmicro.Constants.IRequestCls)
            .then(function(id){
              msg.reqId = id;
              jmicro.socket.send(msg,function(rstMsg,err){
                if(err){
                  reje(err);
                } else {
                  reso(rstMsg);
                }
              });
            });
        });
    });
  },

}

jmicro.rpc.Message = function() {
  this.protocol = jmicro.Constants.PROTOCOL_JSON;
  this.msgId =  -1,
  this.reqId  =  -1,
  this.sessionId  =  -1,
  this.len  =  -1,
  this.version  =  '0.0.0',
  this.type  =  -1,
  this.flag  =  0,
  this.payload  =  ''
}

jmicro.rpc.Message.prototype = {

}

jmicro.rpc.IdRequest = function() {
    this.type  =  jmicro.Constants.LOng;
    this.num  =  1;
    this.clazz  =  '';
}
jmicro.rpc.IdRequest.prototype = {

}

jmicro.rpc.ApiRequest = function() {
  this.params = {};
  this.serviceName = '';
  this.method = '';
  this.args = [];
  this.namespace = '';
  this.version = '';
  this.reqId = -1;
  this.msg = '';
}

jmicro.rpc.ApiRequest.prototype = {

}

jmicro.rpc.ApiResponse = function() {
  this.id = -1;
  this.msg = null;
  this.reqId =  -1;
  this.result = null;
  this.success = true;
}
jmicro.rpc.ApiResponse.prototype = {

}
