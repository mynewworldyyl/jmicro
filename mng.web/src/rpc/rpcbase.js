
import localStorage from "@/rpc/localStorage";
import ApiRequest from "@/rpc/request";
import cons from "@/rpc/constants";
import security from "@/rpc/security";
import config from "@/rpc/config"
import utils from "@/rpc/utils"

import {Message,Constants} from "@/rpc/message"

import transport from "@/rpc/transport"

import socket from "@/rpc/socket";

//let idCache = {};
let actListeners = {};

let mk2code = {
    "cn.jmicro.api.gateway.IBaseGatewayService##apigateway##0.0.1########fnvHash1a":1630526296,
};

let errCode2Msg = {
    0x06 : "Service not available maybe not started",
};

let reqId = 1;

function __actreq(method,args){
    let req = {};
    req.serviceName = 'cn.jmicro.security.api.IAccountService';
    req.namespace = cons.NS_SECURITY;
    req.version = '0.0.1';
    req.args = args;
    req.method = method;
    return req;
}

export default {
    config,
    actInfo:null,

    addActListener (key,l) {
        /*if(!!this.actListeners[key]) {
            throw 'Exist listener: ' + key;
        }*/
        actListeners[key] = l;
    },

    removeActListener(key) {
        if(!actListeners[key]) {
            return;
        }
        delete actListeners[key];
    },

    isLogin() {
        return this.actInfo != null;
    },

    isAdmin(){
        return this.actInfo != null && this.actInfo.isAdmin;
    },

    login(actName,pwd,cb){
        if(this.actInfo && cb) {
            cb(this.actInfo,null);
            return;
        }
        this.actInfo = null;
        if(!actName) {
            //自动建立匿名账号
            actName = localStorage.get("guestName");
            if(!actName) {
                actName = "";
            }
        }

        if(!pwd) {
            pwd = "";
        }

        let self = this;
        let req = __actreq('login',[actName,pwd]);
        //req.serviceName = 'cn.jmicro.api.security.IAccountService';
        this.callRpc(req)
            .then(( resp )=>{
                if(resp.code == 0) {
                    self.actInfo = resp.data;

                    localStorage.set("actName",self.actInfo.actName);

                    let rememberPwd = localStorage.get("rememberPwd");
                    if(rememberPwd || self.actInfo.actName.startWith("guest_")) {
                        localStorage.set("pwd",pwd);
                    }

                    if(self.actInfo.actName.startWith("guest_")) {
                        localStorage.set("guestName",self.actInfo.actName);
                    }

                    cb(self.actInfo,null);
                    self._notify(Constants.LOGIN);
                } else {
                    cb(null,resp.msg);
                }
            }).catch((err)=>{
            console.log(err);
            cb(null,err);
        });
    },

    _notify : function(type) {
        for(let key in actListeners) {
            if(actListeners[key]) {
                actListeners[key](type,this.actInfo);
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
        self.callRpc(this.__actreq('logout',[]))
            .then(( resp )=>{
                if(resp.data) {
                    self.actInfo = null;
                    if(cb) {
                        cb(true,null)
                    }
                    self._notify(Constants.LOGOUT);
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
        if(config.useWs && !window.WebSocket){
            config.useWs = false;
        }

        if(config.sslEnable) {
            security.init();
        }

        if(ip && ip.length > 0) {
            config.ip = ip;
        }

        if(port && port > 0) {
            config.port = port;
        }

        if(actName && actName.length > 0) {
            localStorage.set("actName",actName);
        }

        if(pwd && pwd.length > 0) {
            localStorage.set("pwd",pwd);
        }

        let req = {};
        req.serviceName = 'cn.jmicro.api.gateway.IBaseGatewayService';
        req.namespace = cons.NS_API_GATEWAY;
        req.version = '0.0.1';
        req.method = 'bestHost';
        req.args = ["nettyhttp"];

        this.callRpc(req)
            .then((data)=>{
                if(data && data.length > 0) {
                    //let jo = jm.utils.parseJson(data);
                    let arr = data.split('#');
                    if(arr[0] && arr[1] && ( arr[0] != config.ip ||  arr[1] != config.port )) {
                        config.ip = arr[0];
                        config.port = arr[1];
                        socket.reconnect();
                    }
                } else {
                    throw "API gateway host not found!";
                }
            }).catch((err)=>{
            throw err;
        })
    },

    createMsg:function(type,respType) {
        let msg = new Message();
        msg.type = type;
        msg.msgId= 0;
        //msg.reqId = 0;
        // msg.linkId = 0;

        //msg.setStream(false);
        msg.setDumpDownStream(false);
        msg.setDumpUpStream(false);
        if(respType) {
            msg.setRespType(respType)
        } else {
            msg.setRespType(cons.MSG_TYPE_PINGPONG);//默认为请求响应模式
        }
        //msg.setNeedResponse(true);
        //msg.setLoggable(false);
        msg.setMonitorable(false);
        msg.setDebugMode(false);
        msg.setLogLevel(cons.LOG_NO)//LOG_WARN
        msg.setFromWeb(true);
        msg.setRpcMk(true);
        msg.setOuterMessage(true);

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

                var req = new ApiRequest();
                req.type = Constants.LOng;
                req.clazz = Constants.MessageCls;
                req.num = 1;
                msg.payload = JSON.stringify(req);
                transport.send(msg,function(rstMsg,err){
                    if(err){
                        reje(err);
                        return;
                    }
                    if(rstMsg.payload){
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
        if(!(upProtocol == 0 || upProtocol == 1)) {
            upProtocol = Constants.PROTOCOL_JSON;
        }
        if(!(downProtocol == 0 || downProtocol == 1)) {
            downProtocol = Constants.PROTOCOL_JSON;
        }
        let self = this;
        return new Promise(function(reso,reje){
            self.callRpc0(req, null, upProtocol,downProtocol)
                .then((data)=>{
                    if(downProtocol == Constants.PROTOCOL_BIN) {
                        reso(utils.parseJson(data));
                    } else {
                        reso(data);
                    }
                })
                .catch((err)=>{
                    reje(err);
                })
        });
    },

    callRpc0(param,type,upProtocol,downProtocol){
        if(!(upProtocol == 0 || upProtocol == 1)) {
            upProtocol = Constants.PROTOCOL_JSON;
        }
        if(!(downProtocol == 0 || downProtocol == 1)) {
            downProtocol = Constants.PROTOCOL_JSON;
        }

        if(!type) {
            type = Constants.MSG_TYPE_REQ_JRPC;
        }

        let self = this;
        if(param instanceof ApiRequest) {
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

    callRpcWithParams(service,namespace,version,method,args) {
        let req = {};
        req.serviceName = service;
        req.namespace = namespace;
        req.version = version;
        req.method = method;
        req.args = args;
        return this.callRpc(req,Constants.PROTOCOL_JSON, Constants.PROTOCOL_JSON);
    },

    callRpcWithRequest(req,type,upProtocol,downProtocol){
        if(typeof type == 'undefined') {
            type = Constants.MSG_TYPE_REQ_JRPC;
        }

        if(typeof upProtocol == 'undefined') {
            upProtocol = Constants.PROTOCOL_JSON;
        }

        if(typeof downProtocol == 'undefined') {
            downProtocol = Constants.PROTOCOL_JSON;
        }

        let smsvnKey = req.serviceName +"##"+req.namespace+"##"+req.version+"########"+req.method;
        if(mk2code[smsvnKey]) {
            return this.callRpcWithTypeAndProtocol(req,type,upProtocol,downProtocol,mk2code[smsvnKey]);
        } else {
            let self = this;
            return new Promise((reso,reje)=>{

                let methodCodeReq = new ApiRequest();
                //methodCodeReq.serviceName = 'cn.jmicro.api.gateway.IBaseGatewayService';
               // methodCodeReq.method = 'fnvHash1a';
                //methodCodeReq.namespace = cons.NS_API_GATEWAY;
                //methodCodeReq.version = '0.0.1';
                methodCodeReq.args = [smsvnKey];
                //methodCodeReq.needResponse = true;

                self.callRpcWithTypeAndProtocol(methodCodeReq, type, Constants.PROTOCOL_JSON,
                    Constants.PROTOCOL_JSON,
                    mk2code[Constants.FNV_HASH_METHOD_KEY])
                    .then((methodCode)=>{
                        mk2code[smsvnKey] = methodCode;
                        self.callRpcWithTypeAndProtocol(req,type,upProtocol,downProtocol,methodCode)
                            .then((resp)=>{
                                reso(resp);
                            })
                            .catch((err)=>{
                                reje(err);
                            })
                    })
                    .catch((err)=>{
                        reje(err);
                    })
            });
        }
    },

    callRpcWithTypeAndProtocol(req,type,upProtocol,downProtocol,methodCode){
        let self = this;
        return new Promise(function(reso,reje){

            let msg =  self.createMsg(type);
            msg.setUpProtocol(upProtocol);
            msg.setDownProtocol(downProtocol);

            msg.setRpcMk(true);
            msg.setSmKeyCode(methodCode);
            msg.setForce2Json(true);
            if(config.includeMethod) {
                msg.putExtra(Constants.EXTRA_KEY_METHOD,req.method,Constants.PREFIX_TYPE_STRING);
            }

            //console.log(req.method+" => " + methodCode);

            if(!req.reqId) {
                req.reqId = reqId++;
            }
            msg.setMsgId(req.reqId);

            if(self.actInfo) {
                req.params['loginKey'] = self.actInfo.loginKey;
            }


            req.serviceName = null;
            req.namespace = null;
            req.version = null;
            req.method = null;

            if(upProtocol == Constants.PROTOCOL_JSON) {
                msg.payload =  utils.toUTF8Array(JSON.stringify(req));
            } else if( upProtocol == Constants.PROTOCOL_BIN ){
                if(typeof req.encode == 'function') {
                    msg.payload = req.encode(Constants.PROTOCOL_BIN);
                }
            } else {
                msg.payload = req;
            }

            if(req.needResponse) {
                msg.setRespType(Constants.MSG_TYPE_PINGPONG);
            }
            transport.send(msg,function(rstMsg,err){
                if(err || !rstMsg.payload.success) {
                    console.log(rstMsg.payload);
                    let rst = rstMsg.payload.result
                    let doFailure = true;
                    if(rst && rst.errorCode != 0) {
                        //alert(rst.msg);
                        if(rst.errorCode == 0x00000004 || rst.errorCode == 0x004C || rst.errorCode == 76) {
                            let actName = localStorage.get("actName");
                            let rememberPwd = localStorage.get("rememberPwd");
                            if(rememberPwd || !actName || actName.startWith("guest_")) {
                                let pwd = localStorage.get("pwd");
                                if(!pwd) {
                                    pwd="";
                                }
                                self.actInfo = null;
                               self.login(actName,pwd,(actInfo,err)=>{
                                    if( self.actInfo && !err) {
                                        self.callRpcWithTypeAndProtocol(req,type,upProtocol,downProtocol,methodCode)
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
                        // reje(err || rst);
                        self.doReject(reje, err || rst);
                    }

                } else {
                    let rst = rstMsg.payload.result;
                    if(rst != null && Object.prototype.hasOwnProperty.call(rst,'errorCode')
                        && Object.prototype.hasOwnProperty.call(rst,'msg') ) {
                        //reje(rst);
                        self.doReject(reje,rstMsg);
                    }else {
                        reso(rst);
                    }
                }
            });
        });
    },

    doReject : function(reje,rst) {
        if(rst && Object.prototype.hasOwnProperty.call(rst,'errorCode')) {
            if(errCode2Msg[rst.errorCode]) {
                reje(errCode2Msg[rst.errorCode]);
            }else {
                reje(rst.msg);
            }
        }else {
            reje(rst);
        }
    },

    callWithObject(params,type,upProtocol,downProtocol){
        let self = this;
        return new Promise((reso,reje)=>{
            if(!params.serviceName) {
                reje('service name cannot be NULL');
                return;
            }

            if(!params.method) {
                reje( 'method name cannot be NULL');
                return;
            }

            if(!params.namespace) {
                params.namespace = cons.DEFAULT_NAMESPACE;
            }

            if(!params.version) {
                params.version = cons.DEFAULT_VERSION;
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

            var req = new ApiRequest();
            req.serviceName = params.serviceName;
            req.method = params.method;
            req.namespace = params.namespace;
            req.version = params.version;
            req.args = params.args;

            req.needResponse = params.needResponse;
            //req.stream = params.stream;

            self.callRpcWithRequest(req,type,upProtocol,downProtocol)
                .then((rst)=>{
                    reso(rst);
                }).catch((err)=>{
                reje(err);
            });

        });

    },

    callWithParams (serviceName, namespace, version, method, args, needResponse){
        let self = this;
        return new Promise((reso,reje)=>{

            if(!serviceName || serviceName.trim() == '') {
                reje('service name cannot be NULL');
                return;
            }

            if(!method || method.trim() == '') {
                reje( 'method name cannot be NULL');
                return;
            }

            if(!namespace  || namespace.trim() == '') {
                namespace = cons.DEFAULT_NAMESPACE;
            }

            if(!version || version.trim() == '') {
                version = cons.DEFAULT_VERSION;
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

            let req = new ApiRequest();
            req.serviceName = serviceName;
            req.method = method;
            req.namespace = namespace;
            req.version = version;
            req.args = args;
            req.needResponse = needResponse;

            self.callRpcWithRequest(req)
                .then((rst)=>{
                    reso(rst);
                }).catch((err)=>{
                reje(err);
            });

        });

    }
}