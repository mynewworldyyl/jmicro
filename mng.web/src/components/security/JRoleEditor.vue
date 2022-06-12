<template>
    <div class="JRoleEditor">

        <div v-if="isLogin && roleList && roleList.length > 0">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td>{{'ID'|i18n}}</td><td>{{'Name'|i18n}}</td><td>{{'Desc'|i18n}}</td>
				<td>{{'ClientId'|i18n}}</td>
                    <td>{{'CreatedBy'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td>
                    <td>{{"Operation"|i18n}}</td></tr>
                </thead>
                <tr v-for="c in roleList" :key="c._id">
                    <td>{{c.roleId}}</td> <td>{{c.name}}</td> <td>{{c.desc}}</td><td>{{c.clientId}}</td>
                    <td>{{c.createdBy}}</td><td>{{c.createdTime | formatDate(1)}}</td>
                    <td>
                        <a  @click="openActInfoDrawer(c)">{{"Permission"|i18n}}</a>
                        <a  @click="updateRoleDrawer(c)">{{"Update"|i18n}}</a>
                        <a  @click="openRoleAuthDrawer(c)">{{"Accounts"|i18n}}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && roleList && roleList.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin || !roleList || roleList.length == 0">
            No permission!
        </div>

        <!-- 当前角色权限-->
        <Drawer  v-model="actInfoDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                 :draggable="true" :scrollable="true" width="80">
            <div>
                <span>{{role.name}}</span>&nbsp;&nbsp;&nbsp;&nbsp;
                <a v-if="isLogin" @click="addPermissions()">{{'ConfigPermissions'|i18n}}</a>
            </div>

            <div style="position:relative;height:auto;margin-top:10px;">
                <Tree v-if="role && role.permissionParseEntires" :data="role.permissionParseEntires " class="actPermissionTree"></Tree>
            </div>
        </Drawer>

        <!-- 配置当前角色权限-->
        <Drawer ref="permissionListDrawer"  v-model="permissionListDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
            <div>
                <a v-if="isLogin" @click="doAddPermission()">{{'Confirm'|i18n}}</a>
            </div>
            <div>
                <Tree ref="plTree" v-if="curActParsedPermissions" :data="curActParsedPermissions" show-checkbox multiple class="permissionTree"></Tree>
            </div>
        </Drawer>

        <!--  创建 或 更新角色 -->
        <Drawer ref="addRole"  v-model="addRoleDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
            <div>
                <a v-if="isLogin" @click="doAddRole()">{{'Confirm'|i18n}}</a>
            </div>
            <div>
				<div>
					<Label v-if="isAdmin" for="forAllClient">{{'forAllClient'|i18n}}</Label>
					<Checkbox v-if="isAdmin"  id="forAllClient" v-model="forAllClient"></Checkbox>
				</div>
				
				
                <Label for="Name">{{'Name'|i18n}}</Label>
                <Input id="Name" v-model="role.name"/>

                <Label for="desc">{{'desc'|i18n}}</Label>
                <Input  id="desc" v-model="role.desc"/>
            </div>
            <div>{{errorMsg}}</div>
        </Drawer>

        <!--  角色授予给账号 -->
        <Drawer ref="roleAuth"  v-model="roleAuthDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
           <role-auth :role="role"></role-auth>
        </Drawer>

    </div>
</template>

<script>

    import act from "@/rpcservice/act"
    import cons from "@/rpc/constants"
    import c from "./c"
    import roleAuth from "./roleauth/role2actauth"

     const sn = 'cn.jmicro.security.api.IRoleServiceJMSrv';
     const ns = cons.NS_SECURITY;
     const v = '0.0.1';

    const cid = 'role';

    export default {
        name: cid,
        components: {
            roleAuth
        },
        data() {
            return {
				isAdmin:false,
                errorMsg:'',
                isLogin:false,
                roleList: [],
                queryParams:{},
                totalNum:0,
                pageSize:10,
                curPage:1,
				
				forAllClient:false,

                srcPermissions:[],
                curActParsedPermissions:[],

                updateModel:false,
                role : {},

                actInfoDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },

                permissionListDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {right:'0px',zindex:1001},
                },

                addRoleDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {right:'0px',zindex:1001},
                },

                roleAuthDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {right:'0px',zindex:1001},
                },

                selOptions:{

                },

            }
        },

        methods: {

            updateRoleDrawer(c) {
                this.updateModel = true;
                this.errorMsg = '';
                this.role = c;
                this.addRoleDrawer.drawerStatus = true;
            },

            openRoleAuthDrawer(c){
                this.errorMsg = '';
                this.role = c;
                this.roleAuthDrawer.drawerStatus = true;
            },

            doAddRole(){
                let self = this;
                self.errorMsg = '';
                let method = self.updateModel ? 'updateRole':'addRole';
				
				if(this.isAdmin && this.forAllClient) {
					this.role.clientId = -1
				}
				
                this.$jr.rpc.callRpcWithParams(sn, ns, v, method, [ this.role ])
                    .then((resp)=>{
                    if(resp.code == 0) {
                        self.refresh();
                        this.addRoleDrawer.drawerStatus = false;
                    } else {
                        self.errorMsg = resp.msg;
                    }
                }).catch((err)=>{
                    self.errorMsg = err;
                });
            },

            addRole() {
                this.updateModel = false;
                this.errorMsg = '';
                this.role = {};
                this.addRoleDrawer.drawerStatus = true;
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
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateRolePermissions',
                        [this.role.roleId, adds, dels])
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
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getRolePermissions', [ this.role.roleId ])
                        .then((resp)=>{
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

            refresh() {
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                if(this.isLogin) {
                    let params = this.getQueryConditions();
                    let self = this;
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listRoles', [params,this.pageSize,this.curPage-1])
                        .then((resp)=>{
                            if(resp.code == 0){
                                if(resp.total == 0) {
                                    console.log("success");
                                }else {
                                    self.roleList = resp.data;
                                    self.totalNum = resp.total;
                                    self.curPage = 1;
                                }

                            } else {
                                window.console.log(resp.msg);
                            }
                        }).catch((err)=>{
                        window.console.log(err);
                    });
                }else {
                    self.roleList = [];
                }
            },

            getQueryConditions() {
                return this.queryParams;
            },
        },

        mounted () {
			this.isAdmin = this.$jr.auth.isAdmin()
            //this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.refresh);
            this.refresh();
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid, "menus":[
                    {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh},
                    {name:"AddRole",label:"Add",icon:"ios-cog",call:self.addRole}]
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

    }
</script>

<style>
    .JRoleEditor{
      
    }

    .JRoleEditor a {
        display: inline-block;
        margin-right: 8px;
    }

</style>