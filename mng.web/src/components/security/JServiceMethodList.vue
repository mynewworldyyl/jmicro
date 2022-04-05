<template>
    <div class="JServiceMethodList">

        <table v-if="isLogin  && keyList && keyList.length > 0" class="configItemTalbe" width="99%">
            <thead><tr><td style="width:500px">{{'Service'|i18n}}</td><td>{{'Method'|i18n}}</td>
				<td>{{'haCode'|i18n}}</td>
                <!--<td style="width:150px">{{'Hash'|i18n}}</td>-->
                <td style="width:70px">{{'ClientId'|i18n}}</td>
                <td>{{'Namespace'|i18n}}</td><td style="width:70px">{{'Version'|i18n}}</td>
                <td style="width:60px">{{'OnLine'|i18n}}</td>
				<td style="width:60px">{{'External'|i18n}}</td>
                <td>{{'UpdatedTime'|i18n}}</td><td>{{'Operation'|i18n}}</td>
            </tr>
            </thead>
            <tr v-for="a in keyList" :key="a.id">
                <td>{{a.serviceName}}</td><td>{{a.method}}</td> <td>{{a.haCode}}</td><td>{{a.clientId}}</td>
                <td>{{a.namespace}}</td><td>{{a.version}}</td><td>{{a.online}}</td><td>{{a.external}}</td>
				<td>{{a.updatedTime|formatDate(2)}}</td>
                <td>&nbsp;
                    <a @click="openDetailDrawer(a)"> {{'Detail' | i18n}} </a>
                    <a v-if="a.perType" @click="openAuthDrawer(a)"> {{'Auth' | i18n}} </a>
                </td>
            </tr>
        </table>

        <div v-if="isLogin  && keyList && keyList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10,20,50,100,150,200]"></Page>
        </div>

        <div v-if="!isLogin">No permission</div>
        <div v-if="isLogin  && (!keyList || keyList.length == 0)">
            No data
        </div>

        <Drawer  v-if="isLogin"  v-model="authClientDrawer.drawerStatus" :closable="false" placement="right"
                 :transfer="true" :draggable="true" :scrollable="true" width="70" @close="closeAuthDrawer()">
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
        </Drawer>

        <Drawer  v-if="isLogin"  v-model="detailDrawer.drawerStatus" :closable="false" placement="right"
                 :transfer="true" :draggable="true" :scrollable="true" width="50" @close="closeDetailDrawer()">
            <table class="detailTable" width="95%">
                <tr>
                    <td>{{"Online"|i18n}}</td><td>{{cm.online}}</td>
                    <td>{{"PerType"|i18n}}</td><td>{{cm.perType}}</td>
                </tr>
                <tr>
                    <td>{{"ServiceName"|i18n}}</td><td colspan="3">{{cm.serviceName}}</td>
                </tr>
                <tr>
                    <td>{{"Namespace"|i18n}}</td><td>{{cm.namespace}}</td>
                    <td>{{"Version"|i18n}}</td><td>{{cm.version}}</td>
                </tr>
                <tr>
                    <td>{{"Method"|i18n}}</td><td>{{cm.method}}</td>
                    <td>{{"Hash"|i18n}}</td><td>{{cm.haCode}}</td>
                </tr>
                <tr>
                    <td>{{"CreatedTime"|i18n}}</td><td>{{cm.createdTime}}</td>
                    <td>{{"UpdatedTime"|i18n}}</td><td>{{cm.updateTime}}</td>
                </tr>
                <tr>
                    <td>{{"ParamType"|i18n}}</td><td colspan="3">{{cm.paramsDescs}}</td>
                </tr>
                <tr>
                    <td>{{"ReturnType"|i18n}}</td><td>{{cm.returnTypeDesc}}</td>
                    <td>{{"ClientId"|i18n}}</td><td>{{cm.clientId}}</td>
                </tr>

            </table>
        </Drawer>

        <div v-if="isLogin"  :style="query.drawerBtnStyle" class="queryDrawerStatu" @mouseenter="openQueryDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="query.drawerStatus" :closable="false" placement="left"
                :transfer="true" :draggable="true" :scrollable="true" width="50">
            <div><i-button @click="doQueryResource()">{{'Confirm'|i18n}}</i-button></div>
            <table>
                <tr>
                    <td>{{"ServiceName"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.serviceName"/>
                    </td>
                    <td>{{"Namespace"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.namespace"/>
                    </td>
                </tr>
                <tr>
                    <td>{{"Version"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.version"/>
                    </td>
                    <td>{{"Online"|i18n}}</td>
                    <td>
                        <Select v-model="queryParams.online">
                            <Option value="">{{"none" | i18n}}</Option>
                            <Option value="true">{{"true"|i18n}}</Option>
                            <Option value="false" >{{"false" | i18n}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>{{"Method"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.method"/>
                    </td>
                    <td>{{"Hash"|i18n}}</td>
                    <td>
                        <Input v-model="queryParams.haCode"/>
                    </td>
                </tr>

                <tr>
                    <td>{{"clientId"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.clientId"/>
                    </td>
                    <td>{{"ActName"|i18n}}</td>
                    <td>
                        <Input v-model="queryParams.actName"/>
                    </td>
                </tr>

                <tr>
                    <td>{{"PerType"|i18n}}</td>
                    <td>
                        <Select v-model="queryParams.perType">
                            <Option value="">{{"none" | i18n}}</Option>
                            <Option value="true">{{"true"|i18n}}</Option>
                            <Option value="false" >{{"false" | i18n}}</Option>
                        </Select>
                    </td>
                    <td>{{"Version"|i18n}}</td>
                    <td>
                        <!--<Input  v-model="queryParams.version"/>-->
                    </td>
                </tr>

            </table>
        </Drawer>

        <Modal
                v-model="auth.showDescDialog"
                title="备注"
                @on-ok="descAuthOk"
                @on-cancel="descAuthCancel">
            <Input v-model="auth.desc"></Input>
        </Modal>

    </div>
</template>

<script>

    import cons from "@/rpc/constants"

    const cid = 'serviceMethodList';
    const sn = 'cn.jmicro.security.api.IServiceMethodListServiceJMSrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    export default {
        name: 'JServiceMethodList',
        data () {
            return {
                authStatus:{"1":'Apply',"2":"Reject","3":"Approve","4":"Revoke"},
                cm:{},
                keyList:[],
                isLogin : false,
                act:null,
                totalNum:0,
                pageSize:20,
                curPage:1,
                dicts:{},

                queryParams:{},

                errMsg:'',

                detailDrawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                authClientDrawer :{
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                query: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },

                auth:{
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

            }
        },
        methods: {

            refreshAuthList(){
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getAuthByServiceMethodCode', [this.cm.haCode])
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
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addAuthByClientId', [this.cm.haCode,this.auth.clientId])
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
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addAuthByAccount', [this.cm.haCode,this.auth.accountName])
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
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateAuthStatus',
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

            doQueryResource(){
                //let qry = this.queryParams;
                this.curPage = 1;
                this.refresh();
            },

            curPageChange(curPage){
                this.curPage = curPage;
                this.refresh();
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

            openQueryDrawer(){
                this.query.drawerStatus = true;
                this.query.drawerBtnStyle.zindex = 10000;
                this.query.drawerBtnStyle.left = '0px';
            },

            openDetailDrawer(cm) {
                this.cm = cm;
                this.errMsg = "";
                this.detailDrawer.drawerStatus = true;
            },

            closeDetailDrawer() {
                this.detailDrawer.drawerStatus = false;
                this.detailDrawer.drawerBtnStyle.zindex = 100;
            },

            refresh() {
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                this.act = this.$jr.auth.actInfo;
                let qry = this.queryParams;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getMethodList', [qry,this.pageSize,this.curPage])
                    .then((resp) => {
                        if (resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        self.totalNum = resp.total;
                        self.keyList = resp.data;
                       /* if(self.keyList) {
                            self.keyList.forEach((e)=>{
                                e.authList = [];
                            })
                        }*/
                    }).catch((err) => {
                    window.console.log(err);
                });
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            //has admin permission, only control the show of the button
            this.$jr.auth.addActListener(this.refresh);
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });
            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);

            self.refresh();
        },
    }
</script>

<style>
    .JServiceMethodList{
        height:auto;
    }
    .queryDrawerStatu{
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

    .detailTable {

    }

    .detailTable tr td{
        padding-right:10px;
    }

</style>