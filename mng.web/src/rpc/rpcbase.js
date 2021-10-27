import localStorage from "./localStorage";
import ApiRequest from "./request";
import cons from "./constants";
import config from "./config"
import utils from "./utils"

import i18n from './i18n'
import lc from "./localStorage"
import {Message,Constants} from "./message"
import transport from "./transport"

//import socket from "./socket";

//let idCache = {};
let actListeners = {};

const fnvCode = -655376287
const FNV_32_PRIME = 0x01000193

let mk2code = {
    "cn.jmicro.api.gateway.IBaseGatewayServiceJMSrv##apigateway##0.0.1##############1##fnvHash1a":fnvCode,
};

/*let errCode2Msg = {
    0x06 : "Service not available maybe not started",
};*/

let reqId = 1

let doLoginOutBefore = false

function __actreq(method,args){
    let req = {};
    req.serviceName = 'cn.jmicro.security.api.IAccountServiceJMSrv';
    req.namespace = cons.NS_SECURITY;
    req.version = '0.0.1';
    req.args = args;
    req.method = method;
    return req;
}

export default {
     config,
     actInfo:null,

    creq(sn,ns,v,method,args){
        let req = {};
        req.serviceName = sn;
        req.namespace = ns;
        req.version = v;
        req.args = args;
        req.method = method;
        return req;
    },

    cmreq(mcode,args){
        let req = {};
        req.mcode = mcode;
        req.args = args;
        return req;
    },

    addActListener (key,l) {
        /*if(!!this.actListeners[key]) {
            throw 'Exist listener: ' + key;
        }*/
        actListeners[key] = l;
    },

    getCode(type) {
        let req = this.cmreq(-1494008743,[type]) //__actreq('getCode',[type]);
        return this.callRpc(req)
    },

    removeActListener(key) {
        if(!actListeners[key]) {
            return;
        }
        delete actListeners[key];
    },

    isLogin() {
         if(!this.actInfo){
             this.actInfo = lc.get(Constants.USER_INFO);
         }
         return this.actInfo != null && this.actInfo != '';
    },

    isAdmin(){
        return this.isLogin()  && this.actInfo.isAdmin;
    },

    setActInfo(actInfo) {
         if(!actInfo) {
            throw 'invalid act info'
         }
        doLoginOutBefore = false
        this.actInfo = actInfo;
        lc.set(Constants.USER_INFO,actInfo)
        this._notify(Constants.LOGIN)
    },

    unsetActInfo() {
        doLoginOutBefore = true
        this.actInfo = null;
        lc.remove(Constants.USER_INFO)
        this._notify(Constants.LOGOUT)
    },

    login(actName,pwd,vcode,vcodeId,cb){
         console.log('not support login, you need regist login method')
    },

    _notify : function(type) {
        for(let key in actListeners) {
            if(actListeners[key]) {
                actListeners[key](type,this.actInfo)
            }
        }
    },

    logout: function (cb){
        doLoginOutBefore = true
        if(!this.actInfo) {
            if(cb) {
                cb(true,null)
            }
            return;
        }
        let self = this;
        self.callRpc(__actreq('logout',[]))
            .then(( resp )=>{
                if(resp.data) {
                    if(cb) {
                        cb(true,null)
                    }
                    self.unsetActInfo()
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

    checkLogin() {
         let self = this
         return new Promise((reso,reje)=>{
             if(!this.isLogin()) {
                 reje()
             }else {
                 let req =  self.cmreq(-515329030,[self.actInfo.loginKey])
                 self.callRpc(req)
                 .then((res)=>{
                    if(res.data) {
                        reso()
                    } else {
                        doLoginOutBefore = true
                        self.actInfo = null;
                        lc.remove(Constants.USER_INFO)
                        reje()
                    }
                 }).catch(err=>{
                     doLoginOutBefore = true
                     self.actInfo = null;
                     lc.remove(Constants.USER_INFO)
                     reje()
                 })
             }
         })
    },

    init : function(opts,cb){
         //ip,port,actName,pwd,mod
        let useWs = opts.useWs ? opts.useWs : config.useWs

        if(useWs){
            if(window && window.WebSocket || utils.isWx()) {
            /*    import('./socket.js')
                    .then(so=>{
                        so.init()
                    })*/
                //let socket = require('./socket.js')
            }else {
                config.useWs = false
            }
        }

        if(opts.clientId) {
            config.clientId = opts.clientId
        }

        if(opts.mod) {
            config.mod = opts.mod
        }

        if(config.sslEnable) {
            import('./security')
                .then(so=>{
                so.init()
            });
        }

        if( opts.ip &&  opts.ip.length > 0) {
            config.ip =  opts.ip;
        }

        if( opts.port &&  opts.port > 0) {
            config.port =  opts.port;
        }

        if( opts.actName &&  opts.actName.length > 0) {
            localStorage.set(Constants.ACT_NAME_KEY, opts.actName);
        }

        if( opts.pwd &&  opts.pwd.length > 0) {
            localStorage.set(Constants.ACT_PWD_KEY, opts.pwd);
        }

        let req = {};
        req.args = ["nettyhttp"];
        req.mcode=-1318264465

         i18n.init()
        //检测登录Token是否有效
        this.checkLogin()
            .then(()=>{
                if(cb) {
                    cb(true)
                }
            }).catch(()=>{
                if(cb) {
                    cb(false)
                }
            })

       /* this.callRpc(req)
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
        })*/
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


    callRpcWithParams(sn,ns,v,method,args,upProtocol,downProtocol){
        let req = this.creq(sn,ns,v,method,args);
        return this.callRpc(req,upProtocol,downProtocol)
    },

    callRpc(req,upProtocol,downProtocol){

        if(typeof upProtocol == 'undefined'  || upProtocol == null) {
            upProtocol = Constants.PROTOCOL_JSON;
        }

        if(typeof downProtocol == 'undefined'  || downProtocol == null) {
            downProtocol = Constants.PROTOCOL_JSON;
        }

        /*if(req.method == 'serverList') {
            console.log(req.method);
        }*/

        if(req.mcode) {
            return this.__callRpcWithTypeAndProtocol(req, upProtocol, downProtocol, req.mcode);
        }else {
            let cid = req.clientId ? req.clientId : config.clientId
            let smSvnKey = req.serviceName +"##"+req.namespace+"##"+req.version+"##############"+cid+"##"+req.method;
            if( mk2code[smSvnKey] ) {
                return this.__callRpcWithTypeAndProtocol(req,upProtocol,downProtocol,mk2code[smSvnKey]);
            } else {
                let self = this;
                return new Promise((reso,reje)=>{
                    let methodCodeReq = new ApiRequest();
                    methodCodeReq.method = 'fnvHash1a';
                    methodCodeReq.args = [smSvnKey];
                    methodCodeReq.type = Constants.MSG_TYPE_REQ_JRPC;
                    self.__callRpcWithTypeAndProtocol( methodCodeReq, Constants.PROTOCOL_JSON,
                        Constants.PROTOCOL_JSON, fnvCode )
                        .then((res)=>{
                            mk2code[smSvnKey] = res.data;
                            self.__callRpcWithTypeAndProtocol(req,upProtocol,downProtocol,mk2code[smSvnKey])
                                .then((resp)=>{
                                    reso(resp);
                                }).catch((err)=>{
                                    reje(err);
                                })
                        }).catch((err)=>{
                            reje(err);
                        })
                });
            }
        }
    },

    argHash( args) {
        let h = 0;//无参数或参数都为空时
        if(args != null && args.length > 0) {
            for(let i = 0; i < args.length; i++) {
                let a = args[i]
                if( a != null ) {
                    //只有非空字段才参数hash
                    h ^=  a.hashCode()
                    h *= FNV_32_PRIME
                }
            }
        }
        return h;
    },

    __callRpcWithTypeAndProtocol(req, upProtocol, downProtocol, methodCode){
        let self = this;
        return new Promise(function(reso,reje){

            if(typeof req.type == 'undefined' || req.type == null) {
                req.type = Constants.MSG_TYPE_REQ_JRPC
            }

            let msg =  self.createMsg(req.type)
            msg.setUpProtocol(upProtocol)
            msg.setDownProtocol(downProtocol)

            msg.setRpcMk(true)
            msg.setSmKeyCode(methodCode)
            msg.setForce2Json(true)
            if(config.includeMethod && req.method) {
                msg.putExtra(Constants.EXTRA_KEY_METHOD,req.method, Constants.PREFIX_TYPE_STRING)
            }

            //console.log(req.method+" => " + methodCode);

            req.reqId = reqId++
            msg.setMsgId(req.reqId)

            if(!req.params) {
                req.params = {}
            }

            if(self.actInfo) {
                //req.params[Constants.TOKEN] = self.actInfo.loginKey
                msg.putExtra(Constants.EXTRA_KEY_LOGIN_KEY,self.actInfo.loginKey,Constants.PREFIX_TYPE_STRING)
            }

            let mn = req.method

            req.serviceName = null
            req.namespace = null
            req.version = null
            req.method = null
            req.type = null

            if(req.args == null || typeof req.args =='undefined') {
                req.args = []
            }

            if(upProtocol == Constants.PROTOCOL_JSON) {
                msg.payload =  utils.toUTF8Array(JSON.stringify(req))
            } else if( upProtocol == Constants.PROTOCOL_BIN ){
                let r = req
                if(!(r instanceof ApiRequest)) {
                    r = new ApiRequest()
                    for(let k in req) {
                        r[k] = req[k]
                    }
                }
                msg.payload = r.encode(Constants.PROTOCOL_BIN)
            } else {
                msg.payload = req
            }

            if(req.needResponse) {
                msg.setRespType(Constants.MSG_TYPE_PINGPONG)
            }
            transport.send(msg,function(rstMsg,err){
                if(!rstMsg || err) {
                    reje(err)
                    return
                }
                if(!rstMsg.payload) {
                    reje(rstMsg)
                    return
                }
                if(rstMsg.isError()) {
                    console.log('Method: ' + mn + ", mcode: " + methodCode + ',' + JSON.stringify(rstMsg.payload))
                    let rst = rstMsg.payload
                    let doFailure = true
                    if(rst && rst.code != 0) {
                        //alert(rst.msg)
                        if(!doLoginOutBefore && (rst.code == 0x004C || rst.code == 76)) {
                            self.actInfo = null
                            self.login(null,null,null,0,(actInfo,err)=>{//做自动登录
                                if(self.actInfo && !err) {
                                    self.__callRpcWithTypeAndProtocol(req,req.type,upProtocol,downProtocol,methodCode)
                                        .then(( r,err )=>{
                                            if(r ) {
                                                reso(r)
                                            } else {
                                                reje(err)
                                            }
                                        }).catch((err)=>{
                                        console.log(err)
                                        reje(err)
                                    })
                                }else {
                                    reje(err || rst)
                                }
                            })
                            doFailure = false;
                        }else if(rst.code == 4) {
                            //需要验证码
                            reje(rst)
                            return
                        }
                    }

                    if(doFailure) {
                        // reje(err || rst);
                        self.doReject(reje, err || rst);
                    }
                } else {
                    let rst = rstMsg.payload;
                    //console.log(rst)
                    if(rst && rst.code != 0) {
                        //reje(rst);
                        self.doReject(reje,rst);
                    }else {
                        reso(rst);
                    }
                }
            });
        });
    },

    doReject : function(reje,rst) {
        if(rst && Object.prototype.hasOwnProperty.call(rst,'code')) {
            /*if(errCode2Msg[rst.code]) {
                reje(errCode2Msg[rst.code]);
            }else {
                reje(rst);
            }*/
            reje(rst);
        }else {
            reje(rst);
        }
    },
}