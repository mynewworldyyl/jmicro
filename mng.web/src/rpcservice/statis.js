import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";
import ps from "@/rpc/pubsub";


let statis = {

  type2Labels:null,
    types:null,
    labels:null,

    subscribeData: function (mkey,t,callback){
    let self = this;
    let topic = mkey+'##'+(t*1000);
    //先订阅主题
    return new Promise((reso,reje)=>{
      ps.subscribe(topic,null,callback)
        .then(id =>{
          if(id < 0) {
            reje("Fail to subscribe topic:"+topic+', please check server log for detail info');
            //m.mng.statis.unsubscribeData(mkey,t,callback);
          } else {
            let req =  self.__ccreq();
            req.method = 'startStatis';
            req.args = [mkey, t];
            rpc.callRpc(req)
              .then((res)=>{
                  let status = res.data
                if(status) {
                  reso(status);
                } else {
                  ps.unsubscribe(topic,callback)
                  reje('Fail to start statis monitor, please check the monitor server is running');
                }
              }).catch(err => {
              reje(err);
            });
          }
        }).catch(err => {
        reje(err);
      });
    });
  },

  unsubscribeData: function (mkey,t,callback){

    return new Promise((reso,reje)=>{
      let topic = mkey + "##"+(t*1000);
      ps.unsubscribe(topic,callback)
        .then(rst => {
          if(!!rst || rst == 0) {
            let req =  this.__ccreq();
            req.method = 'stopStatis';
            req.args = [mkey,t];
            rpc.callRpc(req,rpc.Constants.PROTOCOL_JSON, rpc.Constants.PROTOCOL_JSON)
              .then(r=>{
                reso(r);
              }).catch(err =>{
              reje(err);
            });
          }
        }).catch(err => {
        reje(err);
      });
    });
  },

  getType2Labels: function (callback){
    let self = this;
    if(self.type2Labels) {
      callback(self.type2Labels,null);
    }else {
      let req =  this.__ccreq();
      req.method = 'index2Label';
      req.args = [];
      rpc.callRpc(req,rpc.Constants.PROTOCOL_JSON, rpc.Constants.PROTOCOL_JSON)
        .then(res=>{
            let r = res.data
          self.type2Labels = r.indexes;
          self.types = r.types;
          self.labels = r.labels;
          callback(self.type2Labels,null);
        }).catch(err =>{
        callback(null,err);
      });
    }
  },

  getTypes: function (callback){

    let self = this;
    if(self.type2Labels) {
      if(callback) {
        callback(self.types,null);
      } else {
        return self.types;
      }
    } else {
      if(!callback) {
        return null;
      }else {
        self.getType2Labels(function(t,err){
          if(t) {
            callback(self.types,null);
          }else {
            callback(null,err);
          }
        });
      }
    }
  },

  getLabels: function (callback){
    let self = this;
    if(self.type2Labels) {
      if(callback) {
        callback(self.labels,null);
      }else {
        return self.labels;
      }
    } else {
      if(!callback) {
        return null;
      }else {
        self.getType2Labels(function(t,err){
          if(t) {
            callback(self.labels,null);
          }else {
            callback(null,err);
          }
        });
      }
    }
  },

  __ccreq:function(){
    let req = {};
    req.serviceName=this.sn;
    req.namespace = this.ns;
    req.version = this.v;
    return req;
  },

  sn:'cn.jmicro.api.mng.IStatisMonitorJMSrv',
    ns:cons.NS_MNG,
    v:'0.0.1',
}

export default statis;
