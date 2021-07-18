import rpc from "@/rpc/rpcbase";
import cons from "@/rpc/constants";

export default {

  updatePwd: function (newPwd,oldPwd,cb){
    rpc.callRpc(this.__ccreq('updatePwd',[newPwd,oldPwd]))
      .then(( resp )=>{
        if(resp && resp.code == 0) {
          cb('success',null);
        } else {
          cb(null,resp.msg);
        }
      }).catch((err)=>{
      console.log(err);
      cb(null,err);
    });
  },

  regist: function (actName,pwd,email,mobile,cb){
    //let self = this;
    rpc.callRpc(this.__ccreq('regist',[actName,pwd,email,mobile]))
      .then(( resp )=>{
        if(resp && resp.code == 0) {
          cb('success',null);
        } else {
          cb(null,resp.msg);
        }
      }).catch((err)=>{
      console.log(err);
      cb(null,err);
    });
  },

  getAccountList : function (query,pageSize,curPage){
    return rpc.callRpc(this.__ccreq('getAccountList',[query,pageSize,curPage]));
  },

  countAccount : function (query){
    return rpc.callRpc(this.__ccreq('countAccount',[query]));
  },

  getPermissionsByActId : function (actId){
    return rpc.callRpc(this.__ccreq('getPermissionsByActId',[actId]));
  },

  updateActPermissions : function (actId,adds,dels){
    return rpc.callRpc(this.__ccreq('updateActPermissions',[actId,adds,dels]));
  },

  changeAccountStatus : function (actId){
    return rpc.callRpc(this.__ccreq('changeAccountStatus',[actId]));
  },

  resendActiveEmail : function (actName){
    return rpc.callRpc(this.__ccreq('resendActiveEmail',[actName]));
  },

  getAllPermissions : function (){
    return rpc.callRpc(this.__ccreq('getAllPermissions',[]));
  },

  checkAccountExist : function (actName){
    return rpc.callRpc(this.__ccreq('checkAccountExist',[actName]));
  },

  resetPwdEmail : function (actName,checkCode){
    return rpc.callRpc(this.__ccreq('resetPwdEmail',[actName,checkCode]));
  },

  resetPwd : function ( actName,  checkcode,  newPwd){
    return rpc.callRpc(this.__ccreq('resetPwd',[actName,  checkcode,  newPwd]));
  },


  __ccreq : function(method,args){
      return rpc.creq(this.sn,this.ns,this.v,method,args)
  },

  sn:'cn.jmicro.security.api.IAccountServiceJMSrv',
  ns: cons.NS_SECURITY,
  v : '0.0.1',
}
