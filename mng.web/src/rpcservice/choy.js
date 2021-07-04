import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  getDeploymentList: function (){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getDeploymentList',[]);
  },

  addDeployment: function (deployment){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'addDeployment',[deployment]);
  },

  deleteDeployment: function (id){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'deleteDeployment',[id]);
  },

  updateDeployment: function (deployment){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateDeployment',[deployment]);
  },

  stopProcess: function (insId){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'stopProcess',[insId]);
  },

  updateProcess: function (pi){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateProcess',[pi]);
  },

  getProcessInstanceList: function (all){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getProcessInstanceList',[all]);
  },

  getAgentList: function (showAll){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getAgentList',[showAll]);
  },

  changeAgentState: function (agentId){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'changeAgentState',[agentId]);
  },

  clearResourceCache: function (agentId){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'clearResourceCache',[agentId]);
  },

  stopAllInstance:function(agentId) {
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'stopAllInstance',[agentId]);
  },

  sn:'cn.jmicro.api.mng.IChoreographyService',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
