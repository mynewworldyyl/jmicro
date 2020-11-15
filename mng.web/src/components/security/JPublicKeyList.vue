<template>
    <div class="JPublicKeyList">

        <table class="configItemTalbe" width="99%">
            <thead><tr><td>{{'Prefix'|i18n}}</td><td style="width:550px">{{'PublicKey'|i18n}}</td>
                <td>{{'CreateTime'|i18n}}</td><td>{{'Creater'|i18n}}</td>
                <td>{{'Enable'|i18n}}</td><td>{{'Operator'|i18n}}</td></tr>
            </thead>
            <tr v-for="a in keyList" :key="a.id">
                <td>{{a.instancePrefix}}</td> <td>{{a.publicKey}}</td>
                <td>{{a.createdTime  | formatDate}}</td><td>{{a.creater}}</td> <td>{{a.enable}}</td>
                <td>&nbsp;
                    <a v-if="isLogin && a.enable" @click="enable(a,false)"> {{'Disable' | i18n}} </a>
                    <a v-if="isLogin  && !a.enable" @click="enable(a,true)"> {{'Enable' | i18n}} </a>
                    <a v-if="isLogin" @click="updatePrefix(a)"> {{'Update' | i18n}} </a>
                    <a v-if="isLogin" @click="deleteItem(a.id)"> {{'Delete' | i18n}} </a>
                </td>
            </tr>
        </table>

        <Modal v-model="updatePrefixDialog" :loading="true" width="360" @on-ok="doUpdatePrefix()" ref="updatePrefixDialog">
            <table>
                <tr><td>{{'Prefix'|i18n}}</td><td>
                    <input type="input"   v-model="updatePrefixVal"/></td></tr>

                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="createSecretDialog" :loading="true" width="360" @on-ok="doCreateRsaKey()" ref="createSecretDialog">
            <table>
                <tr><td>{{'Prefix'|i18n}}</td><td>
                    <input type="input"  v-model="updatePrefixVal"/></td></tr>
                <tr><td>{{'Password'|i18n}}</td><td>
                    <input type="input"   v-model="password"/></td></tr>
                <tr><td>{{'PrivateKeyDesc'|i18n}}</td><td>
                    <Input class='textarea' :rows="5" :autosize="{maxRows:8,minRows: 16}"
                           type="textarea" v-model="priKey"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

        <Modal v-model="addSecretDialog" :loading="true" width="360" @on-ok="doAddSecret()" ref="addSecretDialog">
            <table>
                <tr><td>{{'Prefix'|i18n}}</td><td>
                    <input type="input"  v-model="updatePrefixVal"/></td></tr>
                <tr><td>{{'PublicKeyDesc'|i18n}}</td><td>
                    <Input class='textarea' :rows="5" :autosize="{maxRows:8,minRows: 16}"
                           type="textarea" v-model="publicKey"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

    </div>
</template>

<script>

    const cid = 'publicKeyList';
    const sn = 'cn.jmicro.api.security.ISecretService';
    const ns = 'sec';
    const v = '0.0.1';

    export default {
        name: 'JPublicKeyList',
        data () {
            return {

                updatePrefixDialog:false,
                updateItem:null,
                updatePrefixVal:null,

                createSecretDialog:false,
                password:null,

                addSecretDialog:false,
                publicKey:'',
                priKey:'',

                keyList:[],
                isLogin : false,
                act:null,

                msg:'',
            }
        },
        methods: {

            refresh(){
                let self = this;
                this.isLogin = window.jm.rpc.isLogin();
                this.act = window.jm.rpc.actInfo;

                window.jm.rpc.callRpcWithParams(sn, ns, v, 'publicKeysList', [])
                    .then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        self.keyList = resp.data;
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            enable(it,enStatus) {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'enablePublicKey', [it.id,enStatus])
                    .then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                       it.enable = !it.enable;
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            updatePrefix(item) {
                this.updateItem = item;
                this.updatePrefixVal = item.prefix;
                this.updatePrefixDialog = true;
            },

            doUpdatePrefix() {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'updateInstancePrefix', [this.updateItem.id,
                    this.updatePrefixVal])
                    .then((resp)=>{

                        self.updatePrefixDialog = false;

                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                        }else {
                            self.updateItem.instancePrefix = self.updatePrefixVal;
                        }

                        self.updateItem = null;
                        self.updateItem = null;
                        self.updatePrefixVal = null;

                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            deleteItem(id) {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'deletePublicKey', [id])
                    .then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        let idx = -1;
                        for(let i = 0; i < self.keyList.length; i++) {
                            let it = self.keyList[i];
                            if(it.id == id) {
                                idx = i;
                                break;
                            }
                        }

                        if(idx >=0) {
                            self.keyList.splice(idx,1);
                        }

                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            createRsaKey() {
                this.updatePrefixVal="";
                this.password="";
                this.createSecretDialog = true;
                this.priKey = '';
            },

            doCreateRsaKey() {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'createSecret', [this.updatePrefixVal, this.password])
                    .then((resp)=>{
                        self.updatePrefixVal = null;
                        //self.createSecretDialog = false;
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                        } else {
                            self.priKey = resp.data.priKey;
                            self.keyList.push(resp.data);
                        }
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            addSecret() {
                this.updatePrefixVal = "";
                this.publicKey = "";
                this.addSecretDialog = true;
            },

            doAddSecret() {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'addPublicKeyForInstancePrefix',
                    [this.updatePrefixVal, this.publicKey])
                    .then((resp)=>{
                        self.updatePrefixVal = null;
                        self.publicKey = null;
                        self.addSecretDialog = false;

                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        self.keyList.push(resp.data);
                    }).catch((err)=>{
                    window.console.log(err);
                });
            }
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            //has admin permission, only control the show of the button
            window.jm.rpc.addActListener(cid,this.refresh);
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":'process',
                    "menus":[{name:"Create",label:"Create",icon:"ios-cog",call:self.createRsaKey},
                        {name:"Add",label:"Add",icon:"ios-cog",call:self.addSecret},
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });
            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

            this.refresh();
        },
    }
</script>

<style>
    .JPublicKeyList{
        height:auto;
    }
</style>