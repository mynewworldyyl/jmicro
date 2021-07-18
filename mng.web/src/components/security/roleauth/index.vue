<template>
  <div class="roleauth">
      <div>
          <a @click="refreshAuthList()"> {{'Refresh' | i18n}} </a>
          <a  v-if="cm.perType" @click="addAuthByClientId()"> {{'addClientId' | i18n}} </a>
          <a  v-if="cm.perType"  @click="addAuthByAccountName()"> {{'addAccountName' | i18n}} </a>
      </div>
      <p>{{auth.errMsg}}</p>
      <div v-if="auth.showAccountNameBox">
          <Label for="authAccountName">{{"AccountName"|i18n}}</Label>
          <Input id="authAccountName"  v-model="auth.accountName"/>
          <a @click="okAuthByAccountName()"> {{'Ok' | i18n}} </a>
          <a @click="cancelAuth()"> {{'Cancel' | i18n}} </a>
      </div>
      <div v-if="auth.showClientIdBox">
          <Label for="authAuthClientId">{{"Client ID"}}</Label>
          <Input id="authAuthClientId"  v-model="auth.clientId"/>
          <a  @click="okAuthByClientId()"> {{'OK' | i18n}} </a>
          <a  @click="cancelAuth()"> {{'Cancel' | i18n}} </a>
      </div>
      <table v-if="cm.authList">
          <thead><tr><td>{{'Hash'|i18n}}</td><td>{{'ClientId'|i18n}}</td><td>{{'Status'|i18n}}</td>
              <td>{{'actName'|i18n}}</td>
              <td>{{'CreatedTime'|i18n}}</td> <td>{{'UpdatedTime'|i18n}}</td>
              <td>{{'CreatedBy'|i18n}}</td><td>{{'UpdatedBy'|i18n}}</td>
              <td>{{'Desc'|i18n}}</td>
              <td>{{'Operation'|i18n}}</td></tr>
          </thead>
          <tr v-for="a in cm.authList" :key="a.id">
              <td>{{a.haCode}}</td><td>{{a.forId}}</td><td>{{authStatus[a.status]}}</td><td>{{a.actName || '*'}}</td>
              <td>{{a.createdTime|formatDate(1)}}</td><td>{{a.updatedTime|formatDate(1)}}</td>
              <td>{{a.createdBy}}</td><td>{{a.updatedBy}}</td><td v-html="a.remark"></td>
              <td><a v-if="a.status == 3"   @click="authAct(a,4)"> {{'Revoke' | i18n}} </a>
                  <a v-if="a.status == 1 || a.status == 4"   @click="authAct(a,3)"> {{'Approve' | i18n}} </a>
                  <a v-if="a.status == 1"   @click="authAct(a,2)"> {{'Reject' | i18n}} </a>
              </td>
          </tr>
      </table>
  </div>
</template>

<script>

    import rpc from "@/rpc/rpcbase"
    import {Constants} from "@/rpc/message"
    import cons from "@/rpc/constants"

    const cid= 'roleauth';

    const sn = 'cn.jmicro.api.security.IRoleServiceJMSrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    export default {
        name: cid,

        props: {
            role: Object
        },

        data () {
            return {
                authStatus:{"1":'Apply',"2":"Reject","3":"Approve","4":"Revoke"},
                showAccountNameBox:false,
                showClientIdBox:false,
                clientId:null,
                accountName:null,
                errMsg:'',
                showDescDialog:false,
                desc:'',
                auth:null,
                actType:-1,
            }
        },

        mounted(){
            this.refreshAuthList()
        },

        methods:{
            refreshAuthList(){
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, 'getAuthByServiceMethodCode', [this.cm.haCode])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.auth.errMsg = resp.msg;
                            return;
                        }
                        self.$nextTick(() => {self.cm.authList = resp.data})
                        self.auth.errMsg = "test";
                        self.auth.errMsg = "";
                    }).catch((err) => {
                    self.auth.errMsg = err;
                });
            },

            cancelAuth(){
                this.auth.showClientIdBox = false;
                this.auth.showAccountNameBox = false;
                this.auth.errMsg = '';
            },

            okAuthByClientId(){
                if(!this.auth.clientId) {
                    this.auth.errMsg = 'Client ID 不能为空';
                    return;
                }
                this.auth.errMsg = '';
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, 'addAuthByClientId', [this.cm.haCode,this.auth.clientId])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.auth.errMsg = resp.msg;
                            return;
                        }
                        self.auth.showClientIdBox = false;
                        self.refreshAuthList();
                    }).catch((err) => {
                    self.auth.errMsg = err;
                });
            },

            okAuthByAccountName(){
                if(!this.auth.accountName) {
                    this.auth.errMsg = '账号名不能为空';
                    return;
                }
                this.auth.errMsg = '';
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, 'addAuthByAccount', [this.cm.haCode,this.auth.accountName])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.auth.errMsg = resp.msg;
                            return;
                        }
                        self.auth.showAccountNameBox = false;
                        self.refreshAuthList();
                    }).catch((err) => {
                    self.auth.errMsg = err;
                });
            },

            authAct(auth,status){
                this.auth.status = status;
                this.auth.auth = auth;
                this.auth.showDescDialog = true;
            },

            descAuthOk() {
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, 'updateAuthStatus',
                    [this.auth.auth.id,this.auth.status,this.auth.desc])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.auth.errMsg = resp.msg;
                            return;
                        }
                        self.descAuthCancel();
                        self.refreshAuthList();
                    }).catch((err) => {
                    self.auth.errMsg = err;
                });
            },

            descAuthCancel() {
                this.auth.status = -1;
                this.auth.auth = null;
                this.auth.showDescDialog = false;
            },

            addAuthByClientId(){
                this.resetAuthData();
                this.auth.showClientIdBox=true;
            },

            addAuthByAccountName(){
                this.resetAuthData();
                this.auth.showAccountNameBox=true;
            },

            closeAuthDrawer() {
                this.resetAuthData();
                this.authClientDrawer.drawerStatus = false;
                this.authClientDrawer.drawerBtnStyle.zindex = 100;
            },

            openAuthDrawer(cm) {
                this.cm = cm;
                this.resetAuthData();
                this.refreshAuthList();

                this.errMsg = "";
                this.authClientDrawer.drawerStatus = true;
                this.authClientDrawer.drawerBtnStyle.zindex = 1000;
            },

            resetAuthData() {
                this.auth.errMsg = '';
                this.auth.showAccountNameBox=false;
                this.auth.showClientIdBox=false;
                this.auth.clientId=null;
                this.auth.accountName=null;
                this.auth.showDescDialog=false;
                this.auth.desc = '';
                this.auth.auth = null;
                this.auth.status = -1;
            },

        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JUserProfileList{
      height:auto;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
      word-break: break-all;
  }

</style>
