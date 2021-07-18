import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  getHosts : function() {
      return rpc.callRpc(this.__actreq('getHosts', ["nettyhttp"]))
  },

  bestHost : function () {
      return rpc.callRpc(this.__actreq('bestHost', ["nettyhttp"]))
  },

  sn:'cn.jmicro.api.gateway.IBaseGatewayServiceJMSrv',
  ns : cons.NS_API_GATEWAY,
  v:'0.0.1',
}
