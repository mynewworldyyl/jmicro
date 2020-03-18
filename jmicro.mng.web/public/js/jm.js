window.jm = window.jm || {};

jm.mng = {
    Constants:{
        ConfigManager:{
            sn:'org.jmicro.api.mng.IConfigManager', ns:'configManager', v:'0.0.1',
            getChildren:'getChildren',
            update:'update',
            delete:'delete',
        }
    },

    //common config request instance
    __ccreq:function(){
        let req = {};
        req.serviceName=jm.mng.Constants.ConfigManager.sn;
        req.namespace = jm.mng.Constants.ConfigManager.ns;
        req.version = jm.mng.Constants.ConfigManager.v;
        return req;
    },

    getChildren: function (path,all){
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

    add: function (path,val){
        let req =  this.__ccreq();
        req.method = 'add';
        req.args = [path,val];
        return jm.rpc.callRpc(req);
    },


}

/*
export default {
    jm
}*/
