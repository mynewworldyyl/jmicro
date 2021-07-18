<template>
    <div class="JClientConfig">

        <div v-if="isLogin && clientList && clientList.length> 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
                    <tr>
                        <td>{{'clientId' | i18n }}</td>
                        <td>{{'ownerId' | i18n }}</td>
                        <td>{{'Type' | i18n }}</td>
                        <td>{{'name' | i18n }}</td>
                        <td>{{'desc' | i18n }}</td>
                        <td>{{'createdTime' | i18n }}</td>
                        <td>{{'updatedTime' | i18n }}</td>
                        <td>{{'status' | i18n }}</td>
                        <td>{{'Operation' | i18n }}</td>
                    </tr>
                </thead>
                <tr v-for="c in clientList" :key="c.id">
                    <td>{{c.clientId}}</td>
                    <td>{{c.ownerId}}</td>
                    <td>{{c.type}}</td>
                    <td>{{c.name}}</td>
                    <td>{{c.desc}}</td>
                    <td>{{c.createdTime | formatDate(1)}}</td>
                    <td>{{c.updatedTime | formatDate(1)}}</td>
                    <td>{{STATUS[c.status]}}</td>
                    <td>
                        <a v-if="isLogin" @click="updateClientDrawer(c)">{{'Update' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && c.status == 1" @click="changeStatus(c.id,5)">{{'Reject' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && c.status == 2" @click="changeStatus(c.id,3)">{{'Freeze' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && c.status == 3" @click="changeStatus(c.id,2)">{{'Unfreeze' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && clientList && clientList.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin" >{{msg}}</div>

        <div v-if="isLogin  && (!clientList || clientList.length == 0)" >{{msg}}</div>

        <!--  创建 或 更新 -->
        <Drawer  ref="addClient"  v-model="addClientDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
            <div class="addClientcls">
                <a v-if="actInfo" @click="doAddClient()">{{'确认提交'|i18n}}</a>
                <a v-if="actInfo && actInfo.id == client.ownerId && !hideRefresh" @click="refreshToken(client.clientId)">
                    {{'刷新令牌' | i18n }}</a>
                <a v-if="actInfo && actInfo.id == client.ownerId  && hideRefresh" @click="doRefreshToken(client.clientId)">
                    {{'确认刷新' | i18n }}</a>
                <a v-if="actInfo && actInfo.id == client.ownerId  && hideRefresh" @click="cancelRefreshToken()">
                    {{'取消刷新' | i18n }}</a>
                <a v-if="actInfo && actInfo.id == client.ownerId" @click="setAsDefaultClient(client.clientId)">{{'默认租户'}}</a>
                <a v-if="actInfo && actInfo.id == client.ownerId && !hideResend" @click="resendToken(client.clientId)">{{'重发令牌'}}</a>
                <a v-if="actInfo && actInfo.id == client.ownerId && hideResend" @click="doResendToken(client.clientId)">{{'确认重发'}}</a>
            </div>
            <div>

                <Label v-if="doVcode" for="vcode">{{'vcode'|i18n}}</Label>
                <Input  v-if="doVcode"  id="vcode" v-model="vcode"/>

                <Label for="ownerId">{{'ownerId'|i18n}}</Label>
                <Input  id="ownerId" v-model="client.ownerId"/>

                <Label for="type">{{'type'|i18n}}</Label>
                <Input id="type" v-model="client.type"/>

                <Label for="Name">{{'Name'|i18n}}</Label>
                <Input id="Name" v-model="client.name"/>

                <Label for="desc">{{'desc'|i18n}}</Label>
                <Input  id="desc" v-model="client.desc"/>
            </div>

            <div>{{errMsg}}</div>
        </Drawer>

        <Modal title="令牌信息" v-model="tokenDialog" :mask-closable="false" @on-visible-change="onOpenClose">
            {{ tokenContent }}
        </Modal>

    </div>
</template>

<script>

    import {Constants} from "@/rpc/message"
    import rpc from "@/rpc/rpcbase"
/*    import comm from "@/rpcservice/comm"
    import utils from "@/rpc/utils"*/
    import cons from "@/rpc/constants"

    const STATUS_APPLY = 1;
    const STATUS_NORMAL = 2;
    const STATUS_FREEZE = 3;
    const STATUS_DELETE = 4
    const STATUS_REJECT = 5;

    const STATUS = {}
    STATUS[STATUS_APPLY] = 'Apply'
    STATUS[STATUS_NORMAL] = 'Normal'
    STATUS[STATUS_FREEZE] = 'Freeze'
    STATUS[STATUS_DELETE] = 'Delete'

    const cid = 'clientConfig';

    const sn = 'cn.jmicro.security.api.IClientServiceJMSrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    export default {
        name: cid,
        components: {
        },

        data() {
            return {
                msg:'',
                STATUS:STATUS,

                actInfo:null,

                totalNum:0,
                pageSize:10,
                curPage:1,

                client:{},
                isLogin:false,
                clientList: [],
                errMsg:'',

                updateModel: false,
                tokenDialog: false,
                tokenContent: '',

                vcode: null,
                doVcode:false,
                hideRefresh:false,
                hideResend:false,

                addClientDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },

            }
        },

        methods: {
            onOpenClose(flag) {
                if(!flag) {
                    this.tokenContent =""
                    this.tokenDialog = false
                }
            },

            setAsDefaultClient(clientId) {
                this.callRemote('setAsDefaultClient',[clientId],()=>{
                    this.refresh();
                    this.$Modal.info({
                        title: '提示',
                        content: "设置成功",
                    })

                })
            },

            cancelRefreshToken() {
                this.hideRefresh = false
                this.doVcode = false
            },

            refreshToken() {
                this.$Modal.confirm({
                    title: '警告',
                    content: '<p>刷新令牌将使原来令牌失效，需要用新令牌替换旧令牌，然后重启系统生效，请谨慎操作！</p>',
                    onOk: () => {
                        rpc.getCode(2).then((resp) => {//取邮件验证码
                            if (resp.code != 0) {
                                this.$Notice.warning({
                                    title: 'Error',
                                    desc: resp.msg
                                });
                            }else {
                                this.hideRefresh = true
                                this.doVcode = false
                            }
                        }).catch((err) => {
                            this.$Notice.warning({
                                title: 'Error',
                                desc: JSON.stringify(err)
                            });
                        });
                    },
                    onCancel: () => {

                    }
                });
            },

            resendToken() {
                this.vcode = null;
                rpc.getCode(2).then((resp) => {//取邮件验证码
                    if (resp.code != 0) {
                        this.$Notice.warning({
                            title: 'Error',
                            desc: resp.msg
                        });
                    } else {
                        this.hideResend = true;
                        this.doVcode = true
                    }
                }).catch((err) => {
                    this.$Notice.warning({
                        title: 'Error',
                        desc: JSON.stringify(err)
                    });
                });
            },

            doResendToken(clientId) {
                this.callRemote('resendToken',[clientId,this.vcode],(resp)=>{
                    this.hideResend = false
                    this.doVcode = false
                    this.vcode = null;
                    this.$Modal.info({
                        title: '令牌信息',
                        content: ", 令牌已成功发送到你注册邮箱，请注意查收，确保勿泄露给无关人员！",
                    })
                })
            },

            doRefreshToken (clientId) {

                this.callRemote('refreshToken',[clientId,this.vcode],(resp)=>{
                    this.hideRefresh = true
                    this.doVcode = false
                    this.$Modal.info({
                        title: '令牌信息',
                        content: resp.data+", 令牌同时发送到你注册邮箱，请注意保管，确保勿泄露给无关人员！",
                    })
                })
            },

            changeStatus(clientId,status) {
                this.callRemote('updateClientStatus',[this.client.clientId, status],()=>{
                    this.refresh()
                })
            },

            pageSizeChange(pageSize) {
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

            curPageChange(curPage) {
                this.curPage = curPage
                this.refresh()
            },

            updateClientDrawer(c) {
                this.updateModel = true
                this.errMsg = ''
                this.client = c
                this.addClientDrawer.drawerStatus = true
            },

            doAddClient() {
                let self = this
                self.errMsg = ''
                if(self.updateModel) {
                    this.callRemote('updateClient',[this.client],()=>{
                        self.refresh()
                        this.addClientDrawer.drawerStatus = false
                    })
                }else {
                    this.callRemote('addClient',[this.client],()=>{
                        self.refresh();
                        this.addClientDrawer.drawerStatus = false
                    })
                }
            },

            addClient() {
                this.updateModel = false;
                this.errMsg = '';
                this.client = {};
                this.addClientDrawer.drawerStatus = true;
            },

            refresh() {
                let self = this;
                this.actInfo = rpc.actInfo
                this.isLogin = rpc.isLogin()
                if(this.isLogin) {
                    let params = this.getQueryConditions();
                    let self = this;
                    this.callRemote('listClients',[params,self.pageSize,self.curPage-1],(resp)=>{
                        self.clientList = resp.data;
                        self.totalNum = resp.total;
                        self.curPage = 1;
                    })
                } else {
                    self.clientList = [];
                    this.$Notice.warning({
                        title: 'Error',
                        desc: '未登录',
                    });
                }
            },

            getQueryConditions() {
                return this.queryParams;
            },

            callRemote(method,args,sucCb,failCb) {
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, method, args)
                    .then((resp) => {
                    if (resp.code == 0 ) {
                        if(sucCb) {
                            sucCb(resp);
                        }
                    } else {
                        if(failCb) {
                            failCb(resp,resp.msg);
                        } else {
                            this.$Notice.warning({
                                title: 'Error',
                                desc: resp.msg
                            });
                        }
                    }
                }).catch((err) => {
                    if(failCb) {
                        failCb(null,err);
                    } else {
                        this.$Notice.warning({
                            title: 'Error',
                            desc: JSON.stringify(err)
                        });
                    }
                });
            },
        },

        mounted () {

            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            rpc.addActListener(cid,()=>{
                this.refresh();
            });
            let self = this;

            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"Add",label:"Add",icon:"ios-cog",call:self.addClient},
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}
                        ]
                });

            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            this.refresh();

            window.jm.vue.$on('editorClosed',ec);
        },

        beforeDestroy() {
            rpc.removeActListener(cid);
        },

    }
</script>

<style>
    .JResourceConfigView{
    }

    .addClientcls a {
        display: inline-block;
        padding-right: 10px;
    }
</style>