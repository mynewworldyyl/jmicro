import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  getResourceList: function (qry, pageSize, curPage) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getResourceList', [qry, pageSize, curPage]);
  }
,

  addResource: function (name, size) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'addResource', [name, size]);
  }
,

  updateResource: function (res, updateFile) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'updateResource', [res, updateFile]);
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
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'deleteResource', [name]);
  }
,

  queryDict: function () {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'queryDict', []);
  }
,

  waitingResList: function (resId) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'waitingResList', [resId]);
  }
,

  dependencyList: function (resId) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'dependencyList', [resId]);
  }
,

  parseRemoteClass(resId)
  {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'parseRemoteClazz', [resId]);
  }
,

  sn:'cn.jmicro.choreography.api.IResourceResponsitory',
    ns: cons.NS_RESPOSITORY,
    v: '0.0.1'
}
