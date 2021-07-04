import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default{

  count: function (params) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'count', [params]);
  },

  query: function (params,pageSize,curPage) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'query', [params,pageSize,curPage]);
  },

  queryDict: function () {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'queryDict', []);
  },

  getByLinkId: function(linkId) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getByLinkId', [linkId]);
  },

  countLog: function (showType,params) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'countLog', [showType,params]);
  },

  queryLog: function (params,pageSize,curPage) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'queryLog', [params,pageSize,curPage]);
  },

  sn:'cn.jmicro.mng.api.ILogService',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
