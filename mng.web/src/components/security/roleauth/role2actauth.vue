<template>
  <div class="roleauth">
      <div>
          <a   @click="refreshAuthList()"> {{'Refresh' | i18n}} </a>
          <a   @click="addAuthByActId()"> {{'addClientId' | i18n}} </a>
          <a   @click="addAuthByAccountName()"> {{'addAccountName' | i18n}} </a>
      </div>
      <p>{{errMsg}}</p>
      <div v-if="showAccountNameBox">
          <Label for="authAccountName">{{"AccountName"|i18n}}</Label>
          <Input id="authAccountName"  v-model="accountName"/>
          <a @click="okAuthByAccountName()"> {{'Ok' | i18n}} </a>
          <a @click="cancelAuth()"> {{'Cancel' | i18n}} </a>
      </div>
      <div v-if="showClientIdBox">
          <Label for="authAuthClientId">{{"Client ID"}}</Label>
          <Input id="authAuthClientId"  v-model="actId"/>
          <a  @click="okAuthByClientId()"> {{'OK' | i18n}} </a>
          <a  @click="cancelAuth()"> {{'Cancel' | i18n}} </a>
      </div>
      <table v-if="authList" class="configItemTalbe">
          <thead><tr><td>{{'RoleName'|i18n}}</td><td>{{'ActName'|i18n}}</td><td>{{'Status'|i18n}}</td>
              <td>{{'CreatedTime'|i18n}}</td> <td>{{'UpdatedTime'|i18n}}</td>
              <td>{{'CreatedBy'|i18n}}</td><td>{{'UpdatedBy'|i18n}}</td>
              <td>{{'Desc'|i18n}}</td>
              <td>{{'Operation'|i18n}}</td></tr>
          </thead>
          <tr v-for="a in authList" :key="a.id">
              <td>{{a.roleName}}</td><td>{{a.actName}}</td><td>{{authStatus[a.status]}}</td>
              <td>{{a.createdTime|formatDate(1)}}</td><td>{{a.updatedTime|formatDate(1)}}</td>
              <td>{{a.createdBy}}</td><td>{{a.updatedBy}}</td><td v-html="a.remark"></td>
              <td><a v-if="a.status == st.STATUS_APPROVE"   @click="descAuthOk(a.id,st.STATUS_REVOKE)"> {{'Revoke' | i18n}} </a>
                  <a v-if="a.status == st.STATUS_APPLY || a.status == st.STATUS_REJECT"   @click="descAuthOk(a.id,st.STATUS_APPROVE)"> {{'Approve' | i18n}} </a>
                  <a v-if="a.status == st.STATUS_APPLY"   @click="descAuthOk(a.id,st.STATUS_REJECT)"> {{'Reject' | i18n}} </a>
				  <a v-if="a.status == st.STATUS_REVOKE"   @click="reinvokeAuth(a.actId)"> {{'Reinvoke' | i18n}} </a>
              </td>
          </tr>
      </table>
  </div>
</template>

<script>

    import cons from "@/rpc/constants"

    import st from "../c"

    const cid= 'roleauth';

    const sn = 'cn.jmicro.security.api.IRoleServiceJMSrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    export default {
        name: cid,

        props: {
            role: Object
        },

        watch:{
            "role":function() {
                this.refreshAuthList()
            }
        },

        data () {
            return {
                st,
                authStatus: st.statusLabels,
                showAccountNameBox: false,
                showClientIdBox: false,
                actId: null,
                accountName: null,

                showDescDialog: false,
                desc: '',
                actType: -1,
                authList: [],

                errMsg:'',
            }
        },

        mounted(){
        },

        methods:{
			
			reinvokeAuth(aid){
				this.actId = aid
				this.showClientIdBox=true;
				this.okAuthByClientId()
			},
			
            refreshAuthList(){
                if(!this.role) {
                    return
                }
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listRoleByRoleId', [this.role.roleId])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.$nextTick(() => { self.authList = resp.data })
                        self.errMsg = "";
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            cancelAuth(){
                this.showClientIdBox = false;
                this.showAccountNameBox = false;
                this.errMsg = '';
            },

            okAuthByClientId(){
                if(!this.actId) {
                    this.errMsg = 'Client ID 不能为空';
                    return;
                }
                this.errMsg = '';
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addRoleActId', [this.role.roleId,this.actId])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.showClientIdBox = false;
                        self.refreshAuthList();
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            okAuthByAccountName(){
                if(!this.accountName) {
                    this.errMsg = '账号名不能为空';
                    return;
                }
                this.errMsg = '';
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addRoleActName', [this.role.roleId,this.accountName])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.showAccountNameBox = false;
                        self.refreshAuthList();
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            authAct(auth,status){
                this.status = status;
                this.auth = auth;
                this.showDescDialog = true;
            },

            descAuthOk(authId,status) {
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateRoleActStatus',
                    [authId, status, this.desc])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.descAuthCancel();
                        self.refreshAuthList();
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            descAuthCancel() {
                this.status = -1;
                this.auth = null;
                this.showDescDialog = false;
            },

            addAuthByActId(){
                this.resetAuthData();
                this.showClientIdBox=true;
            },

            addAuthByAccountName(){
                this.resetAuthData();
                this.showAccountNameBox=true;
            },

            closeAuthDrawer() {
                this.resetAuthData();
            },

            openAuthDrawer(cm) {
                this.cm = cm;
                this.resetAuthData();
                this.refreshAuthList();

                this.errMsg = "";
            },

            resetAuthData() {
                this.errMsg = '';
                this.showAccountNameBox=false;
                this.showClientIdBox=false;
                this.actId=null;
                this.accountName=null;
                this.showDescDialog=false;
                this.desc = '';
                this.status = -1;
            },

        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .roleauth a {
      display: inline-block;
      margin-right: 8px;
  }

</style>
