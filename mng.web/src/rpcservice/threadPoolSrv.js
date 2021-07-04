import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  serverList : function () {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'serverList', []);
  },

  getInfo : function (key,type) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getInfo', [key,type]);
  },

  sn:'cn.jmicro.api.mng.IThreadPoolMonitor',
    ns : cons.NS_MNG,
    v:'0.0.1',
}
