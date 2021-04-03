<template>
    <div class="JStatisConfigView" ref="name">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr style="width:30px">
                    <td>{{'ID' | i18n }}</td>
                    <td>{{'clientId' | i18n }}</td>
                    <td>{{'StatisType' | i18n }}</td>
                    <td>{{'StatisKey' | i18n }}</td>
                    <td>{{'CTimeout(S)' | i18n }}</td>
                    <td >{{'StatisIndex' | i18n }}</td>
                   <!-- <td>{{'TimeUnit' | i18n }}</td>
                    <td >{{'TimeCnt' | i18n }}</td>-->
                    <td>{{'ToType' | i18n }}</td>
                    <td>{{'ToParams' | i18n }}</td>
                    <td>{{'Exp0' | i18n }}</td>
                    <td>{{'Exp1' | i18n }}</td>

                    <td>{{'Enable' | i18n }}</td>
                    <!--<td>{{'Tag' | i18n }}</td>-->
                    <!--<td>{{'CreatedBy' | i18n }}</td>-->
                    <td>{{'Operation' | i18n }}</td>
                </tr></thead>
                <tr v-for="c in logList" :key="c.id">
                    <td>{{c.id}}</td>
                    <td>{{c.clientId}}</td>
                    <td>{{byTypes[c.byType]}}</td>
                    <td>{{c.byKey}}</td>
                    <td>{{c.counterTimeout}}</td>
                    <td><span v-for="va in c.statisIndexs" :key="va.vk">{{ va.vk }},</span></td>
                   <!-- <td>{{c.timeUnit}}</td>
                    <td>{{c.timeCnt}}</td>-->
                    <td>{{toTypes[c.toType]}}</td>
                    <td>{{c.toParams}}</td>
                    <td>{{c.expStr}}</td>
                    <td>{{c.expStr1}}</td>
                    <td>{{c.enable}}</td>
                   <!-- <td>{{c.tag}}</td>-->
                   <!-- <td>{{c.createdBy}}</td>-->
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

        <div v-if="!isLogin" >Not login</div>

        <div v-if="isLogin  && (!logList || logList.length == 0)" >No data</div>

        <Drawer v-model="addStatisConfigDialog" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="80">

              <!--  <Checkbox :disabled="true" v-model="cfg.enable">{{ 'Enable' | i18n}}</Checkbox>&nbsp;&nbsp;&nbsp;&nbsp;-->
            <Button v-if="!readonly" @click="doSave()">{{'Confirm'|i18n}}</Button>
            <Button v-if="!readonly" @click="doRefreshDict()">{{'RefreshDict'|i18n}}</Button><br/>
            <Label v-if="errMsg"  style="color:red">{{errMsg}}</Label><br/>

            <a v-if="!readonly" href="javascript:void(0)" @click="addStatisIndex()" style="text-align: left">{{'Add' | i18n }}</a>
            <table id="statisIndex" width="99%">
                <thead>
                <tr style="width:30px">
                    <td>{{'IndexType' | i18n }}</td>
                    <td>{{'Desc' | i18n }}</td>
                    <td>{{'Key' | i18n }}</td>
                    <td>{{'Numerator' | i18n }}</td>
                    <td>{{'Denominator' | i18n }}</td>
                    <td>{{'Operation' | i18n }}</td>
                </tr>
                </thead>
                <tbody>
                <JStatisIndex v-for="(si,idx) in cfg.statisIndexs" :key="si.keyName" :si="si"
                              @delete="delStatisIndex(idx)" :readonly="readonly"/>
                </tbody>
            </table>

                <Label for="byType">{{'byType' | i18n}}</Label>
                <Select :disabled="readonly" id="byType" v-model="cfg.byType" >
                    <!--<Option value="*" >none</Option>-->
                    <Option v-for="(key,val) in byTypes" :value="val" :key="key">{{key | i18n}}</Option>
                </Select>

                <Label v-if="byKeyShow.sn"  for="ByService">{{'ByService' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.sn"  id="ByService" v-model="byKey.sn"/>-->
                <Select :disabled="readonly" v-if="byKeyShow.sn"   id="ByService" :filterable="true"
                         ref="ByService" :label-in-value="true" v-model="byKey.sn">
                  <!--  <Option value="*" >none</Option>-->
                    <Option v-for="(v) in serviceNames"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.ns"  for="ByNamespace">{{'ByNamespace' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.ns"  id="ByNamespace" v-model="byKey.ns"/>-->
                <Select :disabled="readonly" v-if="byKeyShow.sn"   id="ByNamespace" :filterable="true"
                        ref="ByService" :label-in-value="true" v-model="byKey.ns">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in byCurNamespaces"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.ver"  for="ByVersion">{{'ByVersion' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.ver"  id="ByVersion" v-model="byKey.ver"/>-->
                <Select :disabled="readonly" v-if="byKeyShow.ver"   id="ByVersion" :filterable="true"
                        ref="ByService" :label-in-value="true" v-model="byKey.ver">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in byCurVersions"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.sm"  for="ByMethod">{{'ByMethod' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.sm"  id="ByMethod" v-model="byKey.sm"/>-->
                <Select :disabled="readonly" v-if="byKeyShow.sm"   id="ByMethod" :filterable="true"
                        ref="ByService" :label-in-value="true" v-model="byKey.sm">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in byCurMethods"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.ins"  for="ByIns">{{'ByIns' | i18n}}</Label>
                <Input :disabled="readonly" v-if="byKeyShow.ins"  id="ByIns" v-model="byKey.ins"/>
                <!--<Select :disabled="readonly" v-if="byKeyShow.ins"   id="ByIns" :filterable="true"
                        ref="ByIns" :label-in-value="true" v-model="byKey.ins">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in byCurInstances"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>-->

                <Label v-if="byKeyShow.allIns"  for="ByAllIns">{{'ByIns' | i18n}}</Label>
                <Input :disabled="readonly" v-if="byKeyShow.allIns"  id="ByAllIns" v-model="byKey.ins"/>
                <!--<Select :disabled="readonly" v-if="byKeyShow.allIns"   id="ByAllIns" :filterable="true"
                        ref="ByIns" :label-in-value="true" v-model="byKey.ins">
                   <Option value="*" >none</Option>
                    <Option v-for="(v) in allInstances"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>-->

                <Label v-if="cfg.byType==5 || cfg.byType==3" for="byAccount">{{'Account' | i18n}}</Label>
                <Input :disabled="readonly" v-if="cfg.byType==5 || cfg.byType==3"  id="byAccount" v-model="cfg.byKey"/>

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

                <Label for="counterTimeout">{{'CounterTimeout' | i18n}}(S)</Label>
                <Input :disabled="readonly" id="counterTimeout" v-model="cfg.counterTimeout"/>

                <Label for="actName">{{'ActName' | i18n}}</Label>
                <Input :disabled="readonly" id="actName" v-model="cfg.actName"/>

                <!--<Label for="timeUnit">{{'timeUnit' | i18n}}</Label>
                <Select id="timeUnit" v-model="cfg.timeUnit">
                    <Option v-for="k in timeUnits" :value="k" :key="k">{{k | i18n}}</Option>
                </Select>
                -->

              <!--  <Label for="statisIndex">{{'statisIndex' | i18n}}</Label>-->
                <!--<CheckboxGroup id="statisIndex" v-model="cfg.statisIndexs">
                    <Checkbox v-for="(k,v) in statisIndex" :key="k" :label="v" :value="k">
                        <span>{{ k | i18n }}</span>
                    </Checkbox>
                </CheckboxGroup>-->

            <Label for="minNotifyTime">{{'minNotifyTime' | i18n}}(MS)</Label>
            <Input :disabled="readonly" id="minNotifyTime" v-model="cfg.minNotifyTime"/>

            <Label for="namedType">{{'NamedType' | i18n}}</Label>&nbsp;&nbsp;&nbsp;<a href="javascript:void(0);" @click="namedTypeDetail()">{{'Detail'|i18n}}</a>
            <Select :disabled="readonly" id="namedType" v-model="cfg.namedType">
                <Option value="" >none</Option>
                <Option v-for="k in namedTypeNames" :value="k" :key="k">{{k | i18n}}</Option>
            </Select>

            <Label  for="exp0">{{'Exp0' | i18n}}</Label>
            <Input :disabled="readonly" id="exp0" v-model="cfg.expStr"/>
          <!--
          <Input :disabled="readonly"  id="byExpression"  class='textarea'
                    :autosize="{maxRows:5,minRows: 2}" type="textarea" v-model="cfg.expStr"/>
            -->
            <Label for="exp1">{{'Exp1' | i18n}}</Label>
            <Input id="exp1" v-model="cfg.expStr1"/>

        </Drawer>

    </div>
</template>

<script>

    import JStatisIndex from './JStatisIndex.vue'

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

    const BY_TYPE_SERVICE_METHOD = 1;
    const BY_TYPE_SERVICE_INSTANCE_METHOD = 2;
    const BY_TYPE_SERVICE_ACCOUNT_METHOD = 3;
    const BY_TYPE_INSTANCE = 4;
    const BY_TYPE_ACCOUNT = 5;

    //const BY_TYPE_EXP  = "Expression";

    /*
    const TO_TYPE_DB = "DB";
    const TO_TYPE_SERVICE_METHOD = "ServiceMethod";
    const TO_TYPE_CONSOLE = "Console";
    const TO_TYPE_FILE = "File";
    */
    const TO_TYPE_DB = 1;
    const TO_TYPE_SERVICE_METHOD = 2;
    //const TO_TYPE_CONSOLE = 3;
    const TO_TYPE_FILE = 4;
    const TO_TYPE_MONITOR_LOG = 5;
    const TO_TYPE_MESSAGE = 6;

    //const PREFIX_TOTAL =1; // "total";
    //const PREFIX_TOTAL_PERCENT = 2; //"totalPercent";
    //const PREFIX_QPS = 3; //"qps";
    //const PREFIX_CUR = 4; //"cur";
    //const PREFIX_CUR_PERCENT = 5; //"curPercent";

    const REMOTE_KEYS = [window.jm.rpc.Constants.SERVICE_METHODS, window.jm.rpc.Constants.SERVICE_NAMESPACES,
        window.jm.rpc.Constants.SERVICE_VERSIONS,window.jm.rpc.Constants.INSTANCES];

   /* const EXP_TYPE_SERVICE = 1;
    const EXP_TYPE_ACCOUNT = 2;
    const EXP_TYPE_INSTANCE = 3;*/

    const cid = 'statisConfig';

    const sn = 'cn.jmicro.monitor.statis.api.IStatisConfigService';
    const ns = 'StatisMonitor';
    const v = '0.0.1';

    //const LOGS = ['No','Trance','Debug','Info','Warn','Error','Final'];

    export default {
        name: cid,
        components: {
            JStatisIndex,
        },
        watch:{
            'cfg.byType':function(val){
                this.byTypeChange(val);
            },

            'cfg.toType':function(val){
                this.toTypeChange(val);
            },

            'byKey.sn':function(val){
                this.byServiceTypeChange(val);
            },

            'smToKey.sn':function(val){
                this.toServiceTypeChange(val);
            },

        },

        data() {
            return {
                timeUnits:[ UNIT_SE,UNIT_MU,UNIT_HO,UNIT_DA ],
                toTypes:{ 1:'Db', 2:"ServiceMethod", 3:'Console', 4:'File',5:'Log',6:'Message'  },
                byTypes:{ 1:'Method', 2: 'InstanceMethod', 3:'AccountMethod', 4:'Instance'/*,5: 'Account'*/},
                statisIndex:{1:'Total',2:'TotalPercent',3:'Qps',4:'Cur',5:'CurPercent'},
                //expTypes:{'Service':EXP_TYPE_SERVICE,'Account':EXP_TYPE_ACCOUNT,'Instance':EXP_TYPE_INSTANCE},

                services:[],
                namespaces:{},
                versions:{},
                methods:{},
                instances:{},

                byCurNamespaces:[],
                byCurVersions:[],
                byCurMethods:[],
                byCurInstances:[],

                namedTypeNames:[],
                allInstances:[],

                isLogin:false,
                logList: [],
                errMsg:'',
                byKey:{sn:'',ns:'',ver:'',sm:'',ins:''},
                byKeyShow:{sn:false,ns:false,ver:false,sm:false,ins:false,allIns:false,exp:false},

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
                addStatisConfigDialog:false,

                readonly : false,
            }
        },

        methods: {

            addStatisIndex() {
                this.cfg.statisIndexs.push({ nums:[],dens:[] })
            },

            delStatisIndex(idx) {
                if(this.cfg.statisIndexs && this.cfg.statisIndexs.length > 0
                    && this.cfg.statisIndexs.length > idx) {
                    this.cfg.statisIndexs.splice(idx,1);
                }
            },

            byTypeChange(curByType) {
                this.byKeyShow.sn = this.byKeyShow.ns = this.byKeyShow.ver = this.byKeyShow.sm =
                    curByType == BY_TYPE_SERVICE_METHOD ||
                    curByType == BY_TYPE_SERVICE_INSTANCE_METHOD ||
                    curByType == BY_TYPE_SERVICE_ACCOUNT_METHOD;

                //this.byKeyShow.sm = curByType == BY_TYPE_SERVICE_METHOD || curByType == BY_TYPE_SERVICE_INSTANCE_METHOD;

                this.byKeyShow.ins =  curByType == BY_TYPE_SERVICE_INSTANCE_METHOD ;

                this.byKeyShow.allIns = curByType == BY_TYPE_INSTANCE;

                //this.byKeyShow.exp = curByType == BY_TYPE_EXP;
            },

            byServiceTypeChange(curByType) {
                if(curByType == '*') {
                    return;
                }

                if(!this.byKeyShow.sn) {
                    return;
                }

                let self = this;
                let fun = () => {
                    self.byCurNamespaces = self.namespaces[curByType];
                    self.byCurVersions = self.versions[curByType];
                    self.byCurMethods = self.methods[curByType];
                    self.byCurInstances = self.instances[curByType];
                };
                if(!this.namespaces[curByType]) {
                    this.getByServiceName(REMOTE_KEYS, curByType, fun);
                } else {
                    fun();
                }
            },

            doRefreshDict(){
                this.getServiceNames(true);
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
                this.toKeyShow.sn = curToType == TO_TYPE_SERVICE_METHOD;
                if(curToType == TO_TYPE_DB) {
                    this.cfg.toParams = 't_statis_data';
                }else if(curToType == TO_TYPE_MONITOR_LOG) {
                    this.cfg.toParams = 'rpc_log';
                }
            },

            add() {
                this.cfg = {statisIndexs:[]};
                this.errMsg = '';

                this.updateMode=false;
                if(!this.cfg.timeUnit || this.cfg.timeUnit.length == 0) {
                    this.cfg.timeUnit = UNIT_MU;
                }

                if(!this.cfg.timeCnt <= 0) {
                    this.cfg.timeCnt = 1;
                }

                //this.cfg = {};
                this.readonly = false;
                this.addStatisConfigDialog = true;
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
                this.errMsg = '';

                if(cfg.byType == BY_TYPE_INSTANCE ) {
                   this.byKey.ins = this.cfg.byKey;
                }/*else if(cfg.byType == BY_TYPE_ACCOUNT) {

                }*/ else if(cfg.byType != BY_TYPE_ACCOUNT) {
                    //cn.jmicro.api.security.ISecretService##sec##0.0.1##security0##192.168.56.1##51535##publicKeysList##
                    let arr = cfg.byKey.split("##");
                    this.byKey.sn = arr[0];
                    this.byKey.ns = arr[1];
                    this.byKey.ver = arr[2];
                    this.byKey.ins = arr[3];
                    this.byKey.sm = arr[6];
                }

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

                if(self.cfg.statisIndexs && self.cfg.statisIndexs.length > 0) {
                    for(let i = 0; i < self.cfg.statisIndexs.length; i++) {
                        self.cfg.statisIndexs[i].type += ''
                    }
                }

                self.cfg.byType += '';
                self.cfg.toType += '';
                if(!fromView) {
                    this.readonly = false;
                }
                this.addStatisConfigDialog = true;
            },

            view(cfg) {
                this.readonly = true;
                this.update(cfg,true);
            },

            doSave() {
                let self = this;

                //this.$refs.addNodeDialog.buttonLoading = false;

                if(!this.cfg.statisIndexs || this.cfg.statisIndexs.length == 0) {
                    this.errMsg = '统计指标不能为空';
                    return;
                }

                for(let i = 0; i < this.cfg.statisIndexs.length; i++ ) {
                    let si = this.cfg.statisIndexs[i];
                    if(si.type <= 0 || si.type > 5) {
                        this.errMsg = '统计指标类型不合法'+  si.type;
                        return;
                    }

                    if(!si.vk || si.vk.length == 0) {
                        this.errMsg = '统计指标名称不能为空';
                        return;
                    }

                    if(!si.nums || si.nums.length == 0) {
                        this.errMsg = '统计指标分子值不能为空';
                        return;
                    }

                    if((si.type== 2 || si.type == 5) && (!si.dens || si.dens.length == 0)) {
                        this.errMsg = '统计指标分母值不能为空';
                        return;
                    }

                }

                if(!this.cfg.byType) {
                    this.errMsg = '统计源不能为空';
                    return;
                }

                if(!this.cfg.toType) {
                    this.errMsg = '统计结果目标类型不能为空';
                    return;
                }

                if(this.cfg.byType == BY_TYPE_INSTANCE  ) {
                    if(!this.byKey.ins || this.byKey.ins.length == 0) {
                        this.errMsg = '实例名称不能为空';
                        return;
                    }
                    this.cfg.byKey = this.byKey.ins;
                }else if(this.cfg.byType == BY_TYPE_ACCOUNT) {
                    if(!this.cfg.byKey || this.cfg.byKey.length == 0) {
                        this.errMsg = '统计账号不能为空';
                        return;
                    }
                } else {
                    //cn.jmicro.api.security.ISecretService##sec##0.0.1##security0##192.168.56.1##51535##publicKeysList##
                    if(!this.byKey.sn || this.byKey.sn.length == 0) {
                        this.errMsg = '服务名称不不能为空';
                        return;
                    }
                    if(!this.byKey.ns || this.byKey.ns.length == 0) {
                        this.byKey.ns='*';
                    }
                    if(!this.byKey.ver || this.byKey.ver.length == 0) {
                        this.byKey.ver='*';
                    }

                    if(!this.byKey.sm || this.byKey.sm.length == 0) {
                        this.byKey.sm='*';
                    }

                    if(!this.byKey.ins || this.byKey.ins.length == 0) {
                        this.byKey.ins='*';
                    }

                    this.cfg.byKey = this.byKey.sn+'##'+this.byKey.ns+'##'+this.byKey.ver+'##'+this.byKey.ins+'######'+this.byKey.sm;
                }

                if(!this.cfg.timeUnit || this.cfg.timeUnit.length == 0) {
                    this.cfg.timeUnit = UNIT_MU;
                }

                if(!this.cfg.timeCnt <= 0) {
                    this.cfg.timeCnt = 1;
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
                }else if(self.cfg.toType == TO_TYPE_DB) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.cfg.toParams = 't_statis_data';
                    }
                }else if(self.cfg.toType == TO_TYPE_MONITOR_LOG) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  '日志标签不能为空';
                        return;
                    }
                }else if(self.cfg.toType == TO_TYPE_MESSAGE) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  '消息主题不能为空';
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
                            this.addStatisConfigDialog = false;
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
                            this.addStatisConfigDialog = false;
                        }).catch((err)=>{
                            window.console.log(err);
                    });
                }
            },

            refresh() {
                this.errMsg = '';
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
                    window.console.log(err);
                });
            },

            getServiceNames(forceRefresh) {
                let self = this;
                window.jm.mng.comm.getDicts([window.jm.rpc.Constants.SERVICE_NAMES,
                    window.jm.rpc.Constants.NAMED_TYPES,window.jm.rpc.Constants.ALL_INSTANCES,],'',forceRefresh)
                    .then((opts)=>{
                        if(opts) {

                            self.namespaces = {};
                            self.versions = {};
                            self.methods = {};
                            self.instances = {};

                            self.serviceNames = opts[window.jm.rpc.Constants.SERVICE_NAMES];
                            self.namedTypeNames = opts[window.jm.rpc.Constants.NAMED_TYPES];
                            self.allInstances = opts[window.jm.rpc.Constants.ALL_INSTANCES];

                            if(self.byKeyShow.sn && self.byType) {
                                self.byServiceTypeChange(self.byType);
                            }
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

            namedTypeDetail() {

            }

        },

        mounted () {
            this.errMsg = '';
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
    .JStatisConfigView{
    }


</style>