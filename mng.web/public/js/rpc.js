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

jm.config = {
    //ip:"192.168.3.3",
    // ip:'192.168.1.129',
    ip:'192.168.56.1',
    //ip:'47.112.161.111',
    //ip:'jmicro.cn',
    //ip:'192.168.101.22',
    //ip:'172.18.0.1',
    port:'9090',
    //port:'80',
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
  MNG:'mng',
}

jm.goTo = function(url) {
    location.href = url;
}

String.prototype.startWith=function(str){
    var reg=new RegExp("^"+str);
    return reg.test(this);
}

String.prototype.endWith=function(str){
    var reg=new RegExp(str+"$");
    return reg.test(this);
}

Date.prototype.toDecDay = function() {
    let curDate = new Date();
    let decVal = curDate.getTime() - this.getTime();
    if(decVal <= 0) {
        return '0';
    }

    let minuteMS = 60*1000;
    let hourMS = 60*minuteMS;
    let dateMS = 24*hourMS;

    let str = '';

    let v = parseInt(decVal/dateMS);
    if(v > 0) {
        str += v + 'D,';
    }

    decVal = decVal % dateMS;
    v = parseInt(decVal/hourMS);
    if(v > 0) {
        str += v + 'H,';
    }

    decVal = decVal % hourMS;
    v = parseInt(decVal/minuteMS);
    if(v > 0) {
        str += v + 'M,';
    }

    decVal = decVal % minuteMS;
    v = parseInt(decVal/1000);
    if(v > 0) {
        str += v + 'S';
    }

    return str;

}

Date.prototype.format = function(fmt) {
    var o = {
        "M+" : this.getMonth()+1,                 //月份
        "d+" : this.getDate(),                    //日
        "h+" : this.getHours(),                   //小时
        "m+" : this.getMinutes(),                 //分
        "s+" : this.getSeconds(),                 //秒
        "q+" : Math.floor((this.getMonth()+3)/3), //季度
        "S"  : this.getMilliseconds()             //毫秒
    };
    if(/(y+)/.test(fmt)) {
        fmt=fmt.replace(RegExp.$1, (this.getFullYear()+"").substr(4 - RegExp.$1.length));
    }
    for(var k in o) {
        if(new RegExp("("+ k +")").test(fmt)){
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));
        }
    }
    return fmt;
}

//for safari
if(typeof Array.prototype.fill == 'undefined') {
    Array.prototype.fill = function(e) {
        for(var i = 0; i < this.length; i++) {
            this[i] = e;
        }
    }
}

if(typeof String.prototype.trim == 'undefined') {
    String.prototype.trim = function(e) {
        return str.replace(/^\s+|\s+$/g,"");
    }
}

if(typeof String.prototype.trim == 'undefined') {
    String.prototype.trim = function(e) {
        return this.replace(/^\s+|\s+$/g,"");
    }
}

if(typeof String.prototype.leftTrim == 'undefined') {
    String.prototype.leftTrim = function() {
        return this.replace( /^\s*/, '');
    }
}

if(typeof String.prototype.rightTrim == 'undefined') {
    String.prototype.rightTrim = function() {
        return this.replace(/(\s*$)/g, "");
    }
}

