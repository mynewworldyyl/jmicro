<template>
    <div class="JResourceConfigView">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
                    <tr>
                        <td>{{'ID' | i18n }}</td>
                        <td>{{'Resource' | i18n }}</td>
                        <td>{{'InstanceName' | i18n }}</td>
                        <td>{{'Interval' | i18n }}</td>
                        <td>{{'ToType' | i18n }}</td>
                        <td>{{'ToParams' | i18n }}</td>
                        <td>{{'ExtParams' | i18n }}</td>
                        <td>{{'Exp0' | i18n }}</td>
                        <td>{{'createdByAct' | i18n }}</td>
                        <td>{{'Enable' | i18n }}</td>
                        <td>{{'Operation' | i18n }}</td>
                    </tr>
                </thead>
                <tr v-for="c in logList" :key="c.id">
                    <td>{{c.id}}</td>
                    <td>{{c.resName}}</td>
                    <td>{{c.monitorInsName}}</td>
                    <td>{{c.t}}</td>
                    <td>{{toTypes[c.toType]}}</td>
                    <td>{{c.toParams}}</td>
                    <td>{{c.extParams}}</td>
                    <td>{{c.expStr}}</td>
                    <td>{{c.createdByAct}}</td>
                    <td>{{c.enable}}</td>
                    <td>
                        <a v-if="isLogin && !c.enable" @click="update(c)">{{'Update' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin && !c.enable" @click="remove(c.id)">{{'Delete' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && c.enable" @click="enable(c.id)">{{'Disable' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && !c.enable" @click="enable(c.id)">{{'Enable' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin" @click="view(c)">{{'View' | i18n }}</a>&nbsp;&nbsp;
                    </td>
                </tr>
            </table>
        </div>

        <!--<div v-if="isLogin  && logList && logList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>-->

        <div v-if="!isLogin" >{{msg}}</div>

        <div v-if="isLogin  && (!logList || logList.length == 0)" >{{msg}}</div>

        <Drawer v-model="addConfigDialog" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="80">

            <Button v-if="!readonly" @click="doSave()">{{'Confirm'|i18n}}</Button><br/>
            <Label v-if="errMsg"  style="color:red">{{errMsg}}</Label><br/>

            <Label for="resourceNames">{{'resourceNames' | i18n}}</Label>
            <!--<Input v-if="byKeyShow.sn"  id="ByService" v-model="byKey.sn"/>-->
            <Select :disabled="readonly" id="resourceNames" :filterable="true"
                    ref="resourceNames" :label-in-value="true" v-model="cfg.resName"
            @change="resourceNameChange">
                <!--  <Option value="*" >none</Option>-->
                <Option v-for="(v) in resourceNames"  :value="v"  v-bind:key="v">{{v}}</Option>
            </Select>

            <Label for="monitorInsName">{{'monitorInsName' | i18n}}</Label>
            <Select :disabled="readonly" id="monitorInsName" v-model="cfg.monitorInsName"
                    filterable allow-create>
                <Option value="*" >{{'All' | i18n}}</Option>
                <Option v-for="key in allInstances" :value="key" :key="key">{{key | i18n}}</Option>
            </Select>

            <Label for="period">{{'period' | i18n}}({{'MS' | i18n}})</Label>
            <Input :disabled="readonly"  id="period" v-model="cfg.t"/>

            <Label for="toType">{{'toType' | i18n}}</Label>
            <Select :disabled="readonly" id="toType" v-model="cfg.toType">
                <Option v-for="(key,v) in toTypes" :value="v" :key="key">{{key | i18n}}</Option>
            </Select>

            <Label v-if="toKeyShow.sn"  for="ToService">{{'ToService' | i18n}}</Label>
            <!--<Input v-if="toKeyShow.sn"  id="ToService" v-model="smToKey.sn"/>-->
            <Select :disabled="readonly" v-if="toKeyShow.sn"   id="ToService" :filterable="true"
                    ref="ToService" :label-in-value="true" v-model="smToKey.sn">
                <Option value="*" >none</Option>
                <Option v-for="v in serviceNames"  :value="v"  v-bind:key="v">{{v}}</Option>
            </Select>

            <Label v-if="toKeyShow.sn"  for="ToNamespace">{{'ToNamespace' | i18n}}</Label>
            <!--<Input v-if="toKeyShow.sn"  id="ToNamespace" v-model="smToKey.ns"/>-->
            <Select :disabled="readonly" v-if="toKeyShow.sn"   id="ToNamespace" :filterable="true"
                    ref="ToNamespace" :label-in-value="true" v-model="smToKey.ns">
                <Option value="*" >none</Option>
                <Option v-for="(v) in toCurNamespaces"  :value="v"  v-bind:key="v">{{v}}</Option>
            </Select>

            <Label v-if="toKeyShow.sn"  for="ToVersion">{{'ToVersion' | i18n}}</Label>
            <!-- <Input v-if="toKeyShow.sn"  id="ToVersion" v-model="smToKey.ver"/>-->
            <Select :disabled="readonly" v-if="toKeyShow.sn"   id="ToVersion" :filterable="true"
                    ref="ToVersion" :label-in-value="true" v-model="smToKey.ver">
                <Option value="*" >none</Option>
                <Option v-for="(v) in toCurVersions"  :value="v"  v-bind:key="v">{{v}}</Option>
            </Select>

            <Label v-if="toKeyShow.sn"  for="ToMethod">{{'ToMethod' | i18n}}</Label>
            <!-- <Input v-if="toKeyShow.sn"  id="ToMethod" v-model="smToKey.sm"/>-->
            <Select :disabled="readonly" v-if="toKeyShow.sn"   id="ToMethod" :filterable="true"
                    ref="ToMethod" :label-in-value="true" v-model="smToKey.sm">
                <Option value="*" >none</Option>
                <Option v-for="(v) in toCurMethods"  :value="v"  v-bind:key="v">{{v}}</Option>
            </Select>

            <Label v-if="cfg.toType == 1 "  for="db">{{'Table' | i18n}}</Label>
            <Input :disabled="readonly" v-if="cfg.toType ==  1 "  id="db" v-model="cfg.toParams"/>

            <Label v-if="cfg.toType == 4 "  for="fileName">{{'File' | i18n}}</Label>
            <Input :disabled="readonly" v-if="cfg.toType == 4 "  id="fileName" v-model="cfg.toParams"/>

            <Label v-if="cfg.toType == 5 "  for="tag">{{'Tag' | i18n}}</Label>
            <Input :disabled="readonly" v-if="cfg.toType == 5 "  id="tag" v-model="cfg.toParams"/>

            <Label v-if="cfg.toType == 6 "  for="topic">{{'Topic' | i18n}}</Label>
            <Input :disabled="readonly" v-if="cfg.toType == 6 "  id="topic" v-model="cfg.toParams"/>

            <Label v-if="cfg.toType == 7 "  for="email">{{'Email' | i18n}}</Label>
            <Input :disabled="readonly" v-if="cfg.toType == 7 "  id="email" v-model="cfg.toParams"/>

            <Label for="extParams">{{'extParams' | i18n}}</Label>
            <Input id="extParams" v-model="cfg.extParams"/>

            <Label  for="exp0">{{'Exp' | i18n}}</Label>
            <Input :disabled="readonly" id="exp0" v-model="cfg.expStr"/>

            <Label for="metadataName" style="font-weight: bold">{{cfg.resName}}<a href="javascript:void(0)"
            @click="reloadResourceMetadata()">
                {{'Refresh'|i18n}}</a></Label>
            <table id="metadataName" width="99%">
                <thead>
                    <tr style="width:30px">
                        <!--<td>{{'ResName' | i18n }}</td>-->
                        <td>{{'Name' | i18n }}</td>
                        <td>{{'Type' | i18n }}</td>
                        <td>{{'Desc' | i18n }}</td>
                    </tr>
                </thead>
                <tbody>
                    <tr  v-for="si in curResourceMetadatas" :key="si.name">
                       <!-- <td>{{si.resName}}</td>-->
                        <td>{{si.name}}</td>
                        <td>{{dataTypes[si.dataType]}}</td>
                        <td>{{si.desc}}</td>
                    </tr>
                </tbody>
            </table>

        </Drawer>

    </div>
</template>

<script>

    //import JStatisIndex from './JStatisIndex.vue'
    const UNIT_SE = "S";
    const UNIT_MU = "M";
    const UNIT_HO = "H";
    const UNIT_DA = "D";
    //const UNIT_MO = "Month";

   /* const BY_TYPE_SERVICE = "Service";
    const BY_TYPE_SERVICE_ACCOUNT='ServiceAccount';
    const BY_TYPE_SERVICE_INSTANCE = "ServiceInstance";*/
    //const BY_TYPE_CLIENT_INSTANCE = "ClientInstance";

    /*const BY_TYPE_SERVICE_INSTANCE_METHOD = "ServiceInstanceMethod";
    const BY_TYPE_SERVICE_ACCOUNT_METHOD = "ServiceAccountMethod";
    const BY_TYPE_SERVICE_METHOD = "ServiceMethod";
    const BY_TYPE_INSTANCE = "Instance";
    const BY_TYPE_ACCOUNT = "Account";*/

    //const BY_TYPE_EXP  = "Expression";

    /*
    const TO_TYPE_DB = "DB";
    const TO_TYPE_SERVICE_METHOD = "ServiceMethod";
    const TO_TYPE_CONSOLE = "Console";
    const TO_TYPE_FILE = "File";
    */
    //const TO_TYPE_DB = 1;
    const TO_TYPE_SERVICE_METHOD = 2;
    //const TO_TYPE_CONSOLE = 3;
    const TO_TYPE_FILE = 4;
    const TO_TYPE_MONITOR_LOG = 5;
    const TO_TYPE_MESSAGE = 6;
    const TO_TYPE_EMAIL = 7;

    //const PREFIX_TOTAL =1; // "total";
    //const PREFIX_TOTAL_PERCENT = 2; //"totalPercent";
    //const PREFIX_QPS = 3; //"qps";
    //const PREFIX_CUR = 4; //"cur";
    //const PREFIX_CUR_PERCENT = 5; //"curPercent";

    const REMOTE_KEYS = [window.jm.rpc.Constants.SERVICE_METHODS,
        window.jm.rpc.Constants.SERVICE_NAMESPACES,
        window.jm.rpc.Constants.SERVICE_VERSIONS,
        window.jm.rpc.Constants.INSTANCES];

    const DATA_TYPES = {
        4:"Integer", 3:"Float", 2:"Boolean", 1:"String"
    }

   /* const EXP_TYPE_SERVICE = 1;
    const EXP_TYPE_ACCOUNT = 2;
    const EXP_TYPE_INSTANCE = 3;*/

    const cid = 'resourceConfig';

    const sn = 'cn.jmicro.resource.IMngResourceService';
    const ns = "resourceMonitorServer";
    const v = '0.0.1';

    //const LOGS = ['No','Trance','Debug','Info','Warn','Error','Final'];

    export default {
        name: cid,
        components: {
            //JStatisIndex,
        },
        watch:{

            "cfg.resName":function(resName) {
                if(resName && resName.length > 0) {
                    this.resourceNameChange(resName,false);
                }
            },

            'cfg.toType':function(val){
                this.toTypeChange(val);
            },

            'smToKey.sn':function(val){
                this.toServiceTypeChange(val);
            },
        },

        data() {
            return {
                msg:'',
                timeUnits:[ UNIT_SE,UNIT_MU,UNIT_HO,UNIT_DA ],
                toTypes:{ 1:'Db', 2:"ServiceMethod", 3:'Console', 4:'File',5:'Log',6:'Message',7:'Email'  },
                dataTypes:DATA_TYPES,

                services:[],
                namespaces:{},
                versions:{},
                methods:{},
                instances:{},
                resourceMetadatas:{},

                resourceNames:[],
                namedTypeNames:[],
                allInstances:[],

                isLogin:false,
                logList: [],
                errMsg:'',

                smToKey:{sn:'',ns:'',ver:'',sm:''},
                toKeyShow:{sn:false,ns:false,ver:false,sm:false},
                //logLevel2Label:LOGS,

                toCurNamespaces:[],
                toCurVersions:[],
                toCurMethods:[],
                toCurInstances:[],

                curLogId:-1,
                cfg:{},
                updateMode: false,
                addConfigDialog:false,

                readonly : false,
                curResourceMetadatas:[],
            }
        },

        methods: {

            reloadResourceMetadata() {
                if(this.cfg.resName && this.cfg.resName.length > 0) {
                    this.resourceNameChange(this.cfg.resName,true);
                }
            },

            resourceNameChange(resName,refresh){
                if(!this.cfg.resName || this.cfg.resName.length == 0) {
                    return;
                }
                if(!refresh && this.resourceMetadatas[resName]) {
                    this.curResourceMetadatas = this.resourceMetadatas[resName];
                } else {
                    let self = this;
                    window.jm.rpc.callRpcWithParams(sn,ns,v, 'getResourceMetadata', [resName,refresh])
                        .then((resp)=>{
                            if(resp.code != 0) {
                                self.$Message.success(resp.msg);
                                return;
                            }
                            self.curResourceMetadatas = self.resourceMetadatas[resName] = resp.data;
                        }).catch((err)=>{
                        window.console.log(err);
                    });
                }
            },

            toServiceTypeChange(curToType) {
                let self = this;
                let fun = ()=>{
                    self.toCurNamespaces = self.namespaces[curToType];
                    self.toCurVersions = self.versions[curToType];
                    self.toCurMethods = self.methods[curToType];
                };

                if(!this.namespaces[curToType]) {
                    this.getByServiceName(REMOTE_KEYS, curToType, fun);
                } else {
                    fun();
                }
            },

            toTypeChange(curToType) {
                this.toKeyShow.sn = curToType == TO_TYPE_SERVICE_METHOD
            },

            add() {
                this.cfg = {};
                this.updateMode=false;
                this.readonly = false;
                this.addConfigDialog = true;
            },

            remove(id) {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn,ns,v, 'delete', [id])
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

            enable(id) {
                let self = this;
                window.jm.rpc.callRpcWithParams(sn,ns,v, 'enable', [id])
                    .then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        for(let i = 0; i < self.logList.length; i++) {
                            if(self.logList[i].id == id) {
                                self.logList[i].enable = !self.logList[i].enable;
                                break;
                            }
                        }
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            update(cfg,fromView) {

                let self = this;
                this.updateMode = true;
                this.cfg = cfg;

                self.resourceNameChange(cfg.resName,false);

                if(self.cfg.toType == TO_TYPE_SERVICE_METHOD) {
                    if(self.cfg.toParams && self.cfg.toParams.length > 0) {
                        let arr = self.cfg.toParams.split("##");
                        if(arr.length > 6) {
                            self.smToKey.sn = arr[0];
                            self.smToKey.ns = arr[1];
                            self.smToKey.ver = arr[2];
                            self.smToKey.ins = arr[3];
                            self.smToKey.sm = arr[6];
                        }
                    }
                }

                self.cfg.toType += '';
                if(!fromView) {
                    this.readonly = false;
                }
                this.addConfigDialog = true;
            },

            view(cfg) {
                this.readonly = true;
                this.update(cfg,true);
            },

            doSave() {
                let self = this;

                if(!this.cfg.monitorInsName) {
                    this.monitorInsName = '*';
                }

                if(!this.cfg.resName ||this.cfg.resName.length == 0 ) {
                    this.errMsg = '监听资源不能为空';
                    return;
                }

                if(self.cfg.toType == TO_TYPE_SERVICE_METHOD) {
                    if(!self.smToKey.sn || self.smToKey.sn.length == 0) {
                        self.errMsg = '接收数据目标服务不能为空';
                        return;
                    }

                    if(!self.smToKey.ns || self.smToKey.ns.length == 0) {
                        self.errMsg = '接收数据目标名称空间不能为空';
                        return;
                    }

                    if(!self.smToKey.ver || self.smToKey.ver.length == 0) {
                        self.errMsg = '接收数据目标版本不能为空';
                        return;
                    }

                    if(!self.smToKey.sm || self.smToKey.sm.length == 0) {
                        self.errMsg = '接收数据目标方法不能为空';
                        return;
                    }
                    this.cfg.toParams = this.smToKey.sn+'##'+this.smToKey.ns+'##'+this.smToKey.ver+'########'+this.smToKey.sm;
                }else if(self.cfg.toType == TO_TYPE_FILE ) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  self.cfg.type == 2 ? '数据库表名不能为空':'文件名不能为空';
                        return;
                    }
                }/*else if(self.cfg.toType == TO_TYPE_DB) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.cfg.toParams = 't_statis_data';
                    }
                }*/else if(self.cfg.toType == TO_TYPE_MONITOR_LOG) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  '日志标签不能为空';
                        return;
                    }
                }else if(self.cfg.toType == TO_TYPE_MESSAGE) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  '消息主题不能为空';
                        return;
                    }
                }else if(self.cfg.toType == TO_TYPE_EMAIL) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  'Emai地圵不能为空';
                        return;
                    }
                    if(!window.jm.utils.checkEmail(self.cfg.toParams)) {
                        self.errMsg =  'Emai地圵不合法';
                        return;
                    }
                }

                if(!this.updateMode) {
                    window.jm.rpc.callRpcWithParams(sn,ns,v, 'add', [self.cfg])
                        .then((resp)=>{
                            if(resp.code != 0) {
                                self.$Message.success(resp.msg);
                                return;
                            }
                            this.addConfigDialog = false;
                            self.logList.push(resp.data);

                        }).catch((err)=>{
                            window.console.log(err);
                    });
                } else {
                    window.jm.rpc.callRpcWithParams(sn,ns,v, 'update', [self.cfg])
                        .then((resp)=>{
                            if(resp.code != 0) {
                                self.$Message.success(resp.msg);
                                return;
                            }
                            this.addConfigDialog = false;
                        }).catch((err)=>{
                            window.console.log(err);
                    });
                }
            },

            refresh() {
                let self = this;
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }

                window.jm.rpc.callRpcWithParams(sn,ns,v, 'query', [])
                .then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }

                    let ll = resp.data;
                    self.logList = ll;

                }).catch((err)=>{
                    if(err && !!err.errorCode) {
                        self.msg = err.msg;
                    }else {
                        self.msg = err;
                    }
                });
            },

            getServiceNames() {
                let self = this;
                window.jm.mng.comm.getDicts([window.jm.rpc.Constants.SERVICE_NAMES,
                    window.jm.rpc.Constants.SERVICE_VERSIONS,
                    window.jm.rpc.Constants.SERVICE_NAMESPACES,
                    window.jm.rpc.Constants.MONITOR_RESOURCE_NAMES,
                    window.jm.rpc.Constants.ALL_INSTANCES,],'')
                    .then((opts)=>{
                        if(opts) {
                            self.serviceNames = opts[window.jm.rpc.Constants.SERVICE_NAMES];
                            self.serviceVersions = opts[window.jm.rpc.Constants.SERVICE_VERSIONS];
                            self.serviceNamespaces = opts[window.jm.rpc.Constants.SERVICE_NAMESPACES];
                            self.allInstances = opts[window.jm.rpc.Constants.ALL_INSTANCES];
                            self.serviceMethods = opts[window.jm.rpc.Constants.SERVICE_METHODS];
                            self.resourceNames = opts[window.jm.rpc.Constants.MONITOR_RESOURCE_NAMES];
                        }
                }).catch((err)=>{
                    throw err;
                });
            },

            getByServiceName(keys,sn,cb) {
                let self = this;
                if(!sn || sn.length == 0) {
                    return;
                }
                window.jm.mng.comm.getDicts(keys,sn)
                    .then((opts)=>{
                        if(opts) {
                            for(let k in opts) {
                                if(k == window.jm.rpc.Constants.SERVICE_VERSIONS) {
                                    self.versions[sn] = opts[k];
                                }else  if(k == window.jm.rpc.Constants.SERVICE_NAMESPACES) {
                                    self.namespaces[sn] = opts[k];
                                }else  if(k == window.jm.rpc.Constants.SERVICE_METHODS) {
                                    self.methods[sn] = opts[k];
                                }else  if(k == window.jm.rpc.Constants.INSTANCES) {
                                    self.instances[sn] = opts[k];
                                }
                            }
                            if(cb) {
                                cb();
                            }
                        }

                    }).catch((err)=>{
                        throw err;
                });
            },

        },

        mounted () {

            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            window.jm.rpc.addActListener(cid,this.refresh);
            let self = this;
            this.getServiceNames();

            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"Add",label:"Add",icon:"ios-cog",call:self.add},
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}
                        ]
                });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            this.refresh();

            window.jm.vue.$on('editorClosed',ec);
        },

        beforeDestroy() {
            window.jm.rpc.removeActListener(cid);
        },

    }
</script>

<style>
    .JResourceConfigView{
    }
</style>