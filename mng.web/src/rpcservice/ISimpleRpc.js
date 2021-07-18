import rpc from "@/rpc/rpcbase";

export default {

  hello: function (msg) {
    let req = rpc.creq(this.sn,this.ns,this.v, 'hello', [msg])
    return rpc.callRpc(req);
  },

  sn:'cn.expjmicro.example.api.rpc.ISimpleRpc',
  ns : 'exampleProvider',
  v:'0.0.1',

}
