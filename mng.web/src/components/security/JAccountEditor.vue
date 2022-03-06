<template>
    <div class="JAccountEditor">

        <div v-if="isLogin && actList && actList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td>Act Name</td><td>Client ID</td><td>Regist Time</td><td>Statu Code</td>
                    <td>Mobile</td><td>Email</td><td>LoginNum</td><td>LastLoginTime</td>
                    <td>{{"Operation"|i18n}}</td></tr>
                </thead>
                <tr v-for="c in actList" :key="c._id">
                    <td>{{c.actName}}</td><td>{{c.clientId}}</td><td>{{c.registTime | formatDate(1)}}</td>
                    <td>{{c.statuCode}}</td> <td>{{c.mobile}}</td> <td>{{c.email}}</td>
                    <td>{{c.loginNum}}</td><td>{{c.lastLoginTime | formatDate(2)}}</td>
                    <td>
                          <a v-if="c.statuCode==2" @click="openRoleInfoDrawer(c)">{{"Role"|i18n}}</a>
                        &nbsp;<a v-if="c.statuCode==2" @click="openActInfoDrawer(c)">{{"Permission"|i18n}}</a> &nbsp;&nbsp;&nbsp;&nbsp;
                          <a v-if="c.statuCode == 4" @click="changeAccountStatus(c)">{{"Unfreeze"|i18n}}</a>
                          <a v-if="c.statuCode == 2" @click="changeAccountStatus(c)">{{"Freeze"|i18n}}</a>
                          <a v-if="c.statuCode == 1" @click="resendActiveEmail(c)">{{"SendEmail"|i18n}}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && actList && actList.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin || !actList || actList.length == 0">
            No permission!
        </div>

        <Drawer  v-model="actInfoDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                 :draggable="true" :scrollable="true" width="80">
            <div>
                <a v-if="isLogin" @click="addPermissions()">Config Permissions</a>
            </div>

            <div style="position:relative;height:auto;margin-top:10px;">
                <Tree v-if="role && role.permissionParseEntires" :data="role.permissionParseEntires " class="actPermissionTree"></Tree>
            </div>
        </Drawer>

        <Drawer ref="permissionListDrawer"  v-model="permissionListDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="50">
            <div>
                <a v-if="isLogin" @click="doAddPermission()">Confirm</a>
            </div>
            <div>
                <Tree ref="plTree" v-if="curActParsedPermissions" :data="curActParsedPermissions" show-checkbox multiple class="permissionTree"></Tree>
            </div>
        </Drawer>

        <Drawer ref="roleInfoDrawer"  v-model="roleInfoDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="80">
            <actAuth :act="role"></actAuth>
            <!--<div>
                <a v-if="isLogin" @click="doUpdateActRole()">Confirm</a>
            </div>
            <div>
            ` <Transfer v-if="role"
                        :titles="['可选值','已选值']"
                    :data="allRoleList"
                    :target-keys="role.roles"
                    :render-format="getRoleLabel"
                    :operations="['Delete','Add']"
                    filterable
                    @on-change="roleSelect">
            </Transfer>
            </div>-->

        </Drawer>

    </div>
</template>

<script>

    //import treeTable from '../treetable/LinkLogTreeTable.vue'
    import act from "@/rpcservice/act"
    import cons from "@/rpc/constants"
    import c from "./c"
    import actAuth from "./roleauth/act2roleauth"

    const cid = 'account';

    export default {
        name: cid,
        components: {
            actAuth
        },
        data() {
            return {
                isLogin:false,
                actList: [],
                queryParams:{},
                totalNum:0,
                pageSize:10,
                curPage:1,

                srcPermissions:[],
                curActParsedPermissions:[],

                role : null, //store act current seledted

                actInfoDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },

                roleInfoDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },

                permissionListDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {right:'0px',zindex:1001},
                },

                selOptions:{},

            }
        },

        methods: {

            resendActiveEmail(c) {
                let self = this;
                act.resendActiveEmail(c.actName).then((resp) => {
                    if (resp.code == 0) {
                        self.$Message.info("Successfully");
                    } else {
                        self.$Message.info(resp.msg);
                    }
                }).catch((err) => {
                    window.console.log(err);
                });
            },

            changeAccountStatus(act) {
                let self = this;
                act.changeAccountStatus(act.id).then((resp) => {
                    if (resp.code == 0) {
                        self.refresh();
                    } else {
                        self.$Message.success(resp.msg);
                    }
                }).catch((err) => {
                    window.console.log(err);
                });
            },

            openRoleInfoDrawer(mi) {
                this.role = mi;
                this.roleInfoDrawer.drawerStatus = true;

              /*  if(this.allRoleList && this.allRoleList.length > 0) {
                    this.roleInfoDrawer.drawerStatus = true;
                } else {
                    let self = this;
                    this.$jr.rpc.callRpcWithParams(act.sn, act.ns, act.v, 'getAllRoleList', [])
                        .then((resp) => {
                            if (resp.code == 0 && resp.total > 0) {
                                this.allRoleList = resp.data.map((item)=>{
                                    return {key:item.roleId,label:item.name}
                                })
                            } else {
                                self.$Message.success(resp.msg);
                            }
                            this.roleInfoDrawer.drawerStatus = true;
                        }).catch((err) => {
                            window.console.log(err);
                    });
                }*/
            },

            doAddPermission: function () {
                this.permissionListDrawer.drawerStatus = false;
                let perms = [];
                let curSelNodes = this.$refs.plTree.getCheckedNodes();
                for (let k = 0; k <  curSelNodes.length; k++) {
                    if(curSelNodes[k].srcData) {
                        perms.push(curSelNodes[k].srcData.haCode);
                    }

                }

                let adds = [];
                if (this.role.pers) {
                    for (let i = 0; i < perms.length; i++) {
                        let f = false;
                        for (let j = 0; j < this.role.pers.length; j++) {
                            if (perms[i] == this.role.pers[j]) {
                                f = true;
                                break;
                            }
                        }
                        if (!f) {
                            adds.push(perms[i]);
                        }
                    }
                } else {
                    adds = perms;
                }

                let dels = [];
                if (this.role.pers) {
                    for (let i = 0; i < this.role.pers.length; i++) {
                        let f = false;
                        for (let j = 0; j < perms.length; j++) {
                            if (perms[j] == this.role.pers[i]) {
                                f = true;
                                break;
                            }
                        }

                        if (!f) {
                            dels.push(this.role.pers[i]);
                        }
                    }
                }

                if (adds.length > 0 || dels.length > 0) {
                    let self = this;
                    let sn = 'cn.jmicro.security.api.IServiceMethodListServiceJMSrv';
                    let ns = cons.NS_SECURITY;
                    let v = '0.0.1';
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateActPermissions',
                        [this.role.id, adds, dels])
                        .then((resp) => {
                            if (resp.code == 0) {
                                self.role.pers = perms;
                                self.role.permissionParseEntires = [];
                                self.openActInfoDrawer(self.role);
                            } else {
                                self.$Message.success(resp.msg);
                            }
                        }).catch((err) => {
                        window.console.log(err);
                    });
                }
            },

            addPermissions() {
                if(!this.srcPermissions || this.srcPermissions.length == 0) {
                    let self = this;
                    act.getAllPermissions()
                        .then((resp)=>{
                        if(resp.code == 0) {
                            self.srcPermissions = resp.data;
                            if(self.srcPermissions) {
                                self.curActParsedPermissions =  self.parsePermissionData();
                                if( self.curActParsedPermissions && self.curActParsedPermissions.length > 0) {
                                    self.permissionListDrawer.drawerStatus = true;
                                } else {
                                    self.$Message.success('Parse permission data error: ' + resp.data);
                                }
                            }
                        } else {
                            self.$Message.success(resp.msg);
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                } else {
                    this.curActParsedPermissions =  this.parsePermissionData(this.srcPermissions);
                    this.permissionListDrawer.drawerStatus = true;
                }
            },

            openActInfoDrawer(mi) {
                this.role = mi;

                if(this.role.permissionParseEntires && this.role.permissionParseEntires.length > 0) {
                    this.actInfoDrawer.drawerStatus = true;
                } else {
                    let self = this;
                    act.getPermissionsByActId(this.role.id).then((resp)=>{
                        if(resp.code == 0 && resp.data) {
                            self.role.permissionEntires = resp.data;
                            if(self.role.permissionEntires) {
                                self.parseActPermissionData();
                                if(!self.role.permissionParseEntires || self.role.permissionParseEntires.length == 0) {
                                    self.$Message.success('Parse act: '+self.role.actName+' permission data error: ' + JSON.stringify(resp.data));
                                }
                            }
                            self.actInfoDrawer.drawerStatus = true;
                        } else {
                            self.$Message.success(resp.msg);
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }
            },

            curPageChange(curPage){
                this.curPage = curPage;
                this.refresh();
            },

            delPermissions() {

            },

            parseActPermissionData() {
                this.curActParsedPermissions =  c.parseActPermissionData(this.role);
            },

            parsePermissionData() {
                return c.parsePermissionData(this);
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

            doQuery() {
                let self = this;
                let params = this.getQueryConditions();
                act.countAccount(params).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    } else {
                        self.totalNum = resp.data;
                        self.curPage = 1;
                        self.refresh();
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            refresh() {
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                if(this.$jr.rpcisAdmin()) {
                    let params = this.getQueryConditions();
                    act.getAccountList(params,this.pageSize,this.curPage-1).then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        let ll = resp.data;
                        self.actList = ll;
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }else {
                    self.actList = [];
                }
            },

            getQueryConditions() {
                return this.queryParams;
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.refresh);
            this.refresh();
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid, "menus":[{name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);
        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },

        filters: {
            formatDate: function(time) {
                // 后期自己格式化
                return new Date(time).format("yyyy/MM/dd hh:mm:ss S");
            }
        },

    }
</script>

<style>
    .JAccountEditor{
       min-height: 500px;
    }

    .JAccountEditor a {
        display: inline-block;
        margin-right: 8px;
    }

</style>