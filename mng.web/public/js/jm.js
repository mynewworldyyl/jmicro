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

    callRpc : function(req,upProtocol,downProtocol) {
        return new Promise(function(reso,reje){
            jm.rpc.callRpc(req, null, upProtocol,downProtocol)
                .then((data)=>{
                    if(downProtocol == jm.rpc.Constants.PROTOCOL_BIN) {
                        reso(jm.utils.parseJson(data));
                    } else {
                        reso(data);
                    }
                })
                .catch((err)=>{
                    reje(err);
                })
        });
    },

    callRpcWithParams : function(service,namespace,version,method,args) {
        let req = {};
        req.serviceName = service;
        req.namespace = namespace;
        req.version = version;
        req.method = method;
        req.args = args;
        return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
    },

    srv : {
        getServices: function (){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getServices',[]);
        },

        updateItem: function (si){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'updateItem',[si]);
        },

        updateMethod: function (method){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'updateMethod',[method]);
        },

        sn:'cn.jmicro.api.mng.IManageService',
        ns : MNG,
        v:'0.0.1',
    },

    conf:{
        getChildren : function (path,all){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getChildren',[path,all]);
        },

        update: function (path,val){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'update',[path,val]);
        },

        delete: function (path){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'delete',[path]);
        },

        add: function (path,val,isDir){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'add',[path,val,isDir]);
        },

        sn:'cn.jmicro.api.mng.IConfigManager',
        ns: MNG,
        v:'0.0.1',
    },

    ps : {

        listeners:{},

        onMsg : function(msg) {
            let cbs = this.listeners[msg.topic];
            for(let i = 0; i < cbs.length; i++){
                if(!!cbs[i]) {
                    cbs[i](msg);
                }
            }
        },

        subscribe: function (topic,ctx,callback){

            if(this.listeners[topic] && this.listeners[topic].length > 0) {
                let cs = this.listeners[topic];
                callback.id = 0; //已经由别的接口订阅此主题，现在只需要注入回调即可
                let flag = false;
                //排除同一个回调方法重复订阅同一主题的情况
                for(let i = 0; i < cs.length; i++) {
                    if(cs[i] == callback) {
                        flag = true;
                        break;
                    }
                }
                if(!flag) {
                    cs.push(callback);
                }

                return new Promise(function(reso){
                    reso(0);
                });
            }

            let req =  this.__ccreq();
            req.method = 'subscribe';
            req.args = [topic, ctx | {}];
            if(!this.listeners[topic]) {
                this.listeners[topic] = [];
            }
            self.listeners[topic].push(callback);

            let self = this;
            return new Promise(function(reso,reje){
                jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
                    .then((id)=>{
                        if( id > 0 && !!callback ) {
                            callback.id = id;
                        }
                        reso(id);
                    }).catch(err =>{
                        reje(err);
                });
            });
        },

        unsubscribe: function (topic,callback){
            let cs = this.listeners[topic];
            if(cs && cs.length > 0) {
                let idx = -1;
                for(let i =0; i < cs.length; i++) {
                    if(cs[i] == callback) {
                        idx = i;
                        break;
                    }
                }
                if(idx >= 0) {
                    cs.splice(idx,1);
                }
            }

            if(!!cs && cs.length > 0) {
                return new Promise(function(reso,reje){
                    reso(0);
                });
            }

            let req =  this.__ccreq();
            req.method = 'unsubscribe';
            req.args = [callback.id];
            return new Promise(function(reso,reje){
                jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
                    .then((rst)=>{
                        if(!rst) {
                            //console.log("Fail to unsubscribe topic:"+topic);
                            reje("Fail to unsubscribe topic:"+topic)
                        }else {
                            reso(rst);
                        }
                    }).catch(err =>{
                        reje(err);
                });
            });
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        MSG_TYPE_ASYNC_RESP : 0x06,

        sn:'cn.jmicro.gateway.MessageServiceImpl',
        ns:MNG,
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
                            jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
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
                          jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
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
                jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
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
            jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
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
        actInfo:null,
        actListeners:{},

        addListener : function(key,l) {
            if(!!this.actListeners[key]) {
                throw 'Exist listener: ' + key;
            }
            this.actListeners[key] = l;
        },

        removeListener : function(key) {
            if(!this.actListeners[key]) {
               return;
            }
            delete this.actListeners[key];
        },

        _notify : function(type) {
            for(let key in this.actListeners) {
                if(this.actListeners[key]) {
                    this.actListeners[key](type,this.actInfo);
                }
            }
        },

        login: function (actName,pwd,cb){
            if(this.actInfo && cb) {
                cb(this.actInfo,null);
                return;
            }
            let self = this;
            let req =  this.__ccreq();
            req.method = 'login';
            req.args = [actName,pwd];
            jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
                .then(( actInfo )=>{
                    if(actInfo && actInfo.success) {
                        self.actInfo = actInfo;
                        cb(actInfo,null);
                        jm.mng.init(function(suc){
                            self._notify(jm.rpc.Constants.LOGIN);
                        });
                    } else {
                        cb(null,actInfo.msg);
                    }
                }).catch((err)=>{
                    console.log(err);
                    cb(null,err);
                });;
        },

        logout: function (cb){
            if(!this.actInfo) {
                if(cb) {
                    cb(true,null)
                }
                return;
            }
            let self = this;
            let req =  this.__ccreq();
            req.method = 'logout';
            req.args = [ this.actInfo.loginKey ];
            jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
                .then(( actInfo )=>{
                    if(actInfo) {
                        self.actInfo = null;
                        jm.mng.comm.adminPer = false;
                        if(cb) {
                            cb(true,null)
                        }
                        jm.mng.init(function(suc){
                            self._notify(jm.rpc.Constants.LOGOUT);
                        });
                    }else {
                        if(cb) {
                            cb(false,'logout fail')
                        }
                    }
                }).catch((err)=>{
                    console.log(err);
                    if(cb) {
                        cb(false,err)
                    }
            });
        },

        __ccreq:function(){
            let req = {};
            req.serviceName = 'cn.jmicro.api.security.IAccountService';
            req.namespace = 'act';
            req.version = '0.0.1';
            return req;
        },

    },

    monitor : {
        data:{},

        serverList: function (){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'serverList',[]);
        },

        status: function (srvKeys){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'status',[srvKeys]);
        },

        enable: function (srvKey,enable){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'enable',[srvKey,enable]);
        },

        sn:'cn.jmicro.api.mng.IMonitorServerManager',
        ns : MNG,
        v:'0.0.1',
    },

    repository : {

        getResourceList: function (onlyFinish){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getResourceList',[onlyFinish]);
        },

        addResource: function (name,size){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'addResource',[name,size]);
        },

        addResourceData: function (name,data,blockNum){
            let req = {};

            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;

            req.method = 'addResourceData';
            req.args = [name,data,blockNum];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_BIN, jm.rpc.Constants.PROTOCOL_JSON);
        },

        deleteResource: function (name){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'deleteResource',[name]);
        },

        sn:'cn.jmicro.choreography.api.IResourceResponsitory',
        ns : 'rrs',
        v:'0.0.1',
    },

    choy : {

        getDeploymentList: function (){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getDeploymentList',[]);
        },

        addDeployment: function (deployment){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'addDeployment',[deployment]);
        },

        deleteDeployment: function (id){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'deleteDeployment',[id]);
        },

        updateDeployment: function (deployment){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'updateDeployment',[deployment]);
        },

        stopProcess: function (insId){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'stopProcess',[insId]);
        },

        getProcessInstanceList: function (all){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getProcessInstanceList',[all]);
        },

        getAgentList: function (showAll){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getAgentList',[showAll]);
        },

        changeAgentState: function (agentId){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'changeAgentState',[agentId]);
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
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'hasPermission',[per]);
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
                    jm.mng.callRpcWithParams(self.sn,self.ns,self.v,'getDicts',[nokeys])
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
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getAllConfigs',[]);
        },

        update: function (mcConfig){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'update',[mcConfig]);
        },

        delete: function (mcConfig){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'delete',[mcConfig]);
        },

        add: function (mcConfig){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'add',[mcConfig]);
        },

        getConfigByMonitorKey: function (key){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getConfigByMonitorKey',[key]);
        },

        updateMonitorTypes: function (key,adds,dels){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'updateMonitorTypes',[key,adds,dels]);
        },

        getMonitorKeyList: function (){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getMonitorKeyList',[]);
        },

        getConfigByServiceMethodKey: function (key){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getConfigByServiceMethodKey',[key]);
        },

        updateServiceMethodMonitorTypes: function (key,adds,dels){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'updateServiceMethodMonitorTypes',[key,adds,dels]);
        },

        getAllConfigsByGroup: function (groups){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getAllConfigsByGroup',[groups]);
        },

        addNamedTypes: function (name){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'addNamedTypes',[name]);
        },

        getTypesByNamed: function (name){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getTypesByNamed',[name]);
        },

        updateNamedTypes: function (key,adds,dels){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'updateNamedTypes',[key,adds,dels]);
        },

        getNamedList: function (){
            return jm.mng.callRpcWithParams(this.sn,this.ns,this.v,'getNamedList',[]);
        },

        sn:'cn.jmicro.api.mng.IMonitorTypeService',
        ns : 'mng',
        v:'0.0.1',
    },

    logSrv : {

        count: function (params) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'count', [params]);
        },

        query: function (params,pageSize,curPage) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'query', [params,pageSize,curPage]);
        },

        queryDict: function () {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'queryDict', []);
        },

        getByLinkId: function(linkId) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'getByLinkId', [linkId]);
        },

        countLog: function (params) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'countLog', [params]);
        },

        queryLog: function (params,pageSize,curPage) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'queryLog', [params,pageSize,curPage]);
        },

        sn:'cn.jmicro.api.mng.ILogService',
        ns : 'mng',
        v:'0.0.1',
    },

    threadPoolSrv : {

        serverList : function () {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'serverList', []);
        },

        getInfo : function (key,type) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'getInfo', [key,type]);
        },

        sn:'cn.jmicro.api.mng.IThreadPoolMonitor',
        ns : 'mng',
        v:'0.0.1',
    },

    hostNameSrv : {

        getHosts : function(name) {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'getHosts', [name]);
        },

        bestHost : function () {
            return jm.mng.callRpcWithParams(this.sn, this.ns, this.v, 'bestHost', []);
        },

        sn:'cn.jmicro.api.gateway.IHostNamedService',
        ns : 'mng',
        v:'0.0.1',
    },

}

/*
export default {
    jm
}*/
