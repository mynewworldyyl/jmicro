import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

const   dicts = {};

export default  {

    __actreq :  function(method,args){
        return rpc.creq(this.sn,this.ns,this.v,method,args)
    },

  init : function(/*cb*/) {
    //jm.mng.comm.init(cb);
  },

  hasPermission: function (per){
      return rpc.callRpc(this.__actreq('hasPermission', [per]))
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
        rpc.callRpc(this.__actreq('getDicts', [nokeys,qry]))
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
    let self = this;
      rpc.callRpc(this.__actreq('getI18NValues', [lang]))
      .then((data)=>{
        self.data = data;
        callback();
      }).then((err) => {
      callback(err);
    });
  },

  sn:'cn.jmicro.api.mng.ICommonManagerJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
