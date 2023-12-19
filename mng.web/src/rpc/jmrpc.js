import rpc from "./rpcbase.js"
import utils from "./utils.js"
import lc from "./localStorage.js"
import i18n from "./i18n.js"
import cons0 from "./constants.js"
import config from "./config.js"
import {Constants} from "./message.js"
import ps from "./pubsub.js"
import auth from "./auth.js"

let cons = {}
//cons = Object.assign(cons,cons0)


for(let key in Constants) {
	cons[key] = Constants[key]
}

for(let key in cons0) {
	cons[key] = cons0[key]
}


(function(g){
	g = g || window
	g.$jr = {
		rpc,
		utils,
		lc,
		i18n,
		cons,
		config,
		ps,
		auth,
		
		/*
		creq: function(sn, ns, v, method, args) {
			return rpc.creq(sn, ns, v, method, args)
		},
		
		cmreq: function(mcode, args) {
			return rpc.cmreq(mcode, args)
		},
		
		init: function (opts, cb) {
			return rpc.init(opts, cb)
		},
		
		createMsg: function (type, respType) {
			return rpc.createMsg(type, respType)
		},
		
		callRpcWithParams: function(sn, ns, v, method, args, upProtocol, downProtocol) {
			return rpc.callRpcWithParams(sn, ns, v, method, args, upProtocol, downProtocol)
		},
		
		invokeByCode: function(mcode,args,upProtocol, downProtocol) {
			return rpc.invokeByCode(mcode,args,upProtocol, downProtocol)
		},
		
		callRpc: function(req, upProtocol, downProtocol) {
			return rpc.callRpc(req, upProtocol, downProtocol)
		},
		
		sendMessage: function(params, msgType, headerParams, upProtocol, downProtocol, notLogin) {
			return rpc.sendMessage(params, msgType, headerParams, upProtocol, downProtocol, notLogin)
		},
		*/
	}
})(window)