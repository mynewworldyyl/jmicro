import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default{
  data:{},

  serverList: function (){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'serverList',[]);
  },

  status: function (srvKeys){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'status',[srvKeys]);
  },

  enable: function (srvKey,enable){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'enable',[srvKey,enable]);
  },

  sn:'cn.jmicro.api.mng.IMonitorServerManager',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
