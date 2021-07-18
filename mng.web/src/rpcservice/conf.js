import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {
    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  getChildren : function (path,all){
      return rpc.callRpc(this.__actreq('getChildren', [path,all]));
  },

  update: function (path,val){
      return rpc.callRpc(this.__actreq('update', [path,val]))
  },

  delete: function (path){
      return rpc.callRpc(this.__actreq('delete', [path]))
},

  add: function (path,val,isDir){
      return rpc.callRpc(this.__actreq('add', [path,val,isDir]))
  },

  sn:'cn.jmicro.api.mng.IConfigManagerJMSrv',
  ns: cons.NS_MNG,
  v:'0.0.1',
}

