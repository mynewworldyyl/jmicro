import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  getKvs : function() {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getKvs', []);
  },

  getModuleList : function () {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getModuleList', []);
  },

  getModuleKvs : function (module) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getModuleKvs', [module]);
  },

  updateKv : function (module,kv) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'updateKv', [module,kv]);
  },

  sn:'cn.jmicro.mng.api.IProfileService',
  ns : cons.NS_MNG,
  v:'0.0.1',
}

