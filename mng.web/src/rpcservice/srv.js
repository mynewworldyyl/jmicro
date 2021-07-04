import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {
  getServices: function (all){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'getServices',[all]);
  },

  updateItem: function (si){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateItem',[si]);
  },

  updateMethod: function (method){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateMethod',[method]);
  },

  sn:'cn.jmicro.api.mng.IManageService',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
