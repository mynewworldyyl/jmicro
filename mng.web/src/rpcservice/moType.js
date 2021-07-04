import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default{

  getAllConfigs: function (){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getAllConfigs',[]);
  },

  update: function (mcConfig){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'update',[mcConfig]);
  },

  delete: function (mcConfig){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'delete',[mcConfig]);
  },

  add: function (mcConfig){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'add',[mcConfig]);
  },

  getConfigByMonitorKey: function (key){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getConfigByMonitorKey',[key]);
  },

  updateMonitorTypes: function (key,adds,dels){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateMonitorTypes',[key,adds,dels]);
  },

  getMonitorKeyList: function (){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getMonitorKeyList',[]);
  },

  getConfigByServiceMethodKey: function (key){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getConfigByServiceMethodKey',[key]);
  },

  updateServiceMethodMonitorTypes: function (key,adds,dels){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateServiceMethodMonitorTypes',[key,adds,dels]);
  },

  getAllConfigsByGroup: function (groups){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getAllConfigsByGroup',[groups]);
  },

  addNamedTypes: function (name){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'addNamedTypes',[name]);
  },

  getTypesByNamed: function (name){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getTypesByNamed',[name]);
  },

  updateNamedTypes: function (key,adds,dels){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateNamedTypes',[key,adds,dels]);
  },

  getNamedList: function (){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getNamedList',[]);
  },

  sn:'cn.jmicro.api.mng.IMonitorTypeService',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
