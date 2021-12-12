import rpc from "@/rpc/rpcbase";
import ps from "@/rpc/pubsub";
import cons from "@/rpc/constants";

/*const sn = 'cn.jmicro.mng.api.II8NServiceJMSrv'
const ns = cons.NS_MNG
const v = '0.0.1'*/



export default {

     __actreq(method,args){
         return rpc.creq(this.sn,this.ns,this.v,method,args)
  },

  getAllLogFileEntry() {
    return rpc.callRpc(this.__actreq('getAllLogFileEntry', []));
  },

  startLogMonitor(processId,logFilePath,agentId,offsetFromLastLine) {
      return rpc.callRpc(this.__actreq('startLogMonitor', [processId,logFilePath,
          agentId, offsetFromLastLine]));
  },

  stopLogMonitor(processId,logFilePath,agentId) {
      return rpc.callRpc(this.__actreq('stopLogMonitor', [processId,logFilePath,
          agentId]));
  },

  subscribeLog(processId, logFilePath, agentId, offsetFromLastLine, callback){
    let self = this;
    let topic="/"+processId+"/logs/"+logFilePath;
    //先订阅主题
    return new Promise((reso,reje)=>{
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

  unsubscribeLog(processId, logFilePath, agentId, callback){
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

  sn:'cn.jmicro.mng.api.IAgentLogServiceJMSrv',
  ns : cons.NS_MNG,
  v:'0.0.1',
}
