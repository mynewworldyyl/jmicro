import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {
    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  getKvs : function() {
      return rpc.callRpc(this.__actreq( 'getKvs', []))
  },

  getModuleList : function () {
      return rpc.callRpc(this.__actreq( 'getModuleList', []))
  },

  getModuleKvs : function (module) {
      return rpc.callRpc(this.__actreq( 'getModuleKvs', [module]))
  },

  updateKv : function (module,kv) {
      return rpc.callRpc(this.__actreq( 'updateKv', [module,kv]))
  },

  sn:'cn.jmicro.mng.api.IProfileServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}

