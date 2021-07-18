import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {
    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  getServices: function (all){
      return rpc.callRpc(this.__actreq( 'getServices', [all]))
  },

  updateItem: function (si){
      return rpc.callRpc(this.__actreq( 'updateItem', [si]))
  },

  updateMethod: function (method){
      return rpc.callRpc(this.__actreq( 'updateMethod', [method]))
  },

  sn:'cn.jmicro.api.mng.IManageServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
