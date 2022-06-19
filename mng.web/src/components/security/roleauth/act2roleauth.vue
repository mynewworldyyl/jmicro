<template>
    <div class="actAuth">
        <div>
            <a @click="refreshActAuthList()"> {{'Refresh' | i18n}} </a>
        </div>
        <p>{{errMsg}}</p>
        <div>
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
                    </td>
                </tr>
            </table>
        </div>

        <div style="margin-top: 20px">
            <table v-if="showRoleList" class="configItemTalbe">
                <thead><tr><td>{{'Id'|i18n}}</td><td>{{'Name'|i18n}}</td><td>{{'Desc'|i18n}}</td>
                    <td>{{'Operation'|i18n}}</td></tr>
                </thead>
                <tr v-for="a in showRoleList" :key="a.id">
                    <td>{{a.id}}</td> <td>{{a.name}}</td><td>{{a.desc}}</td>
                    <td><a  @click="selectRole(a.roleId)"> {{'Select' | i18n}} </a></td>
                </tr>
            </table>
        </div>

    </div>
</template>

<script>

    //import {Constants} from "@/rpc/message"
    import cons from "@/rpc/constants"

    import st from "../c"

    const cid= 'actAuth';

    const sn = 'cn.jmicro.security.api.IRoleServiceJMSrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    export default {
        name: cid,

        props: {
            act: Object
        },

        data () {
            return {
                st,
                authStatus: st.statusLabels,

                authList: [],//当前账号已经选择的角色列表，显示于上面已选列表中

                allRoleList:[],// 全量角色列表
                showRoleList:[],//  当前可以选择的角色列表，显示于下面的可选列表中

                errMsg:'',
            }
        },

        watch:{
            "act":function(){
                this.refreshActAuthList();
            }
        },

        mounted(){
            this.refreshActAuthList()
        },

        methods:{
            refreshActAuthList(){
                if(!this.act) {
                    return
                }
				
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listRoleByActId', [this.act.id])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.errMsg = "";
                        this.refreshRoleList(resp.data)
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            refreshRoleList(actRoleList){
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listRoles', [{},1000,0])
                    .then((resp) => {
                        this.authList = [];
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        let al = resp.data
                        if(!actRoleList || actRoleList.length == 0) {
                            this.showRoleList = al
                        } else {
                            let sl = al.filter((e)=>{
                                for(let a of actRoleList) {
                                    if(a.roleId == e.roleId) return false
                                }
                                return true
                            })
                            this.showRoleList = sl
                            this.authList = actRoleList;
                        }
                        self.errMsg = "";
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            selectRole(roleId){
                this.errMsg = '';
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addRoleActId', [roleId,this.act.id])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.refreshActAuthList();
                    }).catch((err) => {
                    self.errMsg = err;
                });
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

        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
    .actAuth{

    }

</style>
