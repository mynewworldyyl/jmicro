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
                <Tree v-if="curAct && curAct.permissionParseEntires" :data="curAct.permissionParseEntires " class="actPermissionTree"></Tree>
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

    </div>
</template>

<script>

    //import treeTable from '../treetable/LinkLogTreeTable.vue'

    const cid = 'account';

    export default {
        name: cid,
        components: {
            //treeTable,
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

                curAct : null,

                actInfoDrawer: {
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

            callMethod1() {
            },

            callMethod2() {
            },

            resendActiveEmail(c) {
                let self = this;
                window.jm.mng.act.resendActiveEmail(c.actName).then((resp) => {
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
                window.jm.mng.act.changeAccountStatus(act.actName).then((resp) => {
                    if (resp.code == 0) {
                        self.refresh();
                    } else {
                        self.$Message.success(resp.msg);
                    }
                }).catch((err) => {
                    window.console.log(err);
                });
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
                if (this.curAct.pers) {
                    for (let i = 0; i < perms.length; i++) {
                        let f = false;
                        for (let j = 0; j < this.curAct.pers.length; j++) {
                            if (perms[i] == this.curAct.pers[j]) {
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
                if (this.curAct.pers) {
                    for (let i = 0; i < this.curAct.pers.length; i++) {
                        let f = false;
                        for (let j = 0; j < perms.length; j++) {
                            if (perms[j] == this.curAct.pers[i]) {
                                f = true;
                                break;
                            }
                        }

                        if (!f) {
                            dels.push(this.curAct.pers[i]);
                        }
                    }
                }

                if (adds.length > 0 || dels.length > 0) {
                    let self = this;
                    let sn = 'cn.jmicro.security.api.IServiceMethodListService';
                    let ns = window.jm.Constants.NS_SECURITY;
                    let v = '0.0.1';
                    window.jm.rpc.callRpcWithParams(sn, ns, v, 'updateActPermissions',
                        [this.curAct.actName, adds, dels])
                        .then((resp) => {
                            if (resp.code == 0) {
                                self.curAct.pers = perms;
                                self.curAct.permissionParseEntires = [];
                                self.openActInfoDrawer(self.curAct);
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
                    window.jm.mng.act.getAllPermissions()
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
                this.curAct = mi;

                if(this.curAct.permissionParseEntires && this.curAct.permissionParseEntires.length > 0) {
                    this.actInfoDrawer.drawerStatus = true;
                } else {
                    let self = this;
                    window.jm.mng.act.getPermissionsByActName(this.curAct.actName).then((resp)=>{
                        if(resp.code == 0 && resp.data) {
                            self.curAct.permissionEntires = resp.data;
                            if(self.curAct.permissionEntires) {
                                self.parseActPermissionData();
                                if(!self.curAct.permissionParseEntires || self.curAct.permissionParseEntires.length == 0) {
                                    self.$Message.success('Parse act '+self.curAct.actName+'permission data error: ' + resp.data);
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
                let pl = [];
                if(!this.curAct.pers) {
                    this.curAct.pers= [];
                }
                for(let modelKey in this.curAct.permissionEntires ) {
                    let srcPs = this.curAct.permissionEntires[modelKey];
                    if(!srcPs || srcPs.length == 0) {
                        continue;
                    }

                    let children = [];
                    pl.push({ title: modelKey, children:children});

                    for(let i = 0; i < srcPs.length; i++) {
                        let sp = srcPs[i];
                        this.curAct.pers.push(sp.haCode);
                        let e = {
                            title : sp.label,
                            expanded : true,
                            srcData : sp,
                            render: (h) => {
                                return h('span',[
                                    h('span',{
                                        style:{ marginLeft: '10px' }
                                    },sp.label)

                                ]);
                            }
                        }
                        children.push(e);
                    }
                }
                this.curAct.permissionParseEntires = pl;
                this.curActParsedPermissions =  pl;
            },

            parsePermissionData() {
                let pl = [];
                let self = this;
                for(let modelKey in self.srcPermissions) {
                    let srcPs = self.srcPermissions[modelKey];
                    if(!srcPs || srcPs.length == 0) {
                        continue;
                    }

                    let children = [];
                    pl.push({ title: modelKey, children:children});

                    let isCheck = (haCode) => {
                        if(self.curAct.pers) {
                            for(let i = 0; i < self.curAct.pers.length; i++) {
                                if(haCode == self.curAct.pers[i]) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }

                    for(let i = 0; i < srcPs.length; i++) {
                        let sp = srcPs[i];
                        let e = {
                            title: sp.label,
                            srcData : sp,
                            expand: true,
                            checked: isCheck(sp.haCode),
                            render: (h/*,params*/) => {
                                return h('span',[
                                    h('span',{
                                        style:{ marginLeft: '10px' }
                                    },sp.label)

                                ]);
                            }
                        }
                        children.push(e);
                    }
                }
                return pl;
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

            doQuery() {
                let self = this;
                let params = this.getQueryConditions();
                window.jm.mng.act.countAccount(params).then((resp)=>{
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
                this.isLogin = window.jm.rpc.isLogin();
                if(window.jm.rpc.isAdmin()) {
                    let params = this.getQueryConditions();
                    window.jm.mng.act.getAccountList(params,this.pageSize,this.curPage-1).then((resp)=>{
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
            window.jm.rpc.addActListener(cid,this.refresh);
            this.refresh();
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid, "menus":[{name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);
        },

        beforeDestroy() {
            window.jm.rpc.removeActListener(cid);
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

    #queryTable td {
        padding-left: 8px;
    }

    .drawerJinvokeBtnStatu{
        position: fixed;
        left: 0px;
        top: 30%;
        bottom: 30%;
        height: 39%;
        width: 1px;
        border-left: 1px solid lightgray;
        background-color: lightgray;
        border-radius: 3px;
        z-index: 1000000;
    }

    .configItemTalbe td {
        text-align: center;
    }

    .permissionTree{
        width: 100%;
    }

    .actPermissionTree{

    }


</style>