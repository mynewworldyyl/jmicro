import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default{

    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

    count: function (params) {
    return rpc.callRpc(this.__actreq( 'count', [params]))
  },

  query: function (params,pageSize,curPage) {
      return rpc.callRpc(this.__actreq( 'query', [params,pageSize,curPage]))
  },

  queryDict: function () {
      return rpc.callRpc(this.__actreq( 'queryDict', []))
  },

  getByLinkId: function(linkId) {
      return rpc.callRpc(this.__actreq( 'getByLinkId', [linkId]))
  },

  countLog: function (showType,params) {
      return rpc.callRpc(this.__actreq( 'countLog', [showType,params]))
  },

  queryLog: function (params,pageSize,curPage) {
      return rpc.callRpc(this.__actreq( 'queryLog', [params,pageSize,curPage]))
  },

  sn:'cn.jmicro.mng.api.ILogServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
