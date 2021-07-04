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

  getPermissionsByActName : function (actName){
    return rpc.callRpc(this.__ccreq('getPermissionsByActName',[actName]));
  },

  updateActPermissions : function (actName,adds,dels){
    return rpc.callRpc(this.__ccreq('updateActPermissions',[actName,adds,dels]));
  },

  changeAccountStatus : function (actName){
    return rpc.callRpc(this.__ccreq('changeAccountStatus',[actName]));
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
    let req = {};
    req.serviceName = 'cn.jmicro.security.api.IAccountService';
    req.namespace = cons.NS_SECURITY;
    req.version = '0.0.1';
    req.args = args;
    req.method = method;
    return req;
  }

}
