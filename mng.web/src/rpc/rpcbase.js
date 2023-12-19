/* eslint-disable */
import ls from './localStorage'
import ApiRequest from './request'
import cons from './constants'
import config from './config'
import utils from './utils'
import i18n from './i18n'
import { Message, Constants } from './message'
import transport from './transport' //import socket from "./socket"
//let idCache = {}
import auth from './auth'
import ps from './pubsub'
import prf from './profile.js'

const fnvCode = -655376287
const FNV_32_PRIME = 16777619

const MINUTE_IN_MS = 1*60*1000

let mk2code = {
    'cn.jmicro.api.gateway.IBaseGatewayServiceJMSrv##apigateway##0.0.1##############1##fnvHash1a': fnvCode
}

/*let errCode2Msg = {
    0x06 : "Service not available maybe not started",
}*/

let msgId = 1

export default {
	MINUTE_IN_MS,
    config,
	mcodes:{
		"getCode":-1494008743,
		"login":-880431443,
		"logout":1105614483,
		"fnvCode":fnvCode
	},//自定义KEY到方法编码映射
	VER_DATA_KEY:Constants.VER_DATA_KEY,
	
    creq(sn, ns, v, method, args) {
        let req = {}
        req.serviceName = sn
        req.namespace = ns
        req.version = v
        req.args = args
        req.method = method
        return req
    },
	
    cmreq(mcode, args) {
        let req = {}
        req.mcode = mcode
        req.args = args
        return req
    },
	
    init: async function (opts, cb) {
        //ip,port,actName,pwd,mod
        let useWs = opts.useWs ? opts.useWs : config.useWs
        config.useWs = useWs
		
        if (useWs) {
            if ((window && window.WebSocket) || utils.isUni()) {
                /*import('./socket.js')
				.then(so=>{
					so.init()
				})*/
                //let socket = require('./socket.js')
            } else {
                config.useWs = false
            }
        }

        if(opts.clientId) {
            config.clientId = opts.clientId
        }

        if (opts.mod) {
            config.mod = opts.mod
        }

        if(config.sslEnable) {
            require('./security').then((so) => {
                so.init()
            })
        }

        if (opts.ip && opts.ip.length > 0) {
            config.ip = opts.ip
        }

        if (opts.port && opts.port > 0) {
            config.port = opts.port
        }

        if (opts.actName && opts.actName.length > 0) {
            ls.set(Constants.ACT_NAME_KEY, opts.actName)
        }

        if(opts.pwd && opts.pwd.length > 0) {
            ls.set(Constants.ACT_PWD_KEY, opts.pwd)
        }

        let req = {}
        req.args = ['nettyhttp']
        req.mcode = -1318264465//fnv服务
		
		//获取版本信息
		let rst = await prf.init()
		console.log("profile return: ",rst)
		
		//检测登录Token是否有效
		rst = await i18n.init()
		console.log("i18n return: ",rst)
		
		rst = await auth.init(opts)
		console.log("auth return: ",rst)
		
		if(rst.code == 0) {
			ps.init()
		}
		
		if(cb) {
			console.log("rpcbase callback: ",rst.code == 0)
			cb(rst.code == 0)
		}
		console.log("rpcbase finish: ",config)
    },
	
    createMsg: function (type, respType) {
        let msg = new Message()
        msg.type = type
		
        msg.msgId = msgId++
        // msg.linkId = 0
        //msg.setStream(false)

        msg.setDumpDownStream(false)
        msg.setDumpUpStream(false)

        if (respType) {
            msg.setRespType(respType)
        } else {
            msg.setRespType(cons.MSG_TYPE_PINGPONG) //默认为请求响应模式
        } //msg.setNeedResponse(true)
        //msg.setLoggable(false)

        msg.setMonitorable(false)
        msg.setDebugMode(false)
        msg.setLogLevel(cons.LOG_NO)//LOG_WARN

        msg.setFromWeb(true)
        msg.setRpcMk(true)
        msg.setOuterMessage(true)
        return msg
    },
	
    getId: function (idClazz) {
        let that = this
        return new Promise(function (reso1, reje) {
            var cacheId = that.idCache[idClazz]

            if (!!cacheId && cacheId.curIndex < cacheId.ids.length) {
                reso1(cacheId.ids[cacheId.curIndex++])
            } else {
                if (!cacheId) {
                    cacheId = {
                        ids: [],
                        curIndex: 0
                    }
                    that.idCache[idClazz] = cacheId
                }

                var msg = that.createMsg(11)
                var req = new ApiRequest()
                req.type = Constants.LOng
                req.clazz = Constants.MessageCls
                req.num = 1
                msg.payload = JSON.stringify(req)
                transport.send(msg, function (rstMsg, err) {
                    if (err) {
                        reje(err)
                        return
                    }

                    if (rstMsg.payload) {
                        cacheId.ids = rstMsg.payload
                        cacheId.index = 0
                        var i = cacheId.ids[cacheId.index++]
                        reso1(i)
                    } else {
                        reje(rstMsg)
                    }
                })
            }
        })
    },
	
    callRpcWithParams(sn, ns, v, method, args, upProtocol, downProtocol) {
        let req = this.creq(sn, ns, v, method, args)
        return this.callRpc(req, upProtocol, downProtocol)
    },
	
	invokeByCode(mcode,args,upProtocol, downProtocol) {
		console.log(mcode)
		if(!args) {
			args = []
		}
		if(typeof mcode == "string") {
			mcode = this.mcodes[mcode]
		}
		const req = this.cmreq(mcode, args)
		return this.callRpc(req,upProtocol, downProtocol)
	},
	
    async callRpc(req, upProtocol, downProtocol) {
        if (typeof upProtocol == 'undefined' || upProtocol == null) {
            upProtocol = Constants.PROTOCOL_JSON
        }

        if (typeof downProtocol == 'undefined' || downProtocol == null) {
            downProtocol = Constants.PROTOCOL_JSON
        }
        /*if(req.method == 'serverList') {
			  console.log(req.method)
		  }*/

        if (req.mcode) {
            return await this.__callRpcWithTypeAndProtocol(req, upProtocol, downProtocol, req.mcode)
        } else {
            let cid = req.clientId ? req.clientId : config.clientId
            let smSvnKey = req.serviceName + '##' + req.namespace + '##' + req.version + '##############' + cid + '##' + req.method

            if (mk2code[smSvnKey]) {
                return await this.__callRpcWithTypeAndProtocol(req, upProtocol, downProtocol, mk2code[smSvnKey])
            } else {
                let self = this
                let methodCodeReq = new ApiRequest()
                methodCodeReq.method = 'fnvHash1a'
                methodCodeReq.args = [smSvnKey]
                methodCodeReq.type = Constants.MSG_TYPE_REQ_JRPC
                
                let res = await self.__callRpcWithTypeAndProtocol(methodCodeReq, Constants.PROTOCOL_JSON, Constants.PROTOCOL_JSON, fnvCode)
				if(res.code == 0) {
					if('getUserCoupon' == req.method) {
						console.log(smSvnKey)
					}
					console.log(res.data+" : "+smSvnKey)
					mk2code[smSvnKey] = res.data
					return await self.__callRpcWithTypeAndProtocol(req, upProtocol, downProtocol, mk2code[smSvnKey])
				} else {
					return {code:1,msg:"Method not found: " + smSvnKey}
				}
            }
        }
    },
	
    argHash(args) {
        let h = 0 //无参数或参数都为空时

        if (args != null && args.length > 0) {
            for (let i = 0; i < args.length; i++) {
                let a = args[i]

                if (a != null) {
                    //只有非空字段才参数hash
                    h ^= a.hashCode()
                    h *= FNV_32_PRIME
                }
            }
        }

        return h
    },
	
    async __callRpcWithTypeAndProtocol(req, upProtocol, downProtocol, methodCode) {
        let self = this
        if(typeof req.type == 'undefined' || req.type == null) {
            req.type = Constants.MSG_TYPE_REQ_JRPC
        }
		
		if(methodCode == 1) {
			console.log("MCode error: ",req)
			throw "MCode error 1: "+ JSON.stringify(req)
		}
        
        let msg = self.createMsg(req.type)
        msg.setUpProtocol(upProtocol)
        msg.setDownProtocol(downProtocol)
        msg.setRpcMk(true)
        msg.setSmKeyCode(methodCode)
        msg.setForce2Json(true)
        
        if(config.includeMethod && req.method) {
            msg.putExtra(Constants.EXTRA_KEY_METHOD, req.method, Constants.PREFIX_TYPE_STRINGG)
        }
		
		msg.putExtra(Constants.EXTRA_KEY_CLIENT_ID, config.clientId, Constants.PREFIX_TYPE_INT)

        if(!req.params) {
            req.params = {}
        }
        
        if (auth.isLogin()) {
            msg.putExtra(Constants.EXTRA_KEY_LOGIN_KEY, auth.actInfo.loginKey, Constants.PREFIX_TYPE_STRINGG)
        }
        
        let mn = req.method
        req.serviceName = null
        req.namespace = null
        req.version = null
        req.method = null
        req.type = null
        
        if (req.args == null || typeof req.args == 'undefined') {
            req.args = []
        }
        
        if (upProtocol == Constants.PROTOCOL_JSON) {
            msg.payload = utils.toUTF8Array(JSON.stringify(req))
        } else {
            if(upProtocol == Constants.PROTOCOL_BIN) {
                let r = req
                if (!(r instanceof ApiRequest)) {
                    r = new ApiRequest()
                    for (let k in req) {
                        r[k] = req[k]
                    }
                }
                msg.payload = r.encode(Constants.PROTOCOL_BIN)
            } else {
                msg.payload = req
            }
        }
        
        //if(req.needResponse) {
            msg.setRespType(Constants.MSG_TYPE_PINGPONG)
        //}
        
		//console.log(msg)
        let rstMsg = await transport.send(msg)
		
		if (rstMsg.getDownProtocol() == Constants.PROTOCOL_BIN) {
		    let resp = new ApiResponse();
		    resp.decode(rstMsg.payload, rstMsg.getDownProtocol());
		    rstMsg.payload = resp;
		}
		
		// if (!req.needResponse) {
		//     return
		// }
        
        if(!rstMsg) {
			console.log(req)
			console.log(msg)
            throw 'No response msg: '
        }
        
        if (!rstMsg.payload) {
			console.log(req)
			console.log(msg)
			console.log("response msg: ",rstMsg)
			throw 'No payload'
        }
        
        if (!rstMsg.isError()) {
           let rst = rstMsg.payload //console.log(rst)
           if (rst && rst.code != 0) {
               //reje(rst)
               return self.doReject(rst)
           } else {
               return rst
           }
        } 
		
		console.log('Method: ' + (typeof mn=='undefined'?'':mn ) + ', mcode: ' + methodCode + ',' + JSON.stringify(rstMsg.payload))
		let rst = rstMsg.payload
		if (rst && rst.code != 0) {
			//alert(rst.msg)
			if (rst.code == 76) {
				auth.unsetActInfo()
				//做自动登录
				let autoLogin = ls.get(Constants.ACT_AUTO_LOGIN_KEY)
				if(autoLogin && (autoLogin==true || autoLogin == 'true')) {
					let actInfo = await auth.login(null, null, null, 0,true)
					if (actInfo) {
						return await self.__callRpcWithTypeAndProtocol(req, req.type, upProtocol, downProtocol, methodCode,false) 
					}else{
						return rst
					}
				} else {
					return rst
				}
			} else {
				return rst
			}
		}

		// reje(err || rst)
		return self.doReject(rst)
		
    },
	
	async sendMessage(params, msgType, headerParams, upProtocol, downProtocol, notLogin) {
	    let self = this
	    if (!msgType) {
	       throw "message type cannot be null"
	    }
	    
		if (typeof upProtocol == 'undefined' || upProtocol == null) {
		    upProtocol = Constants.PROTOCOL_JSON
		}
		
		if(typeof downProtocol == 'undefined' || downProtocol == null) {
		    downProtocol = Constants.PROTOCOL_JSON
		}
		
	    let msg = self.createMsg(msgType)
	    msg.setUpProtocol(upProtocol)
	    msg.setDownProtocol(downProtocol)
	    msg.setRpcMk(false)
		//非RPC调用
	    //msg.setSmKeyCode("")
	    msg.setForce2Json(false)
		
		if(headerParams && headerParams.length > 0) {
			headerParams.forEach(e=>{
				msg.putExtra(e.k, e.v, e.t)
			})
		}
		
		msg.putExtra(Constants.EXTRA_KEY_CLIENT_ID, config.clientId, Constants.PREFIX_TYPE_INT)
	    
	    if(!params) {
	        params = {}
	    }
	    
	    if (auth.isLogin()) {
	        msg.putExtra(Constants.EXTRA_KEY_LOGIN_KEY, auth.actInfo.loginKey, Constants.PREFIX_TYPE_STRINGG)
	    }
	    
	    if (upProtocol == Constants.PROTOCOL_JSON) {
	        msg.payload = utils.toUTF8Array(JSON.stringify(params))
	    } else {
			 msg.payload = params
	    }
	    
	    //if(req.needResponse) {
	    //msg.setRespType(Constants.MSG_TYPE_PINGPONG)
	    //}
	    
	    let rstMsg = await transport.send(msg)
	    
	    if(!rstMsg) {
			console.log(msg)
	        throw 'No response msg: '
	    }
		
	    /*
	    if (!rstMsg.payload) {
			console.log(msg)
			console.log("response msg: ",rstMsg)
			throw 'No payload'
	    }
	    */
	   
	    if (!rstMsg.isError()) {
	       return rstMsg.payload //console.log(rst)
	    } 
		
		//console.log('Method: ' + mn + ', mcode: ' + methodCode + ',' + JSON.stringify(rstMsg.payload))
		let rst = rstMsg.payload
		if (rst && typeof rst.code !='undefined' && rst.code != 0) {
			//alert(rst.msg)
			if (rst.code == 76 && !notLogin) {
				auth.unsetActInfo()
				//做自动登录
				let autoLogin = ls.get(Constants.ACT_AUTO_LOGIN_KEY)
				if(autoLogin && (autoLogin==true || autoLogin == 'true')) {
					let actInfo = await auth.login(null, null, null, 0,true)
					if (actInfo) {
						return await self.sendMessage(params, msgType, headerParams, upProtocol, downProtocol, true) 
					}else {
						return rst
					}
				} else {
					return rst
				}
			} else {
				return rst
			}
		}
		// reje(err || rst)
		return self.doReject(rst)
		
	},
	
    doReject: function (rst) {
        if (rst && Object.prototype.hasOwnProperty.call(rst, 'code')) {
            /*if(errCode2Msg[rst.code]) {
				reje(errCode2Msg[rst.code])
			}else {
				reje(rst)
			}*/
            return rst
        } else {
            return rst
        }
    }
}
