import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  count: function (query) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'count', [query]);
  },

  query: function (queryConditions,pageSize,curPage) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'query', [queryConditions, pageSize,curPage]);
  },

  sn:'cn.jmicro.mng.api.IPSDataService',
  ns : cons.NS_MNG,
  v:'0.0.1',

}
