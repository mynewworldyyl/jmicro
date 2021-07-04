import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {
  getChildren : function (path,all){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getChildren',[path,all]);
  },

  update: function (path,val){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'update',[path,val]);
  },

  delete: function (path){
  return rpc.callRpcWithParams(this.sn,this.ns,this.v,'delete',[path]);
},

  add: function (path,val,isDir){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'add',[path,val,isDir]);
  },

  sn:'cn.jmicro.api.mng.IConfigManager',
  ns: cons.NS_MNG,
  v:'0.0.1',
}

