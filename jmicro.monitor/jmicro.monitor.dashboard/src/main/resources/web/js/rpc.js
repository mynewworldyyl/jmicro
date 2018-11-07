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
    wsContext:'/_ws_',
    httpContext:'/_http_',
    useWs:false
}

jmicro.Constants = {
  MessageCls : 'org.jmicro.api.net.Message',
  IRequestCls : 'org.jmicro.api.server.IRequest',
  ServiceStatisCls : 'org.jmicro.api.monitor.ServiceStatis',
  ISessionCls : 'org.jmicro.api.net.ISession',
  PROTOCOL_BIN : 1,
  PROTOCOL_JSON : 2,
  Integer : 3,
  LOng : 4,
  String : 5,
  DEFAULT_NAMESPACE : 'defaultNamespace',
  DEFAULT_VERSION : "0.0.0",
  STREAM : 1 << 2,
  NEED_RESPONSE : 1 << 1
}

jmicro.transport = {
    send:function(data,cb){
        if(jmicro.config.useWs){
            jmicro.socket.send(data,cb);
        } else {
            jmicro.http.postHelper(jmicro.http.getHttpApiPath(),data,cb);
        }
    }
}

jmicro.rpc = {
    idCache:{},
    init:function(){
        if(jmicro.config.useWs && !!window.WebSocket){
            jmicro.config.useWs = false;
        }
    }
    ,getId : function(idClazz){
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

            msg.flag |= jmicro.Constants.NEED_RESPONSE;

              jmicro.transport.send(msg,function(rstMsg,err){
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

  callRpcWithRequest : function(req){
    var self = this;
    return new Promise(function(reso,reje){
      var msg = new jmicro.rpc.Message();

      var streamCb = req.stream;
      if(typeof req.stream == 'function') {
        req.stream = true;
      }
      msg.payload =  JSON.stringify(req);

      msg.type = 0x7FF8;
      msg.protocol = jmicro.Constants.PROTOCOL_JSON;
      msg.reqId = req.reqId;

      if(req.needResponse)
        msg.flag |= jmicro.Constants.NEED_RESPONSE;

      if(req.stream)
        msg.flag |= jmicro.Constants.STREAM;

      self.getId(jmicro.Constants.MessageCls)
        .then(function(id){
          msg.msgId = id;
          self.getId(jmicro.Constants.IRequestCls)
            .then(function(id){
              msg.reqId = id;
                  jmicro.transport.send(msg,function(rstMsg,err){
                if(req.stream) {
                  streamCb(rstMsg.payload.result,err);
                } else {
                  if(err){
                    reje(err);
                  } else {
                    reso(rstMsg.payload.result);
                  }
                }
              });
            });
        });
    });
  },

  callWithObject:function(params){
    var self = this;
    return new Promise(function(reso,reje){

      if(!params.serviceName) {
        reje('service name cannot be NULL');
        return;
      }

      if(!params.method) {
        reje( 'method name cannot be NULL');
        return;
      }

      if(!params.namespace) {
        params.namespace = jmicro.Constants.DEFAULT_NAMESPACE;
      }

      if(!params.version) {
        params.version = jmicro.Constants.DEFAULT_VERSION;
      }

      if(!params.args ) {
        params.args = [];
      }

      if(!Array.isArray(params.args)){
        reje( 'args must be array');
        return;
      }

      if(!params.stream ) {
        params.stream = false;
      }

      if(typeof params.needResponse == 'undefined') {
        params.needResponse = true;
      }

      var req = new jmicro.rpc.ApiRequest();
      req.serviceName = params.serviceName;
      req.method = params.method;
      req.namespace = params.namespace;
      req.version = params.version;
      req.args = params.args;

      req.needResponse = params.needResponse;
      req.stream = params.stream;

      self.getId(jmicro.Constants.IRequestCls)
        .then(function(id){
          req.reqId = id;
          self.callRpcWithRequest(req)
            .then(function(rst){
              reso(rst);
            }).catch(function(err){
              reje(err);
          });
        });
    });

  },

  callWithParams:function(serviceName,namespace,version,method,args,stream,needResponse){
    var self = this;
    return new Promise(function(reso,reje){

      if(!serviceName || serviceName.trim() == '') {
        reje('service name cannot be NULL');
        return;
      }

      if(!method || method.trim() == '') {
        reje( 'method name cannot be NULL');
        return;
      }

      if(!namespace  || namespace.trim() == '') {
        namespace = jmicro.Constants.DEFAULT_NAMESPACE;
      }

      if(!version || version.trim() == '') {
        version = jmicro.Constants.DEFAULT_VERSION;
      }

      if(!stream ) {
        stream = false;
      }

      if(typeof needResponse == 'undefined') {
        needResponse = true;
      }

      if(!args ) {
        args = [];
      }

      if(!Array.isArray(args)){
        reje( 'args must be array');
        return;
      }

      var req = new jmicro.rpc.ApiRequest();
      req.serviceName = serviceName;
      req.method = method;
      req.namespace = namespace;
      req.version = version;
      req.args = args;
      req.needResponse = needResponse;
      req.stream = stream;

      self.getId(jmicro.Constants.IRequestCls)
        .then(function(id){
          req.reqId = id;
          self.callRpcWithRequest(req)
            .then(function(rst){
              reso(rst);
            }).catch(function(err){
            reje(err);
          });
        });
    });

  },

  callRpc : function(param){
    var self = this;
    if(param instanceof jmicro.rpc.ApiRequest) {
        return self.callRpcWithRequest(param);
    }else if(typeof param  == 'object'){
        return self.callWithObject(param);
    } else if(arguments.length >= 5) {
        if(arguments.length == 5) {
          return self.callWithParams(arguments[0],arguments[1],arguments[2],arguments[3],arguments[4]);
        }else if(arguments.length >= 6){
          return self.callWithParams(arguments[0],arguments[1],arguments[2],arguments[3],arguments[4],arguments[5]);
        }else if(arguments.length >= 7){
          return self.callWithParams(arguments[0],arguments[1],arguments[2],arguments[3],arguments[4],arguments[5],arguments[6]);
        }
    } else {
          return new Promise(function(reso,reje){
          reje('Invalid params');
       });
    }

  }

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
  this.needResponse = true;
  this.stream = false;
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

jmicro.rpc.init();
