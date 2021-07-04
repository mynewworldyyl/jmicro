import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

const   dicts = {};

export default  {

  init : function(/*cb*/) {
    //jm.mng.comm.init(cb);
  },

  hasPermission: function (per){
    return rpc.callRpcWithParams(this.sn,this.ns,this.v,'hasPermission',[per]);
  },

  getDicts: function (keys,qry,forceReflesh){
    let self = this;
    return new Promise(function(reso,reje){
      let ds = {};
      let nokeys = [];
      let f = !!qry && qry.length > 0;
      if(!forceReflesh) {
        for(let i = 0; i <keys.length; i++) {
          let vk = f ? qry+'_'+keys[i]:keys[i];
          if(dicts[vk]) {
            ds[keys[i]] = dicts[vk];
          } else {
            nokeys.push(keys[i]);
          }
        }
      } else {
        nokeys = keys;
      }

      if(nokeys.length == 0) {
        reso(ds);
      } else {
        rpc.callRpcWithParams(self.sn,self.ns,self.v,'getDicts',[nokeys,qry])
          .then((resp)=>{
            if(resp.code != 0) {
              reje(resp.msg);
            } else {
              for(let i = 0; i < nokeys.length; i++) {
                let vk = f ? qry + '_'+nokeys[i] : nokeys[i];
                dicts[vk] =  resp.data[nokeys[i]];
                ds[nokeys[i]] = resp.data[nokeys[i]];
              }
              reso(ds);
            }
          }).catch((err)=>{
          reje(err);
        });
      }

    });
  },

  data:{},
  get: function (key){
    return !this.data ? "undefined" : this.data[key];
  },
  changeLang: function (lang,callback){
    let req =  this.__ccreq();
    req.method = 'getI18NValues';
    req.args = [lang];
    let self = this;
    rpc.callRpc(req,rpc.Constants.PROTOCOL_JSON, rpc.Constants.PROTOCOL_JSON)
      .then((data)=>{
        self.data = data;
        callback();
      }).then((err) => {
      callback(err);
    });
  },

  __ccreq:function(){
    let req = {};
    req.serviceName=this.sn;
    req.namespace = this.ns;
    req.version = this.v;
    return req;
  },

  sn:'cn.jmicro.api.mng.ICommonManager',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
