window.jm = window.jm || {};

let ROOT = '/jmicro/JMICRO';
let MNG = 'mng';

jm.mng = {

    ROUTER_ROOT : ROOT + '/routeRules',
    CONFIG_ROOT : ROOT,
    RULE_ID: 'org.jmicro.api.route.RouteRule',

    cache:{

    },

    srv : {
        getServices: function (){
            let req =  this.__ccreq();
            req.method = 'getServices';
            req.args = [];
            return jm.rpc.callRpc(req);
        },

        updateItem: function (si){
            let req =  this.__ccreq();
            req.method = 'updateItem';
            req.args = [si];
            return jm.rpc.callRpc(req);
        },

        updateMethod: function (method){
            let req =  this.__ccreq();
            req.method = 'updateMethod';
            req.args = [method];
            return jm.rpc.callRpc(req);
        },

        //common config request instance
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
            return jm.rpc.callRpc(req);
        },

        update: function (path,val){
            let req =  this.__ccreq();
            req.method = 'update';
            req.args = [path,val];
            return jm.rpc.callRpc(req);
        },

        delete: function (path){
            let req =  this.__ccreq();
            req.method = 'delete';
            req.args = [path];
            return jm.rpc.callRpc(req);
        },

        add: function (path,val,isDir){
            let req =  this.__ccreq();
            req.method = 'add';
            req.args = [path,val,isDir];
            return jm.rpc.callRpc(req);
        },

        //common config request instance
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
                jm.rpc.callRpc(req)
                    .then((id)=>{
                        if(id > 0 && !!callback) {
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
                jm.rpc.callRpc(req)
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
                          jm.rpc.callRpc(req)
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
                jm.rpc.callRpc(req)
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
            jm.rpc.callRpc(req).then((data)=>{
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

        sn:'org.jmicro.mng.inter.ICommonManager',
        ns : MNG,
        v:'0.0.1',
    },

    monitor : {
        data:{},

        serverList: function (){
            let req =  this.__ccreq();
            req.method = 'serverList';
            req.args = [];
            let self = this;
            return jm.rpc.callRpc(req);
        },

        status: function (srvKeys){
            let req =  this.__ccreq();
            req.method = 'status';
            req.args = [srvKeys];
            return jm.rpc.callRpc(req);
        },

        enable: function (srvKey,enable){
            let req =  this.__ccreq();
            req.method = 'enable';
            req.args = [srvKey,enable];
            return jm.rpc.callRpc(req);
        },

        __ccreq:function(){
            let req = {};
            req.serviceName=this.sn;
            req.namespace = this.ns;
            req.version = this.v;
            return req;
        },

        sn:'org.jmicro.mng.inter.IMonitorServerManager',
        ns : MNG,
        v:'0.0.1',
    },

}

/*
export default {
    jm
}*/
