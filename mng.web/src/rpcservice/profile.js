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

  getModuleKvs : function (md) {
      return rpc.callRpc(this.__actreq( 'getModuleKvs', [md]))
  },

  updateKv : function (md,kv) {
     return rpc.callRpc(this.__actreq( 'updateKv', [md,kv]))
  },
  
  addKv : function (scid,md,key,val,type) {
      return rpc.callRpc(this.__actreq('addKv', [scid,md,key,val,type]))
  },
  
  addModule : function (scid,md) {
      return rpc.callRpc(this.__actreq('addModule', [scid,md]))
  },

  sn:'cn.jmicro.mng.api.IProfileServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}