jm.utils = {
    _genId : 0,
    getId: function(){
        return this._genId ++;
    },

    getTimeAsMills: function() {
        return new Date().getTime();
    },

    strByteLength:  function(str)  {
        var i;
        var len;
        len = 0;
        for (i=0;i<str.length;i++)  {
            if (str.charCodeAt(i)>255) len+=2; else len++;
        }
        return len;
    },

    isInteger: function (value)  {
        if ('/^(\+|-)?\d+$/'.test(value )){
            return true;
        }else {
            return false;
        }
    },

    isFloat: function(value){
        if ('/^(\+|-)?\d+($|\.\d+$)/'.test(value )){
            return true;
        }else{
            return false;
        }
    } ,

    checkUrl: function (value){
        var myReg = '/^((http:[/][/])?\w+([.]\w+|[/]\w*)*)?$/';
        return myReg.test( value );
    },

    checkEmail: function (value){
        var myReg = /^([-_A-Za-z0-9\.]+)@([_A-Za-z0-9]+\.)+[A-Za-z0-9]{2,3}$/;
        return myReg.test(value);
    },

    checkMobile: function (phone){
        return /^1(3|4|5|6|7|8|9)\d{9}$/.test(phone);
    },

    checkIP:   function (value)   {
        var re='/^(\d+)\.(\d+)\.(\d+)\.(\d+)$/';
        if(re.test( value ))  {
            if( RegExp.$1 <256 && RegExp.$2<256 && RegExp.$3<256 && RegExp.$4<256)
                return true;
        }
        return false;
    },

    inherits : function(child, parentCtor) {
        function tempCtor() {};
        tempCtor.prototype = parentCtor.prototype;
        child.superClass_ = parentCtor.prototype;
        child.prototype = new tempCtor();
        child.prototype.constructor = child;
    },

    bind : function(scope, funct){
        return function(){
            return funct.apply(scope, arguments);
        };
    },

    appendUrlParams:function(url,params) {
        if(params) {
            url = url + '?';
            for(var p in params) {
                url = url + p + '=' + params[p]+'&';
            }
            url = url.substr(0, url.length - 1);
        }
        return url;
    },

    parseUrlParams:function(url1) {
        var url = url1;
        if(!url) {
            url = window.location.href;
        }
        var index = url.indexOf('?');

        if(index < 0) {
            return {};
        }

        url = decodeURI(url);
        var qs = url.substring(index+1);

        var params = {};
        var arr = qs.split('&');
        for(var i =0; i < arr.length; i++) {
            var kv = arr[i].split('=');
            params[kv[0]]=kv[1];
        }

        return params;
    },

    isBrowser : function(browser) {
        return jm.utils.browser[browser] != null;
    },

    version : function() {
        for(var b in jm.utils.browser) {
            return jm.utils.browser[b][1].split('.')[0];
        }
    },

    isIe : function() {
        return this.isBrowser(jm.utils.Constants.IE9) ||
            this.isBrowser(jm.utils.Constants.IE8) ||
            this.isBrowser(jm.utils.Constants.IE7) ||
            this.isBrowser(jm.utils.Constants.IE6)
    },

    clone : function(jsObj) {
        var type = typeof jsObj;
        if(type != 'object') {
            return jsObj;
        }
        var obj = {};
        for(var i in jsObj) {
            var o = jsObj[i];
            obj[i] = jm.utils.clone(o);
        }
        return obj;
    },

    isLoad: function(jsUrl) {
        var scripts = document.getElementsByTagName('script');
        if(!scripts || scripts.length < 1) {
            return false;
        }
        for(var i = 0; i < scripts.length; i++) {
            if(jsUrl == scripts[i].src ) {
                return true;
            }
        }
        return false;
    },

    load: function(jsUrl) {
        if(this.isLoad(jsUrl)) {
            return ;
        }
        var oHead = document.getElementsByTagName('HEAD').item(0);
        var oScript= document.createElement("script");
        oScript.type = "text/javascript";
        oScript.src=jsUrl;
        oHead.appendChild( oScript);
    },

    toUTF8Array : function(str) {
        let utf8 = [];
        for (let i=0; i < str.length; i++) {
            let charcode = str.charCodeAt(i);
            if (charcode < 0x80) utf8.push(charcode);
            else if (charcode < 0x800) {
                utf8.push(0xc0 | (charcode >> 6),
                    0x80 | (charcode & 0x3f));
            }
            else if (charcode < 0xd800 || charcode >= 0xe000) {
                utf8.push(0xe0 | (charcode >> 12),
                    0x80 | ((charcode>>6) & 0x3f),
                    0x80 | (charcode & 0x3f));
            }
            // surrogate pair
            else {
                i++;
                charcode = ((charcode&0x3ff)<<10)|(str.charCodeAt(i)&0x3ff)
                utf8.push(0xf0 | (charcode >>18),
                    0x80 | ((charcode>>12) & 0x3f),
                    0x80 | ((charcode>>6) & 0x3f),
                    0x80 | (charcode & 0x3f));
            }
        }
        return utf8;
    },

    fromUTF8Array : function(data) { // array of bytes
        let str = '',
            i;

        for (i = 0; i < data.length; i++) {
            let value = data[i];

            if (value < 0x80) {
                str += String.fromCharCode(value);
            } else if (value > 0xBF && value < 0xE0) {
                str += String.fromCharCode((value & 0x1F) << 6 | data[i + 1] & 0x3F);
                i += 1;
            } else if (value > 0xDF && value < 0xF0) {
                str += String.fromCharCode((value & 0x0F) << 12 | (data[i + 1] & 0x3F) << 6 | data[i + 2] & 0x3F);
                i += 2;
            } else {
                // surrogate pair
                var charCode = ((value & 0x07) << 18 | (data[i + 1] & 0x3F) << 12 | (data[i + 2] & 0x3F) << 6 | data[i + 3] & 0x3F) - 0x010000;

                str += String.fromCharCode(charCode >> 10 | 0xD800, charCode & 0x03FF | 0xDC00);
                i += 3;
            }
        }

        return str;
    },

    utf8StringTakeLen : function(str) { //
        let le = this.toUTF8Array(str).len;
        if(le < jm.rpc.Constants.MAX_BYTE_VALUE) {
            return le +1;
        }else if(le < jm.rpc.Constants.MAX_SHORT_VALUE) {
            return le +3;
        }else if(le < jm.rpc.Constants.MAX_INT_VALUE) {
            return le +7;
        }else {
            throw "String too long for:" + le;
        }
    },

    parseJson:function(data) {
        if(data instanceof Array) {
            let  jsonStr = this.fromUTF8Array(data);
            return JSON.parse(jsonStr);
        }else if(typeof data == 'string') {
            return JSON.parse(data);
        }else if(data instanceof ArrayBuffer){
            let byteArray = [];
            let dv = new DataView(data,data.byteLength);
            for(let i = 0; i <data.byteLength; i++) {
                byteArray.push(dv.getUint8());
            }
            let jsonStr = this.fromUTF8Array(byteArray);
            return JSON.parse(data);
        }else {
            return data;
        }
    },

    flagIs : function( flag,  mask) {
        return (flag & mask) != 0;
    },

    flagSet : function( isTrue, f, mask) {
        return isTrue ?(f |= mask):(f &= ~mask);
    }

}

jm.utils.Constants = {
    // debug level
    INFO:'INFO',
    DEBUG:'DEBUG',
    ERROR:'ERROR',
    FINAL:'FINAL',
    DEFAULT:'DEFAULT',
    IE:'ie',
    IE6:'ie6',
    IE7:'ie7',
    IE8:'ie8',
    IE9:'ie9',
    IE10:'ie10',
    IE10:'ie11',
    chrome:'chrome',
    firefox:'firefox',
    safari:'safari',
    opera:'opera'
}

if(!jm.utils.browser) {
    var ua = navigator.userAgent.toLowerCase();
    var s = null;
    var key = null;
    var bv = null;
    //"mozilla/5.0 (windows nt 10.0; wow64; trident/7.0; .net4.0c; .net4.0e; manmjs; rv:11.0) like gecko"
    if(s = ua.match(/msie ([\d.]+)/)) {
        key = jm.utils.Constants.IE;
        key = key + s[1].split('.')[0];
    }else if(s = ua.match(/rv\:([\d.]+)/)) {
        key = jm.utils.Constants.IE
    }else if(s = ua.match(/firefox\/([\d.]+)/)) {
        key = jm.utils.Constants.firefox
    }else if(s = ua.match(/chrome\/([\d.]+)/)) {
        key = jm.utils.Constants.chrome;
    }else if(s = ua.match(/opera.([\d.]+)/)) {
        key = jm.utils.Constants.opera;
    }else if(s = ua.match(/version\/([\d.]+).*safari/)) {
        key = jm.utils.Constants.safari;
    }
    jm.utils.browser = {};
    if(s != null) {
        jm.utils.browser[key] = [];
        jm.utils.browser[key][0] = s[0].trim();
        jm.utils.browser[key][1] = s[1].trim();
    }
}

if(jm.utils.isBrowser('ie')) {
    Array.prototype.fill = function(e) {
        for(let i = 0; i < this.length; i++) {
            this[i] = e;
        }
    }
}

