window.jm = window.jm || {};

let ROOT = '/jmicro/JMICRO';
let MNG = 'mng';

jm.mng = {

    ROUTER_ROOT : ROOT + '/routeRules',
    CONFIG_ROOT : ROOT,
    AGENT_ROOT : ROOT + '/choreography/agents',
    INSTANCE_ROOT : ROOT + '/choreography/instances',
    DEP_ROOT : ROOT + '/choreography/deployments',

    RULE_ID: 'org.jmicro.api.route.RouteRule',

    cache:{

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

    srv : {
        getServices: function (){
            let req =  this.__ccreq();
            req.method = 'getServices';
            req.args = [];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        updateItem: function (si){
            let req =  this.__ccreq();
            req.method = 'updateItem';
            req.args = [si];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        updateMethod: function (method){
            let req =  this.__ccreq();
            req.method = 'updateMethod';
            req.args = [method];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        sn:'org.jmicro.api.mng.IManageService',
        ns : MNG,
        v:'0.0.1',
    },

    conf:{
        getChildren : function (path,all){
            let req =  this.__ccreq();
            req.method = 'getChildren';
            req.args = [path,all];

           return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        update: function (path,val){
            let req =  this.__ccreq();
            req.method = 'update';
            req.args = [path,val];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        delete: function (path){
            let req =  this.__ccreq();
            req.method = 'delete';
            req.args = [path];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        add: function (path,val,isDir){
            let req =  this.__ccreq();
            req.method = 'add';
            req.args = [path,val,isDir];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        sn:'org.jmicro.api.mng.IConfigManager',
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

            let self = this;
            return new Promise(function(reso,reje){
                jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON)
                    .then((id)=>{
                        if( id > 0 && !!callback ) {
                            callback.id = id;
                            self.listeners[topic].push(callback);
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

        sn:'org.jmicro.gateway.MessageServiceImpl',
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

        sn:'org.jmicro.api.mng.IStatisMonitor',
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

        sn:'org.jmicro.mng.api.ICommonManager',
        ns : MNG,
        v:'0.0.1',
    },

    act : {
        actInfo:null,

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
                        if(cb) {
                            cb(true,null)
                        }
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
            req.serviceName = 'org.jmicro.api.security.IAccountService';
            req.namespace = 'act';
            req.version = '0.0.1';
            return req;
        },

    },

    monitor : {
        data:{},

        serverList: function (){
            let req =  this.__ccreq();
            req.method = 'serverList';
            req.args = [];
            let self = this;
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        status: function (srvKeys){
            let req =  this.__ccreq();
            req.method = 'status';
            req.args = [srvKeys];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        enable: function (srvKey,enable){
            let req =  this.__ccreq();
            req.method = 'enable';
            req.args = [srvKey,enable];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        sn:'org.jmicro.mng.api.IMonitorServerManager',
        ns : MNG,
        v:'0.0.1',
    },

    repository : {

        getResourceList: function (onlyFinish){
            let req =  this.__ccreq();
            req.method = 'getResourceList';
            req.args = [onlyFinish];
            let self = this;
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        addResource: function (name,size){
            let req =  this.__ccreq();
            req.method = 'addResource';
            req.args = [name,size ];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        addResourceData: function (name,data,blockNum){
            let req =  this.__ccreq();
            req.method = 'addResourceData';
            req.args = [name,data,blockNum];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_BIN, jm.rpc.Constants.PROTOCOL_JSON);
        },

        deleteResource: function (name){
            let req =  this.__ccreq();
            req.method = 'deleteResource';
            req.args = [name];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        sn:'org.jmicro.choreography.api.IResourceResponsitory',
        ns : 'rrs',
        v:'0.0.1',
    },

    deployment : {

        getDeploymentList: function (){
            let req =  this.__ccreq();
            req.method = 'getDeploymentList';
            req.args = [];
            let self = this;
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        addDeployment: function (deployment){
            let req =  this.__ccreq();
            req.method = 'addDeployment';
            req.args = [deployment];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        deleteDeployment: function (id){
            let req =  this.__ccreq();
            req.method = 'deleteDeployment';
            req.args = [id];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        updateDeployment: function (deployment){
            let req =  this.__ccreq();
            req.method = 'updateDeployment';
            req.args = [deployment];
            return jm.mng.callRpc(req,jm.rpc.Constants.PROTOCOL_JSON, jm.rpc.Constants.PROTOCOL_JSON);
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        sn:'org.jmicro.choreography.api.IDeploymentService',
        ns : 'mng',
        v:'0.0.1',
    },


}

/*
export default {
    jm
}*/
