import rpc from "@/rpc/rpcbase";

export default {

  hello: function (msg) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'hello', [msg]);
  },

  sn:'cn.expjmicro.example.api.rpc.ISimpleRpc',
  ns : 'exampleProvider',
  v:'0.0.1',

}
