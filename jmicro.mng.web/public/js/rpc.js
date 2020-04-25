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

window.jm = window.jm || {};

jm.config ={
    //ip:"192.168.3.3",
    //ip:'127.0.0.1',
    ip:'192.168.56.1',
    //ip:'192.168.1.145',
    port:'9090',
    wsContext:'_ws_',
    httpContext:'/_http_',
    useWs:true
}

jm.Constants = {
  MessageCls : 'org.jmicro.api.net.Message',
  IRequestCls : 'org.jmicro.api.server.IRequest',
  ServiceStatisCls : 'org.jmicro.api.monitor.ServiceStatis',
  ISessionCls : 'org.jmicro.api.net.ISession',
  Integer : 3,
  LOng : 4,
  String : 5,
  DEFAULT_NAMESPACE : 'defaultNamespace',
  DEFAULT_VERSION : "0.0.0",
}

jm.Constants.Message = {
    HEADER_LEN : 10,
    PROTOCOL_BIN : 0,
    PROTOCOL_JSON  :  1,
    PRIORITY_0  :  0,
    PRIORITY_1  :  1,
    PRIORITY_2  :  2,
    PRIORITY_3  :  3,
    PRIORITY_4  :  4,
    PRIORITY_5  :  5,
    PRIORITY_6  :  6,
    PRIORITY_7  :  7,
    PRIORITY_MIN  :  this.PRIORITY_0,
    PRIORITY_NORMAL  :  this.PRIORITY_3,
    PRIORITY_MAX  :  this.PRIORITY_7,
    MSG_VERSION  :  1,
    FLAG_PROTOCOL  :  1<<0,
    //调试模式
    FLAG_DEBUG_MODE  :  1<<1,
    //需要响应的请求
    FLAG_NEED_RESPONSE  :  1<<2,

    //0B00111000 5---3
    FLAG_LEVEL  :  0X38,

    //异步消息
   FLAG_STREAM  :  1<<6,

    //DUMP上行数据
    FLAG0_DUMP_UP  :  1<<0,
    //DUMP下行数据
    FLAG0_DUMP_DOWN  :  1<<1,

    //可监控消息
    FLAG0_MONITORABLE  :  1<<2,

    //是否启用服务级log
    FLAG0_LOGGABLE  : 1 << 3
}

jm.transport = {
    send:function(data,cb){
        if(jm.config.useWs){
            jm.socket.send(data,cb);
        } else {
            //jm.http.postHelper(jm.http.getHttpApiPath(),data,cb);
            $.ajax({
                url: jm.http.getHttpApiPath(),
                type: "post",
                dataType: "json",
                data: JSON.stringify(data),
                headers: {'Content-Type': 'application/json','DataEncoderType':1},
                success: function (result, statuCode, xhr) {
                    //sucCb(data, statuCode, xhr);
                	result.payload=JSON.parse(result.payload);
                	cb(result,null);
                },
                beforeSend: function (xhr) {
                },
                error: function (err, xhr) {
                    if (errCb) {
                        errCb(err, xhr);
                    } else {
                        sucCb(null, err, xhr);
                    }
                }
            })
        }
    }
}

