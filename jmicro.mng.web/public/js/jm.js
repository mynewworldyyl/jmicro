window.jm = window.jm || {};

let ROOT = '/jmicro/JMICRO';

jm.mng = {

    ROUTER_ROOT : ROOT + '/routeRules',
    CONFIG_ROOT : ROOT,
    RULE_ID: 'org.jmicro.api.route.RouteRule',

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
        ns:'manageService',
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
        ns:'configManager', v:'0.0.1',


    },



}

/*
export default {
    jm
}*/
