window.jm = window.jm || {};

let ROOT = '/jmicro/JMICRO';
let MNG = 'mng';

jm.mng = {

    ROUTER_ROOT : ROOT + '/routeRules',
    CONFIG_ROOT : ROOT,
    AGENT_ROOT : ROOT + '/choreography/agents',
    INSTANCE_ROOT : ROOT + '/choreography/instances',
    DEP_ROOT : ROOT + '/choreography/deployments',

    RULE_ID: 'cn.jmicro.api.route.RouteRule',

    cache:{

    },

    init : function(cb) {
        jm.mng.comm.init(cb);
    },

    srv : {
        getServices: function (){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getServices',[]);
        },

        updateItem: function (si){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateItem',[si]);
        },

        updateMethod: function (method){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateMethod',[method]);
        },

        sn:'cn.jmicro.api.mng.IManageService',
        ns : MNG,
        v:'0.0.1',
    },

    conf:{
        getChildren : function (path,all){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getChildren',[path,all]);
        },

        update: function (path,val){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'update',[path,val]);
        },

        delete: function (path){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'delete',[path]);
        },

        add: function (path,val,isDir){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'add',[path,val,isDir]);
        },

        sn:'cn.jmicro.api.mng.IConfigManager',
        ns: MNG,
        v:'0.0.1',
    },

    statis : {

        type2Labels:null,
        types:null,
        labels:null,

        subscribeData: function (mkey,t,callback){
            let self = this;
            let topic = mkey+'##'+(t*1000);
            //先订阅主题
            return new Promise(function(reso,reje){
                jm.mng.ps.subscribe(topic,null,callback)
                    .then(id =>{
                        if(id < 0) {
                            reje("Fail to subscribe topic:"+topic+', please check server log for detail info');
                            //m.mng.statis.unsubscribeData(mkey,t,callback);
                        } else {
                            let req =  self.__ccreq();
                            req.method = 'startStatis';
                            req.args = [mkey, t];
                            jm.rpc.callRpc(req)
                                .then(status =>{
                                    if(status) {
                                        reso(status);
                                    } else {
                                        jm.mng.ps.unsubscribe(topic,callback)
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
              jm.mng.ps.unsubscribe(topic,callback)
                  .then(rst => {
                      if(!!rst || rst == 0) {
                          let req =  this.__ccreq();
                          req.method = 'stopStatis';
                          req.args = [mkey,t];
                          jm.rpc.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
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
                jm.rpc.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
                    .then(r=>{
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
                        if(!!t) {
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
                        if(!!t) {
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

        sn:'cn.jmicro.api.mng.IStatisMonitor',
        ns:'mng',
        v:'0.0.1',
    },

    i18n : {
        data:{},
        get: function (key){
            return !this.data ? "undefined" : this.data[key];
        },
        changeLang: function (lang,callback){
            let req =  this.__ccreq();
            req.method = 'getI18NValues';
            req.args = [lang];
            let self = this;
            jm.rpc.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
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
        ns : MNG,
        v:'0.0.1',
    },

    act : {

        updatePwd: function (newPwd,oldPwd,cb){
            jm.rpc.callRpc(this.__ccreq('updatePwd',[newPwd,oldPwd]))
                .then(( resp )=>{
                    if(resp && resp.code == 0) {
                        cb('success',null);
                    } else {
                        cb(null,resp.msg);
                    }
                }).catch((err)=>{
                console.log(err);
                cb(null,err);
            });;
        },

        regist: function (actName,pwd,cb){
            let self = this;
            jm.rpc.callRpc(this.__ccreq('regist',[actName,pwd]))
                .then(( resp )=>{
                    if(resp && resp.code == 0) {
                        cb('success',null);
                    } else {
                        cb(null,resp.msg);
                    }
                }).catch((err)=>{
                console.log(err);
                cb(null,err);
            });;
        },

        getAccountList : function (query,pageSize,curPage){
            return jm.rpc.callRpc(this.__ccreq('getAccountList',[query,pageSize,curPage]));
        },

        countAccount : function (query){
            return jm.rpc.callRpc(this.__ccreq('countAccount',[query]));
        },

        getPermissionsByActName : function (actName){
            return jm.rpc.callRpc(this.__ccreq('getPermissionsByActName',[actName]));
        },

        updateActPermissions : function (actName,adds,dels){
            return jm.rpc.callRpc(this.__ccreq('updateActPermissions',[actName,adds,dels]));
        },

        changeAccountStatus : function (actName,status){
            return jm.rpc.callRpc(this.__ccreq('changeAccountStatus',[actName,status]));
        },

        getAllPermissions : function (){
            return jm.rpc.callRpc(this.__ccreq('getAllPermissions',[]));
        },

        __ccreq : function(method,args){
            let req = {};
            req.serviceName = 'cn.jmicro.mng.api.IMngAccountService';
            req.namespace = 'mng';
            req.version = '0.0.1';
            req.args = args;
            req.method = method;
            return req;
        }

    },

    monitor : {
        data:{},

        serverList: function (){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'serverList',[]);
        },

        status: function (srvKeys){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'status',[srvKeys]);
        },

        enable: function (srvKey,enable){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'enable',[srvKey,enable]);
        },

        sn:'cn.jmicro.api.mng.IMonitorServerManager',
        ns : MNG,
        v:'0.0.1',
    },

    repository : {

        getResourceList: function (onlyFinish){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getResourceList',[onlyFinish]);
        },

        addResource: function (name,size){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'addResource',[name,size]);
        },

        addResourceData: function (name,data,blockNum){
            let req = {};

            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;

            req.method = 'addResourceData';
            req.args = [name,data,blockNum];
            return jm.rpc.callRpc(req,jm.rpc.Constants.PROTOCOL_BIN, jm.rpc.Constants.PROTOCOL_JSON);
        },

        deleteResource: function (name){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'deleteResource',[name]);
        },

        sn:'cn.jmicro.choreography.api.IResourceResponsitory',
        ns : 'rrs',
        v:'0.0.1',
    },

    choy : {

        getDeploymentList: function (){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getDeploymentList',[]);
        },

        addDeployment: function (deployment){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'addDeployment',[deployment]);
        },

        deleteDeployment: function (id){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'deleteDeployment',[id]);
        },

        updateDeployment: function (deployment){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateDeployment',[deployment]);
        },

        stopProcess: function (insId){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'stopProcess',[insId]);
        },

        getProcessInstanceList: function (all){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getProcessInstanceList',[all]);
        },

        getAgentList: function (showAll){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getAgentList',[showAll]);
        },

        changeAgentState: function (agentId){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'changeAgentState',[agentId]);
        },

        clearResourceCache: function (agentId){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'clearResourceCache',[agentId]);
        },

        stopAllInstance:function(agentId) {
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'stopAllInstance',[agentId]);
        },

        sn:'cn.jmicro.api.mng.IChoreographyService',
        ns : 'mng',
        v:'0.0.1',
    },

    comm : {
        adminPer:false,
        dicts:{},

        init:function(cb) {
            let self = this;
            window.jm.mng.comm.hasPermission(1).then((rst)=>{
                self.adminPer = rst;
                if(cb) {
                    cb(true);
                }
            }).catch((err)=>{
                window.console.log(err);
                if(cb) {
                    cb(false);
                }
            });
        },

        hasPermission: function (per){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'hasPermission',[per]);
        },

        getDicts: function (keys){
            let self = this;
            return new Promise(function(reso,reje){
                let ds = {};
                let nokeys = [];
                for(let i = 0; i <keys.length; i++) {
                    if(self.dicts[keys[i]]) {
                        ds[keys[i]] = self.dicts[keys[i]];
                    } else {
                        nokeys.push(keys[i]);
                    }
                }

                if(nokeys.length == 0) {
                    reso(ds);
                } else {
                    jm.rpc.callRpcWithParams(self.sn,self.ns,self.v,'getDicts',[nokeys])
                        .then((resp)=>{
                            if(resp.code != 0) {
                                reje(resp.msg);
                            } else {
                                for(let i = 0; i < nokeys.length; i++) {
                                    self.dicts[nokeys[i]] =  resp.data[nokeys[i]];
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

        sn:'cn.jmicro.api.mng.ICommonManager',
        ns : MNG,
        v:'0.0.1',
    },

    moType : {

        getAllConfigs: function (){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getAllConfigs',[]);
        },

        update: function (mcConfig){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'update',[mcConfig]);
        },

        delete: function (mcConfig){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'delete',[mcConfig]);
        },

        add: function (mcConfig){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'add',[mcConfig]);
        },

        getConfigByMonitorKey: function (key){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getConfigByMonitorKey',[key]);
        },

        updateMonitorTypes: function (key,adds,dels){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateMonitorTypes',[key,adds,dels]);
        },

        getMonitorKeyList: function (){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getMonitorKeyList',[]);
        },

        getConfigByServiceMethodKey: function (key){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getConfigByServiceMethodKey',[key]);
        },

        updateServiceMethodMonitorTypes: function (key,adds,dels){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateServiceMethodMonitorTypes',[key,adds,dels]);
        },

        getAllConfigsByGroup: function (groups){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getAllConfigsByGroup',[groups]);
        },

        addNamedTypes: function (name){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'addNamedTypes',[name]);
        },

        getTypesByNamed: function (name){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getTypesByNamed',[name]);
        },

        updateNamedTypes: function (key,adds,dels){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'updateNamedTypes',[key,adds,dels]);
        },

        getNamedList: function (){
            return jm.rpc.callRpcWithParams(this.sn,this.ns,this.v,'getNamedList',[]);
        },

        sn:'cn.jmicro.api.mng.IMonitorTypeService',
        ns : 'mng',
        v:'0.0.1',
    },

    logSrv : {

        count: function (params) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'count', [params]);
        },

        query: function (params,pageSize,curPage) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'query', [params,pageSize,curPage]);
        },

        queryDict: function () {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'queryDict', []);
        },

        getByLinkId: function(linkId) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getByLinkId', [linkId]);
        },

        countLog: function (params) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'countLog', [params]);
        },

        queryLog: function (params,pageSize,curPage) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'queryLog', [params,pageSize,curPage]);
        },

        sn:'cn.jmicro.api.mng.ILogService',
        ns : 'mng',
        v:'0.0.1',
    },

    threadPoolSrv : {

        serverList : function () {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'serverList', []);
        },

        getInfo : function (key,type) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getInfo', [key,type]);
        },

        sn:'cn.jmicro.api.mng.IThreadPoolMonitor',
        ns : 'mng',
        v:'0.0.1',
    },

    hostNameSrv : {

        getHosts : function(name) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getHosts', [name]);
        },

        bestHost : function () {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'bestHost', []);
        },

        sn:'cn.jmicro.api.gateway.IHostNamedService',
        ns : 'mng',
        v:'0.0.1',
    },

    agentLogSrv : {

        getAllLogFileEntry : function() {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'getAllLogFileEntry', []);
        },

        startLogMonitor : function(processId,logFilePath,agentId,offsetFromLastLine) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'startLogMonitor', [processId,logFilePath,
                agentId, offsetFromLastLine]);
        },

        stopLogMonitor : function(processId,logFilePath,agentId) {
            return jm.rpc.callRpcWithParams(this.sn, this.ns, this.v, 'stopLogMonitor', [processId,logFilePath,
                agentId]);
        },

        subscribeLog: function (processId, logFilePath, agentId, offsetFromLastLine, callback){
            let self = this;
            let topic = "/" + processId + '/logs/'　+ logFilePath;
            //先订阅主题
            return new Promise(function(reso,reje){
                jm.mng.ps.subscribe(topic,{},callback)
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
                let topic = "/" + processId + '/logs/'　+ logFilePath;
                jm.mng.ps.unsubscribe(topic,callback)
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
        ns : 'mng',
        v:'0.0.1',
    },

}

/*
export default {
    jm
}*/
