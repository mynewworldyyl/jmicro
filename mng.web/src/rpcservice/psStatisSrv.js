import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  count: function (query) {
      return rpc.callRpc(this.__actreq( 'count', [query]))
  },

  query: function (queryConditions,pageSize,curPage) {
      return rpc.callRpc(this.__actreq( 'query', [queryConditions, pageSize,curPage]))
  },

  sn:'cn.jmicro.mng.api.IPSStatisServiceJMSrv',
    ns : cons.NS_MNG,
  v:'0.0.1',

}
