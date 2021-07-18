import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {
    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  getDeploymentList: function (){
      return rpc.callRpc(this.__actreq('getDeploymentList', []));
  },

  addDeployment: function (deployment){
      return rpc.callRpc(this.__actreq('addDeployment', [deployment]));
  },

  deleteDeployment: function (id){
      return rpc.callRpc(this.__actreq('deleteDeployment', [id]));
  },

  updateDeployment: function (deployment){
      return rpc.callRpc(this.__actreq('updateDeployment', [deployment]));
  },

  stopProcess: function (insId){
      return rpc.callRpc(this.__actreq('stopProcess', [insId]));
  },

  updateProcess: function (pi){
      return rpc.callRpc(this.__actreq('updateProcess', [pi]));
  },

  getProcessInstanceList: function (all){
      return rpc.callRpc(this.__actreq('getProcessInstanceList', [all]));
  },

  getAgentList: function (showAll){
      return rpc.callRpc(this.__actreq('getAgentList', [showAll]));
  },

  changeAgentState: function (agentId){
      return rpc.callRpc(this.__actreq('changeAgentState', [agentId]));
  },

  clearResourceCache: function (agentId){
      return rpc.callRpc(this.__actreq('clearResourceCache', [agentId]));
  },

  stopAllInstance:function(agentId) {
      return rpc.callRpc(this.__actreq('stopAllInstance', [agentId]));
  },

  sn:'cn.jmicro.api.mng.IChoreographyServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
