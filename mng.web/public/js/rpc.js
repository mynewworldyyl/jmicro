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
    //ip:'192.168.101.22',
    //ip:'172.18.0.1',
    port:'9090',
    txtContext : '_txt_',
    binContext : '_bin_',
    httpContext : '/_http_',
    useWs : true
}

jm.Constants = {
  MessageCls : 'cn.jmicro.api.net.Message',
  IRequestCls : 'cn.jmicro.api.server.IRequest',
  ServiceStatisCls : 'cn.jmicro.api.monitor.ServiceStatis',
  ISessionCls : 'cn.jmicro.api.net.ISession',
  Integer : 3,
  LOng : 4,
  String : 5,
  DEFAULT_NAMESPACE : 'defaultNamespace',
  DEFAULT_VERSION : "0.0.0",
}

jm.transport = {
    send : function(data,cb){
        if(jm.config.useWs){
            jm.socket.send(data,cb);
        } else {
            //jm.http.postHelper(jm.http.getHttpApiPath(),data,cb);
            $.ajax({
                url: jm.http.getHttpApiPath(),
                type: "post",
                dataType: "json",
                data: jm.socket.encode(data,false),
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
    },

}

jm.rpc = {
    idCache:{},
    init:function(){
        if(jm.config.useWs && !window.WebSocket){
            jm.config.useWs = false;
        }
    },

    createMsg:function(type) {
        let msg = new jm.rpc.Message();
        msg.type = type;
        msg.id= 0;
        msg.reqId = 0;
        msg.linkId = 0;

        //msg.setStream(false);
        msg.setDumpDownStream(false);
        msg.setDumpUpStream(false);
        msg.setNeedResponse(true);
        msg.setLoggable(false);
        msg.setMonitorable(false);
        msg.setDebugMode(false);
        msg.setLogLevel(jm.rpc.Constants.LOG_NO)//LOG_WARN

        return msg;
    }

    ,getId : function(idClazz){
        let self = this;
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

  callRpcWithRequest : function(req,type,upProtocol,downProtocol){
    if(typeof type == 'undefined') {
        type = jm.rpc.Constants.MSG_TYPE_REQ_RAW;
    }

      if(typeof upProtocol == 'undefined') {
          upProtocol = jm.rpc.Constants.PROTOCOL_BIN;
      }

      if(typeof downProtocol == 'undefined') {
          downProtocol = jm.rpc.Constants.PROTOCOL_BIN;
      }

    return this.callRpcWithTypeAndProtocol(req,type,upProtocol,downProtocol);
  },

    callRpcWithTypeAndProtocol : function(req,type,upProtocol,downProtocol){
        let self = this;
        return new Promise(function(reso,reje){

            let msg =  self.createMsg(type);
            msg.setUpProtocol(upProtocol);
            msg.setDownProtocol(downProtocol);

            if(req.reqId) {
                msg.reqId = req.reqId;
                msg.id = msg.reqId;
            }else {
                msg.reqId = jm.rpc.reqId++;
                msg.id = msg.reqId;
            }

            let ai = jm.mng.act.actInfo;
            if(ai && ai.success ) {
                req.params['loginKey'] = ai.loginKey;
            }

            if(upProtocol == jm.rpc.Constants.PROTOCOL_JSON) {
                msg.payload =  jm.utils.toUTF8Array(JSON.stringify(req));
            } else if(upProtocol == jm.rpc.Constants.PROTOCOL_BIN ){
                if(typeof req.encode == 'function') {
                    msg.payload = req.encode(jm.rpc.Constants.PROTOCOL_BIN);
                }
            } else {
                msg.payload = req;
            }

            if(req.needResponse) {
                msg.setNeedResponse(true);
            }
            jm.transport.send(msg,function(rstMsg,err){
                if(err || !rstMsg.payload.success) {
                    reje(err || rstMsg.payload.msg);
                } else {
                    reso(rstMsg.payload.result);
                }
            });
        });
    },

  callWithObject:function(params,type,upProtocol,downProtocol){
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
      //req.stream = params.stream;

      self.callRpcWithRequest(req,type,upProtocol,downProtocol)
        .then(function(rst){
            reso(rst);
        }).catch(function(err){
        reje(err);
      });

    });

  },

  callWithParams : function(serviceName, namespace, version, method, args, needResponse){
    let self = this;
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

  callRpc : function(param,type,upProtocol,downProtocol){
        if(!upProtocol) {
            upProtocol = jm.rpc.Constants.PROTOCOL_BIN;
        }

        if(!downProtocol) {
            downProtocol = jm.rpc.Constants.PROTOCOL_BIN;
        }

      if(!type) {
          type = jm.rpc.Constants.MSG_TYPE_REQ_RAW;
      }

      let self = this;
    if(param instanceof jm.rpc.ApiRequest) {
        return self.callRpcWithRequest(param,type,upProtocol,downProtocol);
    }else if(typeof param  == 'object'){
        return self.callWithObject(param,type,upProtocol,downProtocol);
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

jm.rpc.Constants = {

    MSG_TYPE_API_REQ : 0x09, //API网关请求
    MSG_TYPE_API_RESP : 0x0A,//API网关请求响应

    MSG_TYPE_REQ_RAW : 0x03,  //纯二进制数据请求
    MSG_TYPE_RRESP_RAW : 0x04, //纯二进制数据响应

    LOG_NO : 0,
    LOG_FINAL : 6,

    CHARSET : 'UTF-8',

    HEADER_LEN : 14,
    PROTOCOL_BIN : 0,
    PROTOCOL_JSON : 1,

    PRIORITY_0 : 0,
    PRIORITY_1 : 1,
    PRIORITY_2 : 2,
    PRIORITY_3 : 3,
    PRIORITY_4 : 4,
    PRIORITY_5 : 5,
    PRIORITY_6 : 6,
    PRIORITY_7 : 7,

    PRIORITY_MIN : 0,
    PRIORITY_NORMAL : 3,
    PRIORITY_MAX : 7,

    MAX_SHORT_VALUE:0X7FFF,

    MAX_BYTE_VALUE :0X7F,

    MAX_INT_VALUE :0x7FFFFFFF,

    //public static final long MAX_LONG_VALUE = Long.MAX_VALUE*2;

    MSG_VERSION :1,

    FLAG_UP_PROTOCOL : 1 << 0,

    FLAG0_DOWN_PROTOCOL : 1 << 6,

    //调试模式
    FLAG_DEBUG_MODE : 1 << 1,

    //需要响应的请求
    FLAG_NEED_RESPONSE : 1 << 2,

    //0B00111000 5---3
    FLAG_LEVEL : 0X38,

    //长度字段类型，1表示整数，0表示短整数
    FLAG_LENGTH_INT : 1 << 6,

    //DUMP上行数据
    FLAG0_DUMP_UP : 1 << 0,
    //DUMP下行数据
    FLAG0_DUMP_DOWN : 1 << 1,

    //可监控消息
    FLAG0_MONITORABLE : 1 << 2,

    //是否启用服务级log
    FLAG0_LOGGABLE : 1 << 3,

}

jm.rpc.Message = function() {

    this.startTime = 0;
    //此消息所占字节数
    this.len = -1;

    //1 byte length
    this.version = 0;

    this.reqId = 0;

    //payload length with byte,4 byte length
    //private int len;

    // 1 byte
    this.type = 0;

    /**
     * dm: is development mode
     * S: data length type 0:short 1:int
     * N: need Response
     * PR: protocol 0:bin, 1:json
     * PPP: Message priority
     *
     *   S P P  P  N dm PR
     * | | | |  |  |  | |
     * 7 6 5 4  3  2  1 0
     * @return
     */
    this.flag = 0;

    /**
     * up: dump up stream data
     * do: dump down stream data
     * M: Monitorable
     * L: 日志级别

     *
     *     L L L M  do up
     * | | | | | |  |  |
     * 7 6 5 4 3 2  1  0
     * @return
     */
    this.flag0 = 0;

    //request or response
    //private boolean isReq;

    //2 byte length
    //private byte ext;

    this.payload = null;


    //*****************development mode field begin******************//
    this.msgId = 0;
    this.linkId = 0;
    this.time = 0;
    this.instanceName = 0;
    this.method = 0;

    //****************development mode field end*******************//
}

//public static boolean
jm.rpc.Message.prototype.is = function( flag,  mask) {
    return (flag & mask) != 0;
}

jm.rpc.Message.prototype.set = function( isTrue, f, mask) {
    return isTrue ?(f |= mask):(f &= ~mask);
}

//public static boolean
/* jm.rpc.Message.prototype.is( flag, mask) {
    return (flag & mask) != 0;
},*/

jm.rpc.Message.prototype.isDumpUpStream = function()  {
    return this.is(this.flag0,jm.rpc.Constants.FLAG0_DUMP_UP);
}

//public boolean
jm.rpc.Message.prototype.isDumpDownStream = function() {
    return this.is(this.flag0, jm.rpc.Constants.FLAG0_DUMP_DOWN);
}

//public void
jm.rpc.Message.prototype.setDumpUpStream = function(f)  {
    //this.flag0 |= f ? jm.rpc.Constants.FLAG0_DUMP_UP : 0 ;
    this.flag0  = this.set(f,this.flag0 ,jm.rpc.Constants.FLAG0_DUMP_UP);
}

//public boolean
jm.rpc.Message.prototype.setDumpDownStream = function(f)  {
    //return this.is(flag0,jm.rpc.Constants.FLAG0_DUMP_DOWN);
    this.flag0  = this.set(f,this.flag0 ,jm.rpc.Constants.FLAG0_DUMP_DOWN);
}

//public boolean
jm.rpc.Message.prototype.isLoggable = function()  {
    return this.is(this.flag0,jm.rpc.Constants.FLAG0_LOGGABLE);
}

//public void
jm.rpc.Message.prototype.setLoggable = function(f)  {
    //this.flag0 |= f ? jm.rpc.Constants.FLAG0_LOGGABLE : 0 ;
    this.flag0  = this.set(f,this.flag0 ,jm.rpc.Constants.FLAG0_LOGGABLE);
}

//public boolean
jm.rpc.Message.prototype.isDebugMode = function() {
    return this.is(this.flag,jm.rpc.Constants.FLAG_DEBUG_MODE);
}

//public void
jm.rpc.Message.prototype.setDebugMode = function(f)  {
    //this.flag |= f ? jm.rpc.Constants.FLAG_DEBUG_MODE : 0 ;
    this.flag  = this.set(f,this.flag ,jm.rpc.Constants.FLAG_DEBUG_MODE);
}

//public boolean
jm.rpc.Message.prototype.isMonitorable = function()  {
    return this.is(this.flag0,jm.rpc.Constants.FLAG0_MONITORABLE);
}

//public void
jm.rpc.Message.prototype.setMonitorable = function(f)  {
   // this.flag0 |= f ? jm.rpc.Constants.FLAG0_MONITORABLE : 0 ;
    this.flag0  = this.set(f,this.flag0 ,jm.rpc.Constants.FLAG0_MONITORABLE);
}

//public boolean
jm.rpc.Message.prototype.isNeedResponse = function()  {
    return this.is(this.flag,jm.rpc.Constants.FLAG_NEED_RESPONSE);
}

//public void
jm.rpc.Message.prototype.setNeedResponse = function(f)  {
    //this.flag |= f ? jm.rpc.Constants.FLAG_NEED_RESPONSE : 0 ;
    this.flag  = this.set(f,this.flag ,jm.rpc.Constants.FLAG_NEED_RESPONSE);
}

/**
 *
 * @param f true 表示整数，false表示短整数
 */
//public void
jm.rpc.Message.prototype.setLengthType = function(f)  {
    //this.flag |= f ? jm.rpc.Constants.FLAG_LENGTH_INT : 0 ;
    this.flag  = this.set(f,this.flag ,jm.rpc.Constants.FLAG_LENGTH_INT);
}

//public boolean
jm.rpc.Message.prototype.isLengthInt = function()  {
    return this.is(this.flag,jm.rpc.Constants.FLAG_LENGTH_INT);
}

//public int
jm.rpc.Message.prototype.getPriority = function()  {
    return ((this.flag >>> 3) & 0x07);
}

//public void
jm.rpc.Message.prototype.setPriority = function(l)  {
    if(l > jm.rpc.Constants.PRIORITY_7 || l < jm.rpc.Constants.PRIORITY_0) {
        throw "Invalid priority: "+l;
    }
    this.flag = ((l << 3) | this.flag);
}

//public byte
jm.rpc.Message.prototype.getLogLevel = function()  {
    return ((this.flag0 >>> 3) & 0x07);
}

//public void
jm.rpc.Message.prototype.setLogLevel = function(v)  {
    if(v < jm.rpc.Constants.LOG_NO || v > jm.rpc.Constants.LOG_FINAL) {
        throw "Invalid Log level: "+v;
    }
    this.flag0 = ((v << 3) | this.flag0);
}

jm.rpc.Message.prototype.getUpProtocol=function() {
    return this.is(this.flag,jm.rpc.Constants.FLAG_UP_PROTOCOL) ? 1:0;
}

jm.rpc.Message.prototype.setUpProtocol=function(protocol) {
    this.flag  = this.set(protocol == jm.rpc.Constants.PROTOCOL_JSON ,this.flag , jm.rpc.Constants.FLAG_UP_PROTOCOL);
}

jm.rpc.Message.prototype.getDownProtocol = function() {
    return this.is(this.flag0, jm.rpc.Constants.FLAG0_DOWN_PROTOCOL)?1:0;
}

jm.rpc.Message.prototype.setDownProtocol = function(protocol) {
    //this.flag |= protocol == jm.rpc.Constants.PROTOCOL_JSON ? jm.rpc.Constants.FLAG_DOWN_PROTOCOL : 0 ;
    this.flag0  = this.set(protocol == jm.rpc.Constants.PROTOCOL_JSON ,this.flag0 ,jm.rpc.Constants.FLAG0_DOWN_PROTOCOL);
}

//public static Message
jm.rpc.Message.prototype.decode = function(b) {
    let msg = this;
    let dataInput = new jm.utils.JDataInput(b);
    //第0个字节
    msg.flag = dataInput.getUByte();
    let len = 0;
    if(this.isLengthInt()) {
        len = dataInput.readInt();
    } else {
        len = dataInput.readUnsignedShort(); // len = 数据长度 + 测试模式时附加数据长度
    }

    if(dataInput.remaining() < len){
        throw "Message len not valid";
    }

    //第3个字节
    msg.version = dataInput.getUByte();

    //read type
    //第4个字节
    msg.type = dataInput.getUByte();

    //第5，6，7，8个字节
    msg.reqId = dataInput.readInt();

    //第9，10，11，12个字节
    msg.linkId = dataInput.readInt();

    //第13个字节
    msg.flag0 = dataInput.getUByte();

    if(msg.isDebugMode()) {
        //读取测试数据头部
        msg.id = dataInput.readUnsignedLong();
        msg.time = dataInput.readUnsignedLong()
        len -= 16;

        msg.instanceName = dataInput.readUtf8String();
        len -= jm.utils.utf8StringTakeLen(msg.instanceName);

        msg.method = dataInput.readUtf8String();
        len -= jm.utils.utf8StringTakeLen(msg.method);

        //减去测试数据头部长度
        //len -= JDataOutput.encodeStringLen(msg.getInstanceName());
        //len -= JDataOutput.encodeStringLen(msg.getMethod());
    }

    if(len > 0){
        let pa = [];
        for(let i = 0; i < len ; i++) {
            pa.push(dataInput.getUByte());
        }
        if(this.getDownProtocol() == jm.rpc.Constants.PROTOCOL_JSON) {
            msg.payload = JSON.parse(jm.utils.fromUTF8Array(pa));
        } else {
            msg.payload = pa;
        }
    }else {
        msg.payload = null;
    }

    msg.len = len + jm.rpc.Constants.HEADER_LEN;

    return msg;
}

//public ByteBuffer
jm.rpc.Message.prototype.encode = function() {
    let buf =  new jm.utils.JDataOutput(1024);
    let len = 0;//数据长度 + 测试模式时附加数据长度

    let data = this.payload;

    if(!(data instanceof ArrayBuffer || data instanceof Array)) {
        let json = JSON.stringify(data);
        data = jm.utils.toUTF8Array(json);
        this.setUpProtocol(jm.rpc.Constants.PROTOCOL_JSON)
    }

    if(data instanceof ArrayBuffer) {
        len = data.byteLength;
    } else {
        len = data.length;
    }

    let inArr = null;
    let meArr = null;
    if(this.isDebugMode()) {
        inArr = jm.utils.toUTF8Array(this.instanceName);
        meArr = jm.utils.toUTF8Array(this.method);
        len = len + inArr.length + meArr.length;
        //2个long的长度，2*8=8
        len += 16;
    }

    //len += Message.HEADER_LEN

    //第1，2个字节 ,len = 数据长度 + 测试模式时附加数据长度
    let maxLen = len + jm.rpc.Constants.HEADER_LEN;
    if(maxLen < jm.rpc.Constants.MAX_SHORT_VALUE) {
        this.setLengthType(false);
    } else if(maxLen < jm.rpc.Constants.MAX_INT_VALUE){
        this.setLengthType(true);
    } else {
        throw "Data length too long than :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+len;
    }

    //let b = new DataView(buf);
    //第0个字节，标志头
    //b.put(this.flag);
    buf.writeUByte(this.flag);

    if(maxLen < jm.rpc.Constants.MAX_SHORT_VALUE) {
        //第1，2个字节 ,len = 数据长度 + 测试模式时附加数据长度
        buf.writeUnsignedShort(len)
    }else if(len < jm.rpc.Constants.MAX_INT_VALUE){
        buf.writeInt(len)
    } else {
        throw "Data too long  :" + jm.rpc.Constants.MAX_INT_VALUE+", but value "+len;
    }

    //第3个字节
    //b.put(this.version);
    buf.writeUByte(this.version);

    //第4个字节
    //writeUnsignedShort(b, this.type);
    //b.put(this.type);
    buf.writeUByte(this.type);

    //第5，6，7，8个字节
    //writeUnsignedInt(b, this.reqId);
    buf.writeInt(this.reqId);

    //第9，10，11，12个字节
    //writeUnsignedInt(b, this.linkId);
    buf.writeInt(this.linkId);

    //第13个字节
    //b.put(this.flag0);
    buf.writeUByte(this.flag0);

    if(this.isDebugMode()) {
        //b.putLong(this.getId());
        //b.putLong(this.getTime());
        //大端写长整数
        buf.writeUnsignedLong(this.id)
        buf.writeUnsignedLong(this.time)

        buf.writeUtf8String(this.instanceName);
        buf.writeUtf8String(this.method);

        //OnePrefixTypeEncoder.encodeString(b, this.instanceName);
        //OnePrefixTypeEncoder.encodeString(b, this.method);

        /*buf.writeUtf8String(this.instanceName,function(len){
            console.log(len);
        });

        buf.writeUtf8String(this.method,function(len){
            console.log(len);
        });*/
    }

    if(data != null){
        if(data instanceof ArrayBuffer) {
            let size = data.byteLength;
            let dv = new DataView(data);
            buf.checkCapacity(size);
            for(let i = 0; i < size; i++) {
                buf.writeUByte(dv.getUint8(i))
            }
        } else {
            let size = data.length;
            buf.checkCapacity(size);
            for(let i = 0; i < size; i++) {
                buf.writeUByte(data[i])
            }
        }
    }

    return buf.getBuf();
},


jm.rpc.Message.prototype.toString = function() {
    return "Message [version=" + this.version + ", msgId=" + this.msgId + ", reqId=" + this.reqId + ", linkId=" + this.linkId
        + ", type=" + this.type + ", flag=" + Number.toHexString(this.flag) + ", flag0=" + Number.toHexString(this.flag0)
        + ", payload=" + this.payload + ", time="+ this.time
        + ", devMode=" + this.isDebugMode() + ", monitorable="+ this.isMonitorable()
        + ", needresp="+ this.isNeedResponse()
        + ", upstream=" + this.isDumpUpStream() + ", downstream="+ this.isDumpDownStream()
        + ", instanceName=" + this.instanceName + ", method=" + this.method + "]";
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
  this.reqId = jm.rpc.reqId++;
  this.serviceName = '';
  this.namespace = '';
  this.version = '';
  this.method = '';
  this.params = {};

  this.args = [];

}

jm.rpc.ApiRequest.prototype = {
    encode : function(protocol) {
        if(protocol == jm.rpc.Constants.PROTOCOL_BIN) {
            let buf =  new jm.utils.JDataOutput(1024);
            buf.writeUnsignedLong(this.reqId);
            buf.writeUtf8String(this.serviceName);
            buf.writeUtf8String(this.namespace);
            buf.writeUtf8String(this.version);
            buf.writeUtf8String(this.method);
            buf.writeObject(this.params);
            buf.writeObjectArray(this.args);
            return buf.getBuf();
        } else if(protocol == jm.rpc.Constants.PROTOCOL_JSON)  {
            return JSON.stringify(this);
        }else {
            throw 'Invalid protocol:'+protocol;
        }
    }

}

jm.rpc.ApiResponse = function() {
  this.id = -1;
  this.msg = null;
  this.reqId =  -1;
  this.result = null;
  this.success = true;
}

jm.rpc.ApiResponse.prototype = {
    decode : function(arrayBuf, protocol) {

        if(protocol == jm.rpc.Constants.PROTOCOL_BIN) {
            let dataInput = new jm.utils.JDataInput(arrayBuf);
            this.id = dataInput.readUnsignedLong();
            this.reqId = dataInput.readUnsignedLong();
            this.success = dataInput.getUByte() > 0 ;
            this.result = [];
            let len = dataInput.remaining();
            for(let i = 0; i < len; i++) {
                this.result.push(dataInput.getUByte());
            }
        } else if(protocol == jm.rpc.Constants.PROTOCOL_JSON)  {

            if(arrayBuf instanceof Array ||arrayBuf　instanceof ArrayBuffer) {
                let dataInput = new jm.utils.JDataInput(arrayBuf);
                let byteArray = [];
                let len = dataInput.remaining();
                for(let i = 0; i < len; i++) {
                    byteArray.push(dataInput.getUByte());
                }

                let jsonStr = jm.utils.fromUTF8Array(byteArray);
                let o = JSON.parse(jsonStr);
                if(o) {
                    this.id = o.id;
                    this.reqId = o.reqId;
                    this.success = o.success;
                    this.result = o.result;
                }
            }
        }else {
            throw 'Invalid protocol:'+protocol;
        }
    }
}

jm.rpc.init();

