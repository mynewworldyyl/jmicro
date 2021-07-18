<template>
    <div class="JLogWarningConfigView" style="position:relative;height:auto">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr style="width:30px">
                    <td>{{'ID' | i18n }}</td>
                    <td>{{'Enable' | i18n }}</td>
                    <td style="width:56px">{{'ClientId' | i18n }}</td>
                    <td style="width:30px">{{'Interval' | i18n }}</td>
                    <td>{{'Exp' | i18n }}</td>
                    <td style="width:36px">{{'Type' | i18n }}</td>
                    <td>{{'Params' | i18n }}</td>
                    <td>{{'Tag' | i18n }}</td>
                    <td>{{'Operation' | i18n }}</td>
                </tr></thead>
                <tr v-for="c in logList" :key="c.id">
                    <td>{{c.id}}</td>
                    <td>{{c.enable}}</td>
                    <td>{{c.clientId}}</td>
                    <td>{{c.minNotifyInterval}}</td>
                    <td>{{c.expStr}}</td>
                    <td>{{type2str[c.type]}}</td>
                    <td>{{c.cfgParams}}</td>
                    <td>{{c.tag}}</td>
                    <td>
                        <a v-if="isLogin" @click="update(c)">{{'Update' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin" @click="remove(c.id)">{{'Delete' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                    </td>
                </tr>
            </table>
        </div>

        <!--<div v-if="isLogin  && logList && logList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>-->

        <div v-if="!isLogin" >Not login</div>

        <div v-if="isLogin  && (!logList || logList.length == 0)" >No data</div>

        <Modal v-model="addWarningConfigDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="doSave()">
            <div>
                <div style="color:red">{{errMsg}}</div>

                <Checkbox v-model="cfg.enable">ENABLE</Checkbox><br/>

                <Label for="type">{{'Type' | i18n}}</Label>
                <Select id="type" v-model="cfg.type">
                    <Option v-for="k in [1,2,3,4]" :value="k" :key="k">{{type2str[k]}}</Option>
                </Select>

                <Label for="minNotifyInterval">{{'Interval' | i18n}}</Label>
                <Input id="minNotifyInterval" v-model="cfg.minNotifyInterval"/>

               <!-- <Label v-if="cfg.type == 1" for="service">{{'Service' | i18n}}</Label>
                <Input v-if="cfg.type == 1"  id="service" v-model="cfg.service"/>-->

                <Label v-if="cfg.type == 1"  for="namespace">{{'Namespace' | i18n}}</Label>
                <Input v-if="cfg.type == 1"  id="namespace" v-model="cfg.namespace"/>

                <Label v-if="cfg.type == 1"  for="version">{{'Version' | i18n}}</Label>
                <Input v-if="cfg.type == 1"  id="version" v-model="cfg.version"/>

                <Label v-if="cfg.type == 2 "  for="db">{{'Table' | i18n}}</Label>
                <Input v-if="cfg.type == 2 "  id="db" v-model="cfg.cfgParams"/>

                <Label v-if=" cfg.type == 4"  for="fileName">{{'File' | i18n}}</Label>
                <Input v-if=" cfg.type == 4"  id="fileName" v-model="cfg.cfgParams"/>

                <Label  for="tag">{{'Tag' | i18n}}</Label>
                <Input  id="tag" v-model="cfg.tag"/>

                <Label v-if="isAdmin"  for="clientId">{{'clientId' | i18n}}</Label>
                <Input v-if="isAdmin"  id="clientId" v-model="cfg.clientId"/>

                <Label for="exp">{{'Exp' | i18n}}</Label>
                <Input id="exp"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                       type="textarea" v-model="cfg.expStr"/>

            </div>
        </Modal>

    </div>
</template>

<script>

    import cons from "@/rpc/constants"
    import rpc from "@/rpc/rpcbase"
    
    const cid = 'warningConfig';

    const sn = 'cn.jmicro.mng.api.ILogWarningConfigJMSrv';
    const ns = cons.NS_MNG;
    const v = '0.0.1';

    //const LOGS = ['No','Trance','Debug','Info','Warn','Error','Final'];

    export default {
        name: cid,
        data() {
            return {
                type2str:{1:'RPC方法',2:'库表名',3:'控制台',4:'文件'},
                isLogin:false,
                isAdmin:false,
                logList: [],
                errMsg:'',
                //logLevel2Label:LOGS,

                curLogId:-1,
                cfg:{},
                updateMode: false,
                addWarningConfigDialog:false,
            }
        },

        components: {

        },

        methods: {

            add() {
                this.updateMode=false;
                this.cfg = {clientId:rpc.actInfo.id};
                this.addWarningConfigDialog = true;
            },

            remove(id) {
                let self = this;
                rpc.callRpcWithParams(sn,ns,v, 'delete', [id])
                    .then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                       for(let i = 0; i < self.logList.length; i++) {
                           if(self.logList[i].id == id) {
                               self.logList.splice(i,1);
                               break;
                           }
                       }
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            update(cfg) {
                this.updateMode=true;
                this.cfg = cfg;

                if(cfg.type == 1 && cfg.cfgParams) {
                    let arr = cfg.cfgParams.split("##");
                    if(arr.length == 3) {
                        this.cfg.service=arr[0];
                        this.cfg.namespace=arr[1];
                        this.cfg.version=arr[2];
                    }
                }

                this.addWarningConfigDialog = true;
            },

            doSave() {
                let self = this;

                if(!self.cfg.expStr || self.cfg.expStr.length == 0) {
                    self.errMsg = '表达式不能为空';
                    return;
                }

                if(self.cfg.type == 1) {
                    if(!self.cfg.namespace || self.cfg.namespace.length == 0) {
                        self.errMsg = '名称空间不能为空';
                        return;
                    }

                    if(!self.cfg.version || self.cfg.version.length == 0) {
                        self.errMsg = '版本不能为空';
                        return;
                    }
                    self.cfg.service = "cn.jmicro.api.monitor.ILogWarningJMSrv";
                    self.errMsg = '';
                    self.cfg.cfgParams = self.cfg.service+'##' +self.cfg.namespace+'##'+self.cfg.version
                        +'########warn##cn.jmicro.api.monitor.MRpcLogItemJRso';
                }else if(self.cfg.type == 2 || self.cfg.type == 4) {
                    if(!self.cfg.cfgParams || self.cfg.cfgParams.length == 0) {
                        self.errMsg =  self.cfg.type == 2 ? '数据库表名不能为空':'文件名不能为空';
                        return;
                    }
                }

                if(!this.updateMode) {
                    rpc.callRpcWithParams(sn,ns,v, 'add', [self.cfg])
                        .then((resp)=>{
                            if(resp.code != 0) {
                                self.$Message.success(resp.msg);
                                return;
                            }
                            this.addWarningConfigDialog = false;
                            self.logList.push(resp.data);
                           // resp.data.levelLabel = LOGS[resp.data.level];

                            delete self.cfg.service;
                            delete self.cfg.namespace;
                            delete self.cfg.version;

                        }).catch((err)=>{
                        window.console.log(err);
                    });
                }else {
                    rpc.callRpcWithParams(sn,ns,v, 'update', [self.cfg])
                        .then((resp)=>{
                            if(resp.code != 0) {
                                self.$Message.success(resp.msg);
                                return;
                            }

                            this.addWarningConfigDialog = false;
                            delete self.cfg.service;
                            delete self.cfg.namespace;
                            delete self.cfg.version;

                            //self.cfg.levelLabel = LOGS[self.cfg.level];

                        }).catch((err)=>{
                        window.console.log(err);
                    });
                }
            },

            refresh() {
                let self = this;
                this.isLogin = rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }

                this.isAdmin = rpc.isAdmin();

                rpc.callRpcWithParams(sn,ns,v, 'query', [])
                .then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }

                    let ll = resp.data;
                    self.logList = ll;

                }).catch((err)=>{
                    window.console.log(err);
                });
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            rpc.addActListener(cid,this.refresh);
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"Add",label:"Add",icon:"ios-cog",call:self.add},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}
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
    .JLogWarningConfigView{
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

</style>