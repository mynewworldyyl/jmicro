import rpc from "@/rpc/rpcbase";
import ps from "@/rpc/pubsub";
import cons from "@/rpc/constants";

export default {

  getAllLogFileEntry : function() {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getAllLogFileEntry', []);
  },

  startLogMonitor : function(processId,logFilePath,agentId,offsetFromLastLine) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'startLogMonitor', [processId,logFilePath,
      agentId, offsetFromLastLine]);
  },

  stopLogMonitor : function(processId,logFilePath,agentId) {
    return rpc.callRpcWithParams(this.sn, this.ns, this.v, 'stopLogMonitor', [processId,logFilePath,
      agentId]);
  },

  subscribeLog: function (processId, logFilePath, agentId, offsetFromLastLine, callback){
    let self = this;
    let topic="/"+processId+"/logs/"+logFilePath;
    //先订阅主题
    return new Promise(function(reso,reje){
      ps.subscribe(topic,{},callback)
        .then(id =>{
          if(id < 0) {
            if(id == -2) {
              reje('Pubsub server is DISABLE');
            }else {
              reje("Fail to subscribe topic:"+topic+', please check server log for detail info');
            }
          } else {
            self.startLogMonitor(processId,logFilePath,agentId,offsetFromLastLine)
              .then(resp =>{
                if(resp && resp.data) {
                  reso(true);
                } else {
                  //jm.mng.ps.unsubscribe(topic,callback)
                  reje(resp.msg);
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

  unsubscribeLog : function(processId, logFilePath, agentId, callback){
    let self = this;
    return new Promise((reso,reje)=>{
      let topic ="/"+processId+'/logs/'+logFilePath;
      ps.unsubscribe(topic,callback)
        .then(rst => {
          if(!!rst || rst == 0) {
            self.stopLogMonitor(processId,logFilePath,agentId)
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

  sn:'cn.jmicro.mng.api.IAgentLogService',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
