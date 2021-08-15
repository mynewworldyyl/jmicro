import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  getResourceList: function (qry, pageSize, curPage) {
      return rpc.callRpc(this.__actreq( 'getResourceList', [qry, pageSize, curPage]))
  }
,

  addResource: function (name, size) {
      return rpc.callRpc(this.__actreq( 'addResource', [name, size]))
  }
,

  updateResource: function (res, updateFile) {
      return rpc.callRpc(this.__actreq( 'updateResource', [res, updateFile]))
  }
,

  addResourceData: function (resId, data, blockNum) {
    let req = {};

    req.serviceName = this.sn;
    req.namespace = this.ns;
    req.version = this.v;

    req.method = 'addResourceData';
    req.args = [resId, data, blockNum];
    return rpc.callRpc(req, rpc.Constants.PROTOCOL_BIN, rpc.Constants.PROTOCOL_JSON);
  }
,

  deleteResource: function (name) {
      return rpc.callRpc(this.__actreq( 'deleteResource', [name]))
  }
,

  queryDict: function () {
      return rpc.callRpc(this.__actreq( 'queryDict', []))
  }
,

  waitingResList: function (resId) {
      return rpc.callRpc(this.__actreq( 'waitingResList', [resId]))
  }
,

  dependencyList: function (resId) {
      return rpc.callRpc(this.__actreq( 'dependencyList', [resId]))
  }
,

  parseRemoteClass(resId)
  {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'parseRemoteClazz', [resId]);
  }
,

  sn:'cn.jmicro.choreography.api.IResourceResponsitoryJMSrv',
    ns: cons.NS_RESPOSITORY,
    v: '0.0.1'
}
