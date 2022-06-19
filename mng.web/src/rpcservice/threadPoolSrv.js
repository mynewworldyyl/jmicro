import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

	__actreq :  function(method,args){
		return rpc.creq(this.sn,this.ns,this.v,method,args)
	},

  serverList : function () {
      return rpc.callRpc(this.__actreq( 'serverList', []))
  },

  getInfo : function (key,type) {
      return rpc.callRpc(this.__actreq('getInfo', [key,type]))
  },

  sn:'cn.jmicro.api.mng.IThreadPoolMonitorJMSrv',
    ns : cons.NS_MNG,
    v:'0.0.1',
}