jm.rpc = {
    idCache:{},
    init:function(){
        if(jm.config.useWs && !window.WebSocket){
            jm.config.useWs = false;
        }
    },

    createMsg:function(type) {
        var msg = new jm.rpc.Message();
        msg.setType(type);
        msg.setProtocol(jm.Constants.Message.PROTOCOL_JSON);
        msg.setId(new Date().getTime());
        msg.setReqId(msg.getId());
        msg.setLinkId(new Date().getTime());

        //msg.setStream(false);
        msg.setDumpDownStream(false);
        msg.setDumpUpStream(false);
        msg.setNeedResponse(true);
        msg.setLoggable(false);
        msg.setMonitorable(false);
        msg.setDebugMode(false);
        msg.setLogLevel(4)//LOG_WARN

        return msg;
    }

    ,getId : function(idClazz){
        var self = this;
        return new Promise(function(reso1,reje){
          var cacheId = self.idCache[idClazz];
          if(!!cacheId && cacheId.curIndex < cacheId.ids.length){
            reso1(cacheId.ids[cacheId.curIndex++]);
          } else {
            if(!cacheId){
              cacheId = {ids:[],curIndex:0};
              self.idCache[idClazz] = cacheId;
            }

            var msg =  self.createMsg(0x0B)

            var req = new jm.rpc.IdRequest();
            req.type = jm.Constants.LOng;
            req.clazz = jm.Constants.MessageCls;
            req.num = 1;
            msg.payload = JSON.stringify(req);
            jm.transport.send(msg,function(rstMsg,err){
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

      let msg =  self.createMsg(0x09)
      let ai = jm.mng.act.actInfo;
      if(ai && ai.success ) {
          req.params['loginKey'] = ai.loginKey;
      }

      let streamCb = req.stream;
      if(typeof req.stream == 'function') {
        req.stream = true;
        msg.setStream(true)
      }
      msg.payload =  JSON.stringify(req);

      msg.reqId = req.reqId;

      if(req.needResponse) {
          msg.setNeedResponse(true);
      }
        jm.transport.send(msg,function(rstMsg,err){
            if(err || !rstMsg.payload.success) {
                reje(err || rstMsg.payload.msg);
            }else {
                reso(rstMsg.payload.result);
            }
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
        params.namespace = jm.Constants.DEFAULT_NAMESPACE;
      }

      if(!params.version) {
        params.version = jm.Constants.DEFAULT_VERSION;
      }

      if(!params.args ) {
        params.args = [];
      }

      if(!Array.isArray(params.args)){
        reje( 'args must be array');
        return;
      }

      if(typeof params.needResponse == 'undefined') {
        params.needResponse = true;
      }

      var req = new jm.rpc.ApiRequest();
      req.serviceName = params.serviceName;
      req.method = params.method;
      req.namespace = params.namespace;
      req.version = params.version;
      req.args = params.args;

      req.needResponse = params.needResponse;
      req.stream = params.stream;

        self.callRpcWithRequest(req)
            .then(function(rst){
                reso(rst);
            }).catch(function(err){
            reje(err);
        });

    });

  },

  callWithParams:function(serviceName,namespace,version,method,args,needResponse){
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
        namespace = jm.Constants.DEFAULT_NAMESPACE;
      }

      if(!version || version.trim() == '') {
        version = jm.Constants.DEFAULT_VERSION;
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

      let req = new jm.rpc.ApiRequest();
      req.serviceName = serviceName;
      req.method = method;
      req.namespace = namespace;
      req.version = version;
      req.args = args;
      req.needResponse = needResponse;

        self.callRpcWithRequest(req)
            .then(function(rst){
                reso(rst);
            }).catch(function(err){
            reje(err);
        });

    });

  },

  callRpc : function(param){
    var self = this;
    if(param instanceof jm.rpc.ApiRequest) {
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

jm.rpc.Message = function() {

    this.startTime = -1;

    //1 byte length
    this.version = jm.Constants.Message.MSG_VERSION;

    this.reqId = -1;

    //payload length with byte,4 byte length
    //private int len;

    // 1 byte
    this.type = 0;

    /**
     * dm: is development mode
     * S: Stream
     * N: need Response
     * P: protocol 0:bin, 1:json
     * LLL: Message priority
     *
     *   S L L  L  N dm P
     * | | | |  |  |  | |
     * 7 6 5 4  3  2  1 0
     * @return
     */
    this.flag = 0;

    /**
     * up: dump up stream data
     * do: dump down stream data
     * M: Monitorable
     * L: 开发日志上发

     *
     *         L M  do up
     * | | | | | |  |  |
     * 7 6 5 4 3 2  1  0
     * @return
     */
    this.flag0 = 0;

    //request or response
    //private boolean isReq;

    //2 byte length
    //private byte ext;

    this.payload=null;

    //*****************development mode field begin******************//
    this.msgId = -1;
    this.linkId = -1;
    this.time = -1;
    this.instanceName = '';
    this.method = '';

    //****************development mode field end*******************//
}

jm.rpc.Message.prototype = {
    is : function(flag, mask){
        return (flag & mask) != 0;
    },

    /* isByShort:function(flag, mask) {
        return (flag & mask) != 0;
    },*/

    isDumpUpStream : function() {
        return this.is(this.flag0,jm.Constants.Message.FLAG0_DUMP_UP);
    },

    setDumpUpStream : function( f) {
        this.flag0 |= f ? jm.Constants.Message.FLAG0_DUMP_UP : 0 ;
    },

    isDumpDownStream : function() {
        return this.is(this.flag0,jm.Constants.Message.FLAG0_DUMP_DOWN);
    },

    setDumpDownStream : function( f) {
        this.flag0 |= f ? jm.Constants.Message.FLAG0_DUMP_DOWN : 0 ;
    },

    isLoggable:function() {
        return this.is(this.flag0,jm.Constants.Message.FLAG0_LOGGABLE);
    },

    setLoggable : function( f) {
        this.flag0 |= f ? jm.Constants.Message.FLAG0_LOGGABLE : 0 ;
    },

    isDebugMode : function() {
        return this.is(this.flag,jm.Constants.Message.FLAG_DEBUG_MODE);
    },

    setDebugMode : function( f) {
        this.flag |= f ? jm.Constants.Message.FLAG_DEBUG_MODE : 0 ;
    },

    isMonitorable : function() {
        return this.is(this.flag0,jm.Constants.Message.FLAG0_MONITORABLE);
    },

    setMonitorable( f) {
        this.flag0 |= f ? jm.Constants.Message.FLAG0_MONITORABLE : 0 ;
    },

    isStream : function() {
        return this.is(this.flag,jm.Constants.Message.FLAG_STREAM);
    },

    setStream:function( f) {
        this.flag |= f ? jm.Constants.Message.FLAG_STREAM : 0 ;
    },

    isNeedResponse:function() {
        return this.is(this.flag,jm.Constants.Message.FLAG_NEED_RESPONSE);
    },

    setNeedResponse : function( f) {
        this.flag |= f ? jm.Constants.Message.FLAG_NEED_RESPONSE : 0 ;
    },

    getLevel : function() {
        return ((this.flag >>> 3) & 0x07);
    },

    setLevel : function( l) {
        if(l > jm.Constants.Message.PRIORITY_7 || l < jm.Constants.Message.PRIORITY_0) {
            throw "Invalid priority: "+l;
        }
        this.flag = ((l << 3) | this.flag);
    },

    getLogLevel : function() {
        return ((this.flag0 >>> 3) & 0x07);
    },

    setLogLevel : function( l) {
        if(l > 6 || l < 0) {
            throw "Invalid log level: "+l;
        }
        this.flag0 = ((l << 3) | this.flag0);
    },

    getProtocolByFlag(flag) {
        return (flag & 0x01);
    },

    getProtocol : function() {
        return this.getProtocolByFlag(this.flag);
    },

    setProtocol:function( protocol) {
        if(protocol == jm.Constants.Message.PROTOCOL_BIN || protocol == jm.Constants.Message.PROTOCOL_JSON) {
            this.flag = ( protocol | (this.flag & 0xFE));
        }else {
            throw ("Invalid protocol: "+protocol);
        }
    },

    getId() {
        return this.msgId;
    },

    setId:function(id) {
        this.msgId = id;
    },

    getVersion : function() {
        return this.version;
    },

    setVersion : function(version) {
        this.version = version;
    },

    getType : function() {
        return this.type;
    },

    setType : function( type) {
        this.type = type;
    },

     getPayload() {
        return this.payload;
    },
    setPayload:  function( payload) {
        this.payload = payload;
    },

    getReqId : function() {
        return this.reqId;
    },

    setReqId : function( reqId) {
        this.reqId = reqId;
    },

    getLinkId : function() {
        return this.linkId;
    },

    setLinkId : function( linkId) {
        this.linkId = linkId;
    },

    getFlag : function() {
        return this.flag;
    },

    getFlag0 : function() {
        return this.flag0;
    },

    getTime : function() {
        return this.time;
    },

    setTime : function( time) {
        this.time = time;
    },

    getInstanceName : function() {
        return this.instanceName;
    },

    setInstanceName : function( instanceName) {
        this.instanceName = instanceName;
    },

     getMethod : function() {
        return this.method;
    },

    setMethod( method) {
        this.method = method;
    },

    getStartTime : function() {
        return this.startTime;
    },

    setStartTime : function( startTime) {
        this.startTime = startTime;
    }

}

jm.rpc.IdRequest = function() {
    this.type  =  jm.Constants.LOng;
    this.num  =  1;
    this.clazz  =  '';
}

jm.rpc.IdRequest.prototype = {

}

jm.rpc.reqId = 1;

jm.rpc.ApiRequest = function() {
  this.params = {};
  this.serviceName = '';
  this.method = '';
  this.args = [];
  this.namespace = '';
  this.version = '';
  this.reqId = jm.rpc.reqId++;
  this.msg = '';
  this.needResponse = true;
  this.stream = false;
}

jm.rpc.ApiRequest.prototype = {

}

jm.rpc.ApiResponse = function() {
  this.id = -1;
  this.msg = null;
  this.reqId =  -1;
  this.result = null;
  this.success = true;
}

jm.rpc.ApiResponse.prototype = {

}

jm.rpc.init();
