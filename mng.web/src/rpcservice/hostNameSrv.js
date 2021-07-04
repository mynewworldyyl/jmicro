import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  getHosts : function() {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getHosts', ["nettyhttp"]);
  },

  bestHost : function () {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'bestHost', ["nettyhttp"]);
  },

  sn:'cn.jmicro.api.gateway.IBaseGatewayService',
  ns : cons.NS_API_GATEWAY,
  v:'0.0.1',
}
