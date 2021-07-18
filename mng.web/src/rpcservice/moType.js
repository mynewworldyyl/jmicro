import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default{
    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },
  getAllConfigs: function (){
      return rpc.callRpc(this.__actreq( 'getAllConfigs', []))
  },

  update: function (mcConfig){
      return rpc.callRpc(this.__actreq( 'update', [mcConfig]))
  },

  delete: function (mcConfig){
      return rpc.callRpc(this.__actreq( 'delete', [mcConfig]))
  },

  add: function (mcConfig){
      return rpc.callRpc(this.__actreq( 'add', [mcConfig]))
  },

  getConfigByMonitorKey: function (key){
      return rpc.callRpc(this.__actreq( 'getConfigByMonitorKey', [key]))
  },

  updateMonitorTypes: function (key,adds,dels){
      return rpc.callRpc(this.__actreq( 'updateMonitorTypes', [key,adds,dels]))
  },

  getMonitorKeyList: function (){
      return rpc.callRpc(this.__actreq( 'getMonitorKeyList', []))
  },

  getConfigByServiceMethodKey: function (key){
      return rpc.callRpc(this.__actreq( 'getConfigByServiceMethodKey', [key]))
  },

  updateServiceMethodMonitorTypes: function (key,adds,dels){
      return rpc.callRpc(this.__actreq( 'updateServiceMethodMonitorTypes', [key,adds,dels]))
  },

  getAllConfigsByGroup: function (groups){
      return rpc.callRpc(this.__actreq( 'getAllConfigsByGroup', [groups]))
  },

  addNamedTypes: function (name){
      return rpc.callRpc(this.__actreq( 'addNamedTypes', [name]))
  },

  getTypesByNamed: function (name){
      return rpc.callRpc(this.__actreq( 'getTypesByNamed', [name]))
  },

  updateNamedTypes: function (key,adds,dels){
      return rpc.callRpc(this.__actreq( 'updateNamedTypes', [key,adds,dels]))
  },

  getNamedList: function (){
      return rpc.callRpc(this.__actreq( 'getNamedList', []))
  },

  sn:'cn.jmicro.api.mng.IMonitorTypeServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