jm.socket = {
    listeners : {},
    waiting:[]
    ,logData:null
    ,idCallback:{}
    ,isInit:false

    ,reconnect: function() {
        if(this.wsk) {
            this.wsk.close();
        }
        this.isInit = false
    }

    ,init : function(onopen) {
        this.isInit = true;
        let url = 'ws://' + jm.config.ip + ':' + jm.config.port +'/'+ jm.config.binContext;
        let self = this;
        if(window.WebSocket){
            self.wsk = new WebSocket(url);  //获得WebSocket对象

            //当有消息过来的时候触发
            self.wsk.onmessage = function(event){
                //console.log(event.data);
                //var msg = JSON.parse(event.data);
                //msg.payload = JSON.parse(msg.payload);
                event.data.arrayBuffer().then(function(buf){
                    let msg = new  jm.rpc.Message();
                    msg.decode(buf);

                    if(msg.type == jm.rpc.Constants.MSG_TYPE_ASYNC_RESP) {
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
                        jm.ps.onMsg(msg.payload);
                    } else  {
                        if(msg.getDownProtocol() == jm.rpc.Constants.PROTOCOL_BIN) {
                            let resp = new jm.rpc.ApiResponse();
                            resp.decode(msg.payload, msg.getDownProtocol());
                            msg.payload = resp;
                        }
                        if(self.listeners[msg.reqId]) {
                            self.listeners[msg.reqId](msg);
                            delete self.listeners[msg.reqId];
                        }
                    }
                });
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
        //msg.setProtocol(jm.rpc.Constants.PROTOCOL_BIN);
        let buffe = msg.encode();
        if(!!self.wsk && self.wsk.readyState == WebSocket.OPEN) {
            this.wsk.send(buffe);
        } else if(!self.wsk || self.wsk.readyState == WebSocket.CLOSED ||
            self.wsk.readyState == WebSocket.CLOSING) {
            this.init(function () {
                self.wsk.send(buffe);
                //self.wsk.send(JSON.stringify(msg));
                //self.wsk.se
                if(self.waiting.length > 0) {
                    for(let i = 0; i < self.waiting.length; i++) {
                        self.waiting[i]();
                    }
                    self.waiting = [];
                }
            });
        } else if(self.wsk.readyState == WebSocket.CONNECTING) {
            self.waiting.push(function(){
                self.wsk.send(buffe);
            })
        }
    }

    ,registListener : function(type,lis) {
        if(!this.listeners[type]) {
            this.listeners[type] = lis;
        } else {
            throw 'type:'+type + ' have been exists';
        }
    },

}

jm.transport = {
    httpContext: 'http://' + jm.config.ip + ':' + jm.config.port +'/'+ jm.config.httpContext,
    send : function(msg,cb){
        if(jm.config.useWs){
            jm.socket.send(msg,cb);
        } else {
            let buff = msg.encode();
            let xhr = new XMLHttpRequest();
            xhr.responseType='arraybuffer';
            xhr.onload = function() {
                if(xhr.readyState == 4 ) {
                    if(xhr.status == 200) {
                        let respBuff = xhr.response;
                        let respMsg = new  jm.rpc.Message();
                        respMsg.decode(respBuff);
                        if(respMsg.type == jm.rpc.Constants.MSG_TYPE_ASYNC_RESP) {
                            if(!respMsg.success) {
                                throw 'fail:' + respMsg.payload;
                            }
                            jm.rpc.onMsg(respMsg.payload);
                        } else  {
                            if(respMsg.getDownProtocol() == jm.rpc.Constants.PROTOCOL_BIN) {
                                let resp = new jm.rpc.ApiResponse();
                                resp.decode(respMsg.payload, respMsg.getDownProtocol());
                                respMsg.payload = resp;
                            }
                            cb(respMsg);
                        }
                    }else {
                        cb(null,xhr.statusText);
                    }
                }
            }
            xhr.open('POST',this.httpContext,true);
            xhr.send(buff);
        }
    },

}

jm.rpc = {
    idCache:{},

    actInfo:null,
    actListeners:{},

    addActListener : function(key,l) {
        /*if(!!this.actListeners[key]) {
            throw 'Exist listener: ' + key;
        }*/
        this.actListeners[key] = l;
    },

    removeActListener : function(key) {
        if(!this.actListeners[key]) {
            return;
        }
        delete this.actListeners[key];
    },

    __actreq : function(method,args){
        let req = {};
        req.serviceName = 'cn.jmicro.api.security.IAccountService';
        req.namespace = 'sec';
        req.version = '0.0.1';
        req.args = args;
        req.method = method;
        return req;
    },

    isLogin: function () {
        return this.actInfo != null;
    },

    login: function (actName,pwd,cb){
        if(this.actInfo && cb) {
            cb(this.actInfo,null);
            return;
        }
        let self = this;
        jm.rpc.callRpc(this.__actreq('login',[actName,pwd]))
            .then(( resp )=>{
                if(resp.code == 0) {
                    self.actInfo = resp.data;
                    cb(self.actInfo,null);
                    self._notify(jm.rpc.Constants.LOGIN);
                } else {
                    cb(null,resp.msg);
                }
            }).catch((err)=>{
            console.log(err);
            cb(null,err);
        });;
    },

    _notify : function(type) {
        for(let key in this.actListeners) {
            if(this.actListeners[key]) {
                this.actListeners[key](type,this.actInfo);
            }
        }
    },

    logout: function (cb){
        if(!this.actInfo) {
            if(cb) {
                cb(true,null)
            }
            return;
        }
        let self = this;
        jm.rpc.callRpc(this.__actreq('logout',[]))
            .then(( resp )=>{
                if(resp.data) {
                    self.actInfo = null;
                    if(cb) {
                        cb(true,null)
                    }
                    self._notify(jm.rpc.Constants.LOGOUT);
                }else {
                    if(cb) {
                        cb(false,'logout fail')
                    }
                }
            }).catch((err)=>{
            console.log(err);
            if(cb) {
                cb(false,err)
            }
        });
    },

    init : function(ip,port,actName,pwd){
        if(jm.config.useWs && !window.WebSocket){
            jm.config.useWs = false;
        }

        if(ip && ip.length > 0) {
            jm.config.ip = ip;
        }

        if(port && port > 0) {
            jm.config.port = port;
        }

        if(actName && actName.length > 0) {
            window.jm.localStorage.set("actName",actName);
        }

        if(pwd && pwd.length > 0) {
            window.jm.localStorage.set("pwd",pwd);
        }

        let req = {};
        req.serviceName = 'cn.jmicro.api.gateway.IHostNamedService';
        req.namespace = 'gateway';
        req.version = '0.0.1';
        req.method = 'bestHost';
        req.args = [];

        jm.rpc.callRpc(req)
            .then((data)=>{
                if(data && data.length > 0) {
                    //let jo = jm.utils.parseJson(data);
                    let arr = data.split(':');
                    if(arr[0] && arr[1] && ( arr[0] != jm.config.ip ||  arr[1] != jm.config.port )) {
                        jm.config.ip = arr[0];
                        jm.config.port = arr[1];
                        jm.socket.reconnect();
                    }
                } else {
                    throw "API gateway host not found!";
                }
            }).catch((err)=>{
                throw err;
            })
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
        //msg.setLoggable(false);
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

    callRpc : function(req,upProtocol,downProtocol) {
        if(!upProtocol) {
            upProtocol = jm.rpc.Constants.PROTOCOL_JSON;
        }
        if(!downProtocol) {
            downProtocol = jm.rpc.Constants.PROTOCOL_JSON;
        }
        let self = this;
        return new Promise(function(reso,reje){
            self.callRpc0(req, null, upProtocol,downProtocol)
                .then((data)=>{
                    if(downProtocol == jm.rpc.Constants.PROTOCOL_BIN) {
                        reso(jm.utils.parseJson(data));
                    } else {
                        reso(data);
                    }
                })
                .catch((err)=>{
                    reje(err);
                })
        });
    },

    callRpc0 : function(param,type,upProtocol,downProtocol){
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

    },


    callRpcWithParams : function(service,namespace,version,method,args) {
        let req = {};
        req.serviceName = service;
        req.namespace = namespace;
        req.version = version;
        req.method = method;
        req.args = args;
        return jm.rpc.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
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

            if(!!jm.rpc && !!jm.rpc.actInfo) {
                req.params['loginKey'] = jm.rpc.actInfo.loginKey;
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
                    let rst = rstMsg.payload.result
                    console.log(rst);
                    let doFailure = true;
                    if(rst && rst.errorCode != 0) {
                        //alert(rst.msg);
                        if(rst.errorCode == 0x00000004 || rst.errorCode == 0x00000006) {
                            let actName = window.jm.localStorage.get("actName");
                            let pwd = window.jm.localStorage.get("pwd");
                            if(actName && pwd) {
                                jm.rpc.actInfo = null;
                                window.jm.rpc.login(actName,pwd,(actInfo,err)=>{
                                    if(actInfo && !err) {
                                        self.callRpcWithTypeAndProtocol(req,type,upProtocol,downProtocol)
                                            .then(( r,err )=>{
                                                if(r ) {
                                                    reso(r);
                                                } else {
                                                    reje(err);
                                                }
                                            }).catch((err)=>{
                                            console.log(err);
                                            reje(err);
                                        });
                                    }else {
                                        reje(err || rst);
                                    }
                                });
                                doFailure = false;
                            }
                        }
                    }

                    if(doFailure) {
                        reje(err || rst);
                    }
                } else {
                    let rst = rstMsg.payload.result;
                    if(rst != null && rst.hasOwnProperty('errorCode') && rst.hasOwnProperty('msg')) {
                        reje(rst);
                    }else {
                        reso(rst);
                    }
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

}

jm.ps = {

    psListeners:{},

    onMsg : function(msg) {
        let cbs = this.psListeners[msg.topic];
        for(let i = 0; i < cbs.length; i++){
            if(!!cbs[i]) {
                cbs[i](msg);
            }
        }
    },

    subscribe: function (topic,ctx,callback){

        if(this.psListeners[topic] && this.psListeners[topic].length > 0) {
            let cs = this.psListeners[topic];
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

        let self = this;
        if(!self.psListeners[topic]) {
            self.psListeners[topic] = [];
        }
        self.psListeners[topic].push(callback);

        return new Promise(function(reso,reje){
            jm.rpc.callRpcWithParams(self.pssn,self.ns,self.v,'subscribe',[topic, ctx || {}])
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

    unsubscribe: function (topic,callback){
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
            return new Promise(function(reso,reje){
                reso(0);
            });
        }
        let self = this;
        return new Promise(function(reso,reje){
            jm.rpc.callRpcWithParams(self.pssn, self.ns, self.v, 'unsubscribe',[callback.id])
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
    publishBytes: function(topic, byteArray,persist,queue,callback,itemContext){
        return this._publishItem(topic, byteArray,persist,queue,callback,itemContext);
    },
    //发送字符串消息
    publishString: function(topic,content,persist,queue,callback,itemContext){
        return this._publishItem(topic, content,persist,queue,callback,itemContext);
    },

    //通过消息服务器调用别外一个RPC方法，args为RPC方法的参数
    callService: function (topic,args,persist,queue,callback,itemContext){
        return this._publishItem(topic,args,persist,queue,callback,itemContext);
    },

    //同时发送多个消息，psItems为消息数组
    publishMultiItems: function (psItems){
        return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'publishMutilItems',[psItems]);
    },

    //发送单个消息
    publishOneItem: function (psItem){
        return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'publishOneItem',[psItem]);
    },

    _item: function(topic, data,persist,queue,callback,itemContext) {
        if(!jm.rpc.isLogin()) {
            throw 'Not login';
        }
        if(!topic ||topic.length == 0) {
            throw 'Topic cannot be null';
        }
        if(!data) {
            throw 'Message body cannot be null';
        }

        let psData = new jm.rpc.PSData(topic,data);

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
        psData.srcClientId = jm.rpc.actInfo.clientId;

        return psData;
    },

    _publishItem: function(topic, data,persist,queue,callback,itemContext) {
        let psData =this._item(topic, data,persist,queue,callback,itemContext);
        return this.publishOneItem(psData);
    },

     pssn:"cn.jmicro.gateway.MessageServiceImpl",
     sn:'cn.jmicro.api.pubsub.IPubSubClientService',
     ns : 'mng',
     v:'0.0.1',
     MSG_TYPE_ASYNC_RESP : 0x06,
},

jm.http = {

    getQueryParam : function(name) {
        if(location.href.indexOf("?")==-1 || location.href.indexOf(name+'=')==-1){
            return '';
        }
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)");
        var r = window.location.search.substr(1).match(reg);
        if (r != null)
            return decodeURI(r[2]);   //对参数进行decodeURI解码
        return null;
    },

    get: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'GET', errCb, fullpath)
    },

    post: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'POST', errCb, fullpath)
    },

    put: function (path, params, cb, errCb, fullpath) {
        this.ajax(path, params, cb, true, 'PUT', errCb, fullpath)
    },

    ajax: function (path, params, sucCb, async, method, errCb, fullpath) {
        if (!sucCb) {
            throw "Callback method cannot be null";
        }

        if (!method) {
            throw "request method cannot be null";
        }

        if (!path) {
            path = "/";
        }

        this._doAjax(path, params, sucCb, async, method, errCb, fullpath);
    }

    ,_doAjax: function(path, params, sucCb, async, method, errCb, fullpath) {
        /* if(!jm.urls.checkNetwork()) {
         throw '网络连接不可用';
         }*/
        if(fullpath) {
            path = fullpath;
        } else if (!(path.startWith('http://') &&  !path.startWith('https://'))){
            path = this.getWebResourceHttpPath(path);
        }
        if(!params) {
            params = {};
        }

        $.ajax({
            data: params,
            type: method,
            async: true,
            url: path,
            jsonp: 'json',
            success: function (data, statuCode, xhr) {
                sucCb(data, statuCode, xhr);
            },
            beforeSend: function (xhr) {
                //xhr.setRequestHeader('Access-Control-Allow-Headers','*');
                /*if (jm.zdd.uc.isLogin()) {
                    xhr.setRequestHeader("loginKey", params.loginKey);
                }*/
            },
            error: function (err, xhr) {
                if (errCb) {
                    errCb(err, xhr);
                } else {
                    sucCb(null, err, xhr);
                }
            }
        });
    }

    ,uploadBase64Picture: function (data, ptype, cb,pos) {
        var params = {
            encode: 'base64',
            imageType: ptype,
            name: 'base64_picture_file' + jm.utils.getId(),
            contentType: 'image/' + ptype + ';base64',
            base64Data: data,
            pos:pos
        }
        var url = this.getWebResourceHttpPath(jm.http.UPLOAD_BASE64);
        jm.http.post(url, params, function (data1, status, xhr) {
            console.log(data1);
            if (status !== 'success') {
                cb(status, data1);
            } else {
                cb(null, data1);
            }
        }, function (err, xhr) {
            console.log(err);
            cb(err, null);
        })
    }

    ,postHelper: function(url,params,cb){
        jm.http.post(url,params, function(data,status,xhr){
            if(data.success) {
                cb(null,data);
            } else {
                cb(data,data);
            }
        },function(err){
            cb(err,null);
        });
    }

    //get: function (path, params, cb, errCb, fullpath)
    ,getHelper: function(url,params,cb,fullpath){
        if(!params) {
            params = {};
        }
        this.get(url,params, function(data,status,xhr){
            cb(null,data);
        },function(err){
            cb(err,null);
        },fullpath);
    }

    ,getUrl: function(url) {
        return this.getWebResourceHttpPath(url);
    },

    getWebContextPath : function() {
        var pathname = location.pathname
        pathname = pathname.substring(1,pathname.length);
        pathname = pathname.substring(0,pathname.indexOf('/'));
        return '/'+pathname;
    },

    getWSApiPath : function() {
        var wp = 'ws://' + location.host+ + jm.config.wsContext;
        return wp;
    },

    getHttpApiPath : function() {
        var wp = 'http://' + location.host + jm.config.httpContext;
        return wp;
    },

    getWebResourceHttpPath : function(subPath) {
        var wp = 'http://' + location.host + jm.utils.getWebContextPath()
            + subPath;
        return wp;
    }
}

jm.rpc.Constants = {

    MSG_TYPE_API_REQ : 0x09, //API网关请求
    MSG_TYPE_API_RESP : 0x0A,//API网关请求响应

    MSG_TYPE_REQ_RAW : 0x03,  //纯二进制数据请求
    MSG_TYPE_RRESP_RAW : 0x04, //纯二进制数据响应

    MSG_TYPE_ASYNC_RESP : 0x06,

    LOG_NO : 0,
    LOG_FINAL : 6,

    CHARSET : 'UTF-8',

    LOGIN:1,
    LOGOUT:2,

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

    //长度字段类型，1表示整数，0表示短整数
    FLAG_LENGTH_INT : 1 << 0,

    //调试模式
    FLAG_DEBUG_MODE  :  1 << 1,

    //需要响应的请求
    FLAG_NEED_RESPONSE  :  1 << 2,

    FLAG_UP_PROTOCOL  :  1<<5,

    FLAG_DOWN_PROTOCOL  :  1 << 6,

    //DUMP上行数据
    FLAG_DUMP_UP  :  1 << 7,

    //DUMP下行数据
    FLAG_DUMP_DOWN  :  1 << 8,

    //可监控消息
    FLAG_MONITORABLE  :  1 << 9,

    //可监控消息
    FLAG_ASYNC_RESUTN_RESULT  :  1 << 13,

}

jm.rpc.PSData = function(topic,data) {

    //消息ID,唯一标识一个消息
    this.id = 0;
    this.flag = jm.rpc.PSData.Constants.FLAG_DEFALUT;
    this.topic = topic;
    this.srcClientId = -1;
    this.data = data;
    //消息发送结果回调的RPC方法，用于消息服务器给发送者回调
    this.callback = null;
    this.context = null;

}

jm.rpc.PSData.Constants = {
    FLAG_DEFALUT : 0,
    FLAG_QUEUE : 1<<0,
    FLAG_PUBSUB : 0<<0,
    //1右移1位，异步方法，决定回调方法的参数类型为消息通知的返回值
    FLAG_ASYNC_METHOD : 1<<1,
    //1右移两位，消息回调通知，决定回调方法的参数类型为消息通知的返回值分别为 消息发送状态码，消息ID，消息上下文透传
    FLAG_MESSAGE_CALLBACK : 1<<2,
    FLAG_PERSIST : 1<<3,
    FLAG_CALLBACK_TOPIC : 1<<4,
    FLAG_CALLBACK_METHOD : 0<<4,

    RESULT_SUCCCESS : 0,

    PUB_OK : 0,
    //无消息服务可用,需要启动消息服务
    PUB_SERVER_NOT_AVAILABALE : -1,
    //消息队列已经满了,客户端可以重发,或等待一会再重发
    PUB_SERVER_DISCARD : -2,
    //消息服务线程队列已满,客户端可以重发,或等待一会再重发,可以考虑增加消息服务线程池大小,或增加消息服务
    PUB_SERVER_BUSUY : -3,

    PUB_TOPIC_INVALID: -4,

    //消息服务器不可用
    RESULT_FAIL_SERVER_DISABLE : -5,

    //发送给消息订阅者失败
    RESULT_FAIL_DISPATCH : -6,

    //回调结果通知失败
    RESULT_FAIL_CALLBACK : -7,

}

jm.rpc.PSData.prototype.isPersist = function() {
    return jm.utils.flagIs(this.flag,jm.rpc.PSData.Constants.FLAG_PERSIST);
}

jm.rpc.PSData.prototype.setPersist = function(f) {
    this.flag = jm.utils.flagSet(f,this.flag,jm.rpc.PSData.Constants.FLAG_PERSIST);
}

jm.rpc.PSData.prototype.queue = function() {
    this.flag = jm.utils.flagSet(true,this.flag,jm.rpc.PSData.Constants.FLAG_QUEUE);
}

jm.rpc.PSData.prototype.pubsub = function() {
    this.flag = jm.utils.flagSet(false,this.flag,jm.rpc.PSData.Constants.FLAG_PUBSUB);
}

jm.rpc.PSData.prototype.callbackTopic = function() {
    this.flag = jm.utils.flagSet(true,this.flag,jm.rpc.PSData.Constants.FLAG_CALLBACK_TOPIC);
}

jm.rpc.PSData.prototype.callbackMethod = function() {
    this.flag = jm.utils.flagSet(false,this.flag,jm.rpc.PSData.Constants.FLAG_CALLBACK_METHOD);
}

jm.rpc.PSData.prototype.isCallbackTopic = function() {
    return jm.utils.flagIs(this.flag,jm.rpc.PSData.Constants.FLAG_CALLBACK_TOPIC);
}

jm.rpc.PSData.prototype.isCallbackMethod = function() {
    return !jm.utils.flagIs(this.flag,jm.rpc.PSData.Constants.FLAG_CALLBACK_TOPIC);
}

jm.rpc.PSData.prototype.isQueue = function() {
    return jm.utils.flagIs(this.flag,jm.rpc.PSData.Constants.FLAG_QUEUE);
}

jm.rpc.PSData.prototype.isPubsub = function() {
    return !jm.utils.flagIs(this.flag,jm.rpc.PSData.Constants.FLAG_QUEUE);
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
     * 0        S:       data length type 0:short 1 : int
     * 1        dm:      is development mode
     * 2        N:       need Response
     * 3,4      PP:      Message priority
     * 5        UPR:     up protocol  0:bin,  1: json
     * 6        DPR:     down protocol 0:bin, 1 : json
     * 7        up:      dump up stream data
     * 8        do:      dump down stream data
     * 9        M:       Monitorable
     * 10,11,12 LLL      Log level
     * 13       A:       async return result，different from async RPC
     *
     A   L  L   L   M  DO UP  DPR  UPR  P    P   N   dm   S
     |    |   |   |  |   |   |  |  |   |    |    |    |   |    |   |
     15  14  13  12  11  10  9  8  7   6    5    4    3   2    1   0

     * @return
     */
    this.flag = 0;

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

jm.rpc.Message.prototype.isAsyncReturnResult = function() {
    return this.is(this.flag,jm.rpc.Constants.FLAG_ASYNC_RESUTN_RESULT);
}

jm.rpc.Message.prototype.setAsyncReturnResult = function(f) {
    this.flag = set(f,this.flag,jm.rpc.Constants.FLAG_ASYNC_RESUTN_RESULT);
}

jm.rpc.Message.prototype.isDumpUpStream = function()  {
    return this.is(this.flag,jm.rpc.Constants.FLAG_DUMP_UP);
}

//public boolean
jm.rpc.Message.prototype.isDumpDownStream = function() {
    return this.is(this.flag, jm.rpc.Constants.FLAG_DUMP_DOWN);
}

//public void
jm.rpc.Message.prototype.setDumpUpStream = function(f)  {
    //this.flag |= f ? jm.rpc.Constants.FLAG_DUMP_UP : 0 ;
    this.flag  = this.set(f,this.flag ,jm.rpc.Constants.FLAG_DUMP_UP);
}

//public boolean
jm.rpc.Message.prototype.setDumpDownStream = function(f)  {
    //return this.is(flag,jm.rpc.Constants.FLAG_DUMP_DOWN);
    this.flag  = this.set(f,this.flag ,jm.rpc.Constants.FLAG_DUMP_DOWN);
}

//public boolean
jm.rpc.Message.prototype.isLoggable = function()  {
    return this.is(this.flag,jm.rpc.Constants.FLAG_LOGGABLE);
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
    return this.is(this.flag,jm.rpc.Constants.FLAG_MONITORABLE);
}

//public void
jm.rpc.Message.prototype.setMonitorable = function(f)  {
   // this.flag |= f ? jm.rpc.Constants.FLAG_MONITORABLE : 0 ;
    this.flag  = this.set(f,this.flag ,jm.rpc.Constants.FLAG_MONITORABLE);
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
    if(l > jm.rpc.Constants.PRIORITY_3 || l < jm.rpc.Constants.PRIORITY_0) {
        throw "Invalid priority: "+l;
    }
    this.flag = ((l << 3) | this.flag);
}

//public byte
jm.rpc.Message.prototype.getLogLevel = function()  {
    return ((this.flag >>> 10) & 0x07);
}

//public void
jm.rpc.Message.prototype.setLogLevel = function(v)  {
    if(v < jm.rpc.Constants.LOG_NO || v > jm.rpc.Constants.LOG_FINAL) {
        throw "Invalid Log level: "+v;
    }
    this.flag = ((v << 10) | this.flag);
}

jm.rpc.Message.prototype.getUpProtocol=function() {
    return this.is(this.flag,jm.rpc.Constants.FLAG_UP_PROTOCOL) ? 1:0;
}

jm.rpc.Message.prototype.setUpProtocol=function(protocol) {
    this.flag  = this.set(protocol == jm.rpc.Constants.PROTOCOL_JSON ,this.flag , jm.rpc.Constants.FLAG_UP_PROTOCOL);
}

jm.rpc.Message.prototype.getDownProtocol = function() {
    return this.is(this.flag, jm.rpc.Constants.FLAG_DOWN_PROTOCOL)?1:0;
}

jm.rpc.Message.prototype.setDownProtocol = function(protocol) {
    //this.flag |= protocol == jm.rpc.Constants.PROTOCOL_JSON ? jm.rpc.Constants.FLAG_DOWN_PROTOCOL : 0 ;
    this.flag  = this.set(protocol == jm.rpc.Constants.PROTOCOL_JSON ,this.flag ,jm.rpc.Constants.FLAG_DOWN_PROTOCOL);
}

//public static Message
jm.rpc.Message.prototype.decode = function(b) {
    let msg = this;
    let dataInput = new jm.utils.JDataInput(b);
    //第0个字节
    msg.flag = dataInput.readUnsignedShort();
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
    //msg.flag = dataInput.getUByte();

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
    buf.writeUnsignedShort(this.flag);

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
        + ", type=" + this.type + ", flag=" + Number.toHexString(this.flag)
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


jm.utils.JDataInput = function(buf) {
    if(!buf) {
        throw 'Read buf cannot be null';
    }
    if(buf instanceof Array) {
        this.buf = new DataView(new ArrayBuffer(buf.length),0, buf.length) ;
        for(let i = 0; i < buf.length; i++) {
            this.buf.setInt8(i,buf[i]);
        }
    }else if (buf instanceof ArrayBuffer) {
        this.buf = new DataView(buf,0, buf.byteLength);
    }else {
        throw 'Not support construct ArrayBuffer from '+(typeof buf);
    }

    this.readPos = 0;
}

//public static int
jm.utils.JDataInput.prototype.readUnsignedShort = function() {
    let firstByte = this.atByte();
    let secondByte = this.atByte();
    let anUnsignedShort  =  firstByte << 8 | secondByte;
    return anUnsignedShort;
},

    jm.utils.JDataInput.prototype.readInt = function() {
        let firstByte = this.atByte();
        let secondByte = this.atByte();
        let thirdByte = this.atByte();
        let fourthByte = this.atByte();
        let anUnsignedInt  = ((firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFF;
        return anUnsignedInt;
    },

//public static long
    jm.utils.JDataInput.prototype.readUnsignedInt = function() {
        /*let firstByte = this.atByte();
        let secondByte = this.atByte();
        let thirdByte = this.atByte();
        let fourthByte = this.atByte();
        let anUnsignedInt  = ((firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFF;
        return anUnsignedInt;*/
        let b = this.getUByte() & 0xff;
        let n = b & 0x7f;
        if (b > 0x7f) {
            b = this.getUByte() & 0xff;
            n ^= (b & 0x7f) << 7;
            if (b > 0x7f) {
                b = this.getUByte() & 0xff;
                n ^= (b & 0x7f) << 14;
                if (b > 0x7f) {
                    b = this.getUByte() & 0xff;
                    n ^= (b & 0x7f) << 21;
                    if (b > 0x7f) {
                        b = this.getUByte() & 0xff;
                        n ^= (b & 0x7f) << 28;
                        if (b > 0x7f) {
                            throw "Invalid int encoding";
                        }
                    }
                }
            }
        }
        return (n >>> 1) ^ -(n & 1); // back to two's-complement
    },

    jm.utils.JDataInput.prototype.getUByte = function() {
        return this.buf.getUint8(this.readPos++);
    }

jm.utils.JDataInput.prototype.atByte = function() {
    return 0xFF & this.getUByte();
}

jm.utils.JDataInput.prototype.remaining = function() {
    return this.buf.byteLength - this.readPos;
}

//public static long
jm.utils.JDataInput.prototype.readUnsignedLong = function() {
    let firstByte = this.atByte();
    let secondByte = this.atByte();
    let thirdByte = this.atByte();
    let fourthByte = this.atByte();

    let fiveByte = this.atByte();
    let sixByte = this.atByte();
    let sevenByte = this.atByte();
    let eightByte = this.atByte();

    let anUnsignedLong  =
        (firstByte << 56 | secondByte << 48 | thirdByte << 40 | fourthByte << 32 |
            fiveByte << 24 | sixByte << 16 | sevenByte << 8 | eightByte) & 0xFFFFFFFFFFFFFFFF;
    return anUnsignedLong;
},

    jm.utils.JDataInput.prototype.readUtf8String = function() {
        let len = this.getUByte();

        if(len == -1) {
            return null;
        }else if(len == 0) {
            return "";
        }

        //Byte.MAX_VALUE
        if(len == jm.rpc.Constants.MAX_BYTE_VALUE) {
            len = this.readUnsignedShort();
            //Short.MAX_VALUE
            if(len == jm.rpc.Constants.MAX_SHORT_VALUE) {
                len = this.readUnsignedInt();
            }
        }

        let arr = [];
        for(let i = 0; i < len; i++) {
            arr.push(this.getUByte());
        }

        return jm.utils.fromUTF8Array(arr);
    }


jm.utils.JDataOutput = function(buf) {
    if( buf instanceof ArrayBuffer) {
        this._buf = buf;
    } else if(typeof buf == 'number') {
        let size = parseInt(buf);
        this._buf = new ArrayBuffer(size);
    }
    this.buf = new DataView(this._buf);
    this.oriSize = this._buf.byteLength;
    this.writePos = 0;
}

jm.utils.JDataOutput.prototype.getBuf = function() {
    return this._buf.slice(0,this.writePos);
}

jm.utils.JDataOutput.prototype.writeUByte = function(v) {
    this.buf.setUint8(this.writePos++,v);
}

jm.utils.JDataOutput.prototype.remaining = function() {
    return this._buf.byteLength - this.writePos;
}

jm.utils.JDataOutput.prototype.checkCapacity = function(len) {
    let rem = this.remaining();
    if(rem >= len) {
        return;
    }

    let size = this.oriSize;
    while(size - (this.oriSize-rem) < len) {
        size *= 2;
    }

    this.oriSize = size;

    let buf0 = new ArrayBuffer(this.oriSize);
    let newBuf = new DataView(buf0,0,this.oriSize);
    for(let i = 0; i < this.writePos; i++) {
        newBuf.setUint8(i,this.buf.getUint8(i));
    }
    this._buf = buf0;
    this.buf = newBuf;

}

//public static void
jm.utils.JDataOutput.prototype.writeUnsignedShort = function(v) {
    if(v > jm.rpc.Constants.MAX_SHORT_VALUE) {
        throw "Max short value is :"+jm.rpc.Constants.MAX_SHORT_VALUE+", but value "+v;
    }
    this.checkCapacity(2);
    this.writeUByte((v >>> 8) & 0xFF);
    this.writeUByte((v >>> 0) & 0xFF);
},

//public static void
    jm.utils.JDataOutput.prototype.writeUnsignedByte = function(v) {
        if(v > jm.rpc.Constants.MAX_BYTE_VALUE) {
            throw "Max byte value is :"+jm.rpc.Constants.MAX_BYTE_VALUE+", but value "+v;
        }
        this.checkCapacity(1);
        this.writeUByte(v);
    },

    jm.utils.JDataOutput.prototype.writeInt = function(v) {
        if(v > jm.rpc.Constants.MAX_INT_VALUE) {
            throw "Max int value is :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+v;
        }
        this.checkCapacity(4);
        //高字节底地址
        this.writeUByte((v >>> 24)&0xFF);
        this.writeUByte((v >>> 16)&0xFF);
        this.writeUByte((v >>> 8)&0xFF);
        this.writeUByte((v >>> 0)&0xFF);

    }

//public static void
jm.utils.JDataOutput.prototype.writeUnsignedInt = function(n) {
    if(n > jm.rpc.Constants.MAX_INT_VALUE) {
        throw "Max int value is :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+v;
    }
    this.checkCapacity(4);
    n = (n << 1) ^ (n >> 31);
    if ((n & ~0x7F) != 0) {
        this.writeUByte((n | 0x80) & 0xFF);
        n >>>= 7;
        if (n > 0x7F) {
            this.writeUByte((n | 0x80) & 0xFF);
            n >>>= 7;
            if (n > 0x7F) {
                this.writeUByte((n | 0x80) & 0xFF);
                n >>>= 7;
                if (n > 0x7F) {
                    this.writeUByte((n | 0x80) & 0xFF);
                    n >>>= 7;
                }
            }
        }
    }
    this.writeUByte(n);

}

jm.utils.JDataOutput.prototype.writeUnsignedLong = function(v) {
    if(v > jm.rpc.Constants.MAX_INT_VALUE) {
        throw "Max int value is :"+jm.rpc.Constants.MAX_INT_VALUE+", but value "+v;
    }
    this.checkCapacity(8);
    this.writeUByte((v >>> 56)&0xFF);
    this.writeUByte((v >>> 48)&0xFF);
    this.writeUByte((v >>> 40)&0xFF);
    this.writeUByte((v >>> 32)&0xFF);
    this.writeUByte((v >>> 24)&0xFF);
    this.writeUByte((v >>> 16)&0xFF);
    this.writeUByte((v >>> 8)&0xFF);
    this.writeUByte((v >>> 0)&0xFF);
}

jm.utils.JDataOutput.prototype.writeByteArray = function(arr) {
    if(!arr || arr.length == 0) {
        this.checkCapacity(4);
        this.writeUnsignedInt(0)
        return;
    }
    let size = arr.length;
    this.checkCapacity(4+size);
    this.writeUnsignedInt(size);
    for(let i = 0; i < size; i++) {
        this.writeUByte(arr[i])
    }
}

jm.utils.JDataOutput.prototype.writeArrayBuffer = function(ab) {
    if(!ab || ab.byteLength == 0) {
        this.checkCapacity(4);
        this.writeUnsignedInt(0)
    }else {
        let size = ab.byteLength;
        this.checkCapacity(4+size);
        this.writeUnsignedInt(size);
        for(let i = 0; i < size; i++) {
            this.writeUByte(ab[i])
        }
    }
}

jm.utils.JDataOutput.prototype.writeUtf8String = function(s) {

    if(s == null) {
        this.checkCapacity(1);
        this.writeUByte(-1);
        return 0;
    } else if(s.length == 0) {
        this.checkCapacity(1);
        this.writeUByte(0);
        return 0;
    }

    let self = this;
    let data = jm.utils.toUTF8Array(s);

    let le = data.length;
    let needLen = le;
    if(le < jm.rpc.Constants.MAX_BYTE_VALUE) {
        needLen = le +1;
        self.checkCapacity(needLen);
        self.writeUByte(le);
    }else if(le < jm.rpc.Constants.MAX_SHORT_VALUE) {
        needLen = le +3;
        this.checkCapacity(needLen);
        self.writeUByte(jm.rpc.Constants.MAX_BYTE_VALUE);
        self.writeUnsignedShort(le);
    }else if(le < jm.rpc.Constants.MAX_INT_VALUE) {
        needLen = le +7;
        self.checkCapacity(needLen);
        self.writeUByte(jm.rpc.Constants.MAX_BYTE_VALUE);
        self.writeUnsignedShort(jm.rpc.Constants.MAX_SHORT_VALUE);
        self.writeUnsignedInt(le);
    }else {
        throw "String too long for:" + le;
    }
    if(le > 0) {
        for(let i = 0; i < le; i++) {
            this.writeUByte(data[i]);
        }
    }
    return needLen;
}

jm.utils.JDataOutput.prototype.writeObject = function(obj) {
    let len = 0;
    for(let key in obj) {
        len++;
    }

    this.writeUnsignedInt(len);
    for(let key in obj) {
        //全部写为字符串
        this.writeUtf8String(key+"");
        this.writeUtf8String(obj[key]+"");
    }

}

jm.utils.JDataOutput.prototype.writeObjectArray = function(arr) {
    if(arr == null || arr.length == 0) {
        this.checkCapacity(4);
        this.writeUnsignedInt(0);
    } else {
        this.checkCapacity(4);
        this.writeUnsignedInt(arr.length);
        for(let i = 0; i <arr.length; i++) {
            let o = arr[i];
            let t = typeof o;
            if(t == 'undefined') {
                throw 'RPC param cannot be undefined';
            }else if(t == 'string') {
                this.writeUtf8String(o);
            }else if(t == 'boolean') {
                if(o) {
                    this.writeUByte(1);
                }else {
                    this.writeUByte(0);
                }
            }else if(t == 'number') {
                this.writeUnsignedLong(o);
            }else if(o instanceof  Array) {
                this.writeByteArray(o);
            }else if (o instanceof ArrayBuffer ) {
                this.writeArrayBuffer(o)
            }else if (t == 'object' ) {
                this.writeObject(o)
            }else {
                throw 'not support encode: ' + o;
            }
        }
    }
}

jm.localStorage = new function() {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    this.set = function(key,value) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        localStorage.setItem(key,value);
    }

    this.remove = function(key) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        localStorage.removeItem(key);
    }

    this.get = function(key) {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        return localStorage.getItem(key);
    }

    this.clear = function() {
        if(!this.isSupport()) {
            alert(this.updateBrowser_);
            return;
        }
        if(confirm('this operation will clear all local storage data?')) {
            localStorage.clear();
        }
    }

    this.supportLocalstorage = function() {
        try {
            return window.localStorage !== null;
        } catch (e) {
            return false;
        }
    }

    this.isSupport = function() {
        if(jm.utils.isIe() && jm.utils.version() < 8) {
            return false;
        }else {
            return true;
        }
    }

}

jm.sessionStorage = new function() {
    //this.updateBrowser_ = jm.utils.i18n.get('update_your_browser');
    this.set = function(key,value) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.setItem(key,value);
    }

    this.remove = function(key) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.removeItem(key);
    }

    this.get = function(key) {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        return sessionStorage.getItem(key);
    }

    this.clear = function() {
        if(!this.supportLocalstorage()) {
            alert(this.updateBrowser_);
            return;
        }
        sessionStorage.clear();
    }

    this.supportLocalstorage = function() {
        try {
            return window.sessionStorage !== null;
        } catch (e) {
            return false;
        }
    }
}