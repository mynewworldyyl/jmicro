import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default{
  data:{},

    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  serverList: function (){
      return rpc.callRpc(this.__actreq( 'serverList', []))
  },

  status: function (srvKeys){
      return rpc.callRpc(this.__actreq( 'status', [srvKeys]))
  },

  enable: function (srvKey,enable){
      return rpc.callRpc(this.__actreq( 'enable', [srvKey,enable]))
  },

  sn:'cn.jmicro.api.mng.IMonitorServerManagerJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
