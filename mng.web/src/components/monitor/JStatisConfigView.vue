<template>
    <div class="JStatisConfigView" style="position:relative;height:auto">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr style="width:30px">
                    <td>{{'ID' | i18n }}</td>
                    <td>{{'StatisType' | i18n }}</td>
                    <td>{{'StatisKey' | i18n }}</td>
                    <td>{{'StatisClients' | i18n }}</td>
                    <td>{{'NamedType' | i18n }}</td>
                    <td >{{'StatisIndex' | i18n }}</td>
                   <!-- <td>{{'TimeUnit' | i18n }}</td>
                    <td >{{'TimeCnt' | i18n }}</td>-->
                    <td>{{'DataTarget' | i18n }}</td>
                    <td>{{'Params' | i18n }}</td>
                    <td>{{'Expression' | i18n }}</td>

                    <td>{{'Enable' | i18n }}</td>
                    <!--<td>{{'Tag' | i18n }}</td>-->
                    <!--<td>{{'CreatedBy' | i18n }}</td>-->
                    <td>{{'Operation' | i18n }}</td>
                </tr></thead>
                <tr v-for="c in logList" :key="c.id">
                    <td>{{c.id}}</td>
                    <td>{{c.byType}}</td>
                    <td>{{c.byKey}}</td>
                    <td>{{c.actName}}</td>
                    <td>{{c.namedType}}</td>
                    <td>{{c.statisIndexs.join(',')}}</td>
                   <!-- <td>{{c.timeUnit}}</td>
                    <td>{{c.timeCnt}}</td>-->
                    <td>{{c.toType}}</td>
                    <td>{{c.toParams}}</td>
                    <td>{{c.expStr}}</td>
                    <td>{{c.enable}}</td>
                   <!-- <td>{{c.tag}}</td>-->
                   <!-- <td>{{c.createdBy}}</td>-->
                    <td>
                        <a v-if="isLogin && !c.enable" @click="update(c)">{{'Update' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin && !c.enable" @click="remove(c.id)">{{'Delete' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && c.enable" @click="enable(c.id)">{{'Disalbe' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin  && !c.enable" @click="enable(c.id)">{{'Enable' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
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

        <Modal v-model="addStatisConfigDialog" :loading="true" ref="addNodeDialog" width="500" @on-ok="doSave()">
            <div>
                <Label v-if="errMsg"  style="color:red">{{errMsg}}</Label><br/>

                <Label for="byType">{{'byType' | i18n}}</Label>
                <Select id="byType" v-model="cfg.byType" >
                    <!--<Option value="*" >none</Option>-->
                    <Option v-for="k in byTypes" :value="k" :key="k">{{k | i18n}}</Option>
                </Select>

                <Label v-if="byKeyShow.sn"  for="ByService">{{'ByService' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.sn"  id="ByService" v-model="byKey.sn"/>-->
                <Select v-if="byKeyShow.sn"   id="ByService" :filterable="true"
                         ref="ByService" :label-in-value="true" v-model="byKey.sn">
                  <!--  <Option value="*" >none</Option>-->
                    <Option v-for="(v) in serviceNames"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.ns"  for="ByNamespace">{{'ByNamespace' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.ns"  id="ByNamespace" v-model="byKey.ns"/>-->
                <Select v-if="byKeyShow.sn"   id="ByNamespace" :filterable="true"
                        ref="ByService" :label-in-value="true" v-model="byKey.ns">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in byCurNamespaces"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.ver"  for="ByVersion">{{'ByVersion' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.ver"  id="ByVersion" v-model="byKey.ver"/>-->
                <Select v-if="byKeyShow.ver"   id="ByVersion" :filterable="true"
                        ref="ByService" :label-in-value="true" v-model="byKey.ver">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in byCurVersions"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.sm"  for="ByMethod">{{'ByMethod' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.sm"  id="ByMethod" v-model="byKey.sm"/>-->
                <Select v-if="byKeyShow.sm"   id="ByMethod" :filterable="true"
                        ref="ByService" :label-in-value="true" v-model="byKey.sm">
                    <!--<Option value="*" >none</Option>-->
                    <Option v-for="(v) in byCurMethods"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.ins"  for="ByIns">{{'ByIns' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.ins"  id="ByIns" v-model="byKey.ins"/>-->
                <Select v-if="byKeyShow.ins"   id="ByIns" :filterable="true"
                        ref="ByIns" :label-in-value="true" v-model="byKey.ins">
                    <!-- <Option value="*" >none</Option>-->
                    <Option v-for="(v) in byCurInstances"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.allIns"  for="ByAllIns">{{'ByIns' | i18n}}</Label>
                <!--<Input v-if="byKeyShow.ins"  id="ByIns" v-model="byKey.ins"/>-->
                <Select v-if="byKeyShow.allIns"   id="ByAllIns" :filterable="true"
                        ref="ByIns" :label-in-value="true" v-model="byKey.ins">
                   <!-- <Option value="*" >none</Option>-->
                    <Option v-for="(v) in allInstances"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="cfg.byType=='Account' || cfg.byType=='ServiceMethodAccount'" for="byAccount">{{'Account' | i18n}}</Label>
                <Input v-if="cfg.byType=='Account' || cfg.byType=='ServiceMethodAccount'"  id="byAccount" v-model="cfg.byKey"/>

               <!-- <Label v-if="byKeyShow.exp"  for="expType">{{'ExpType' | i18n}}</Label>
                &lt;!&ndash;<Input v-if="byKeyShow.ins"  id="ByIns" v-model="byKey.ins"/>&ndash;&gt;
                <Select v-if="byKeyShow.exp"    id="expType" :filterable="true"
                        ref="ByIns" :label-in-value="true" v-model="cfg.expForType">
                    <Option v-for="(v,k) in expTypes"  :value="v"  v-bind:key="v">{{k}}</Option>
                </Select>

                <Label v-if="byKeyShow.exp && cfg.expForType==1"  for="ByExpService">{{'ByService' | i18n}}</Label>
                &lt;!&ndash;<Input v-if="byKeyShow.sn"  id="ByService" v-model="byKey.sn"/>&ndash;&gt;
                <Select v-if="byKeyShow.exp  && cfg.expForType==1"   id="ByExpService" :filterable="true"
                        ref="ByExpService" :label-in-value="true" v-model="byKey.byKey">
                    <Option v-for="(v) in serviceNames"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="byKeyShow.exp && cfg.expForType==2" for="byExpAccount">{{'Account' | i18n}}</Label>
                <Input v-if="byKeyShow.exp && cfg.expForType==2"  id="byExpAccount" v-model="cfg.byKey"/>

                <Label v-if="byKeyShow.exp && cfg.expForType==3"  for="ByExpAllIns">{{'ByIns' | i18n}}</Label>
                &lt;!&ndash;<Input v-if="byKeyShow.ins"  id="ByIns" v-model="byKey.ins"/>&ndash;&gt;
                <Select v-if="byKeyShow.exp && cfg.expForType==3"   id="ByExpAllIns" :filterable="true"
                        ref="ByExpAllIns" :label-in-value="true" v-model="byKey.byKey">
                    <Option v-for="(v) in allInstances"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>-->

                <Label  for="byExpression">{{'Expression' | i18n}}</Label>
                <Input  id="byExpression"  class='textarea'
                        :autosize="{maxRows:5,minRows: 2}" type="textarea" v-model="cfg.expStr"/>

                <Label for="namedType">{{'NamedType' | i18n}}</Label>&nbsp;&nbsp;&nbsp;<a href="javascript:void(0);" @click="namedTypeDetail()">{{'Detail'|i18n}}</a>
                <Select id="namedType" v-model="cfg.namedType">
                    <Option v-for="k in namedTypeNames" :value="k" :key="k">{{k | i18n}}</Option>
                </Select>

                <Label for="toType">{{'toType' | i18n}}</Label>
                <Select id="toType" v-model="cfg.toType">
                    <Option v-for="k in toTypes" :value="k" :key="k">{{k | i18n}}</Option>
                </Select>

                <Label v-if="toKeyShow.sn"  for="ToService">{{'ToService' | i18n}}</Label>
                <!--<Input v-if="toKeyShow.sn"  id="ToService" v-model="smToKey.sn"/>-->
                <Select v-if="toKeyShow.sn"   id="ToService" :filterable="true"
                                  ref="ToService" :label-in-value="true" v-model="smToKey.sn">
                    <Option value="*" >none</Option>
                    <Option v-for="v in serviceNames"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="toKeyShow.sn"  for="ToNamespace">{{'ToNamespace' | i18n}}</Label>
                <!--<Input v-if="toKeyShow.sn"  id="ToNamespace" v-model="smToKey.ns"/>-->
                <Select v-if="toKeyShow.sn"   id="ToNamespace" :filterable="true"
                        ref="ToNamespace" :label-in-value="true" v-model="smToKey.ns">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in toCurNamespaces"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="toKeyShow.sn"  for="ToVersion">{{'ToVersion' | i18n}}</Label>
               <!-- <Input v-if="toKeyShow.sn"  id="ToVersion" v-model="smToKey.ver"/>-->
                <Select v-if="toKeyShow.sn"   id="ToVersion" :filterable="true"
                        ref="ToVersion" :label-in-value="true" v-model="smToKey.ver">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in toCurVersions"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="toKeyShow.sn"  for="ToMethod">{{'ToMethod' | i18n}}</Label>
               <!-- <Input v-if="toKeyShow.sn"  id="ToMethod" v-model="smToKey.sm"/>-->
                <Select v-if="toKeyShow.sn"   id="ToMethod" :filterable="true"
                        ref="ToMethod" :label-in-value="true" v-model="smToKey.sm">
                    <Option value="*" >none</Option>
                    <Option v-for="(v) in toCurMethods"  :value="v"  v-bind:key="v">{{v}}</Option>
                </Select>

                <Label v-if="cfg.toType == 'DB' "  for="db">{{'Table' | i18n}}</Label>
                <Input v-if="cfg.toType ==  'DB' "  id="db" v-model="cfg.toParams"/>

                <Label v-if="cfg.toType == 'File'"  for="fileName">{{'File' | i18n}}</Label>
                <Input v-if="cfg.toType == 'File'"  id="fileName" v-model="cfg.toParams"/>

                <!--<Label for="timeUnit">{{'timeUnit' | i18n}}</Label>
                <Select id="timeUnit" v-model="cfg.timeUnit">
                    <Option v-for="k in timeUnits" :value="k" :key="k">{{k | i18n}}</Option>
                </Select>

                <Label for="timeCnt">{{'timeCnt' | i18n}}</Label>
                <Input id="timeCnt" v-model="cfg.timeCnt"/>-->

              <!--  <Label for="actName">{{'ActName' | i18n}}</Label>
                <Input id="actName" v-model="cfg.actName"/>-->

                <Label for="statisIndex">{{'statisIndex' | i18n}}</Label>
                <CheckboxGroup id="statisIndex" v-model="cfg.statisIndexs">
                    <Checkbox v-for="k in statisIndex" :key="k" :label="k" :value="k">
                        <span>{{ k | i18n }}</span>
                    </Checkbox>
                </CheckboxGroup>

                <Label  for="tag">{{'Tag' | i18n}}</Label>
                <Input  id="tag" v-model="cfg.tag"/>

                <Checkbox v-model="cfg.enable">{{ 'Enable' | i18n}}</Checkbox><br/>

            </div>
        </Modal>

    </div>
</template>

<script>

    const UNIT_SE = "Second";
    const UNIT_MU = "Munites";
    const UNIT_HO = "Hour";
    const UNIT_DA = "Date";
    const UNIT_MO = "Month";

   /* const BY_TYPE_SERVICE = "Service";
    const BY_TYPE_SERVICE_ACCOUNT='ServiceAccount';
    const BY_TYPE_SERVICE_INSTANCE = "ServiceInstance";*/

    const BY_TYPE_SERVICE_INSTANCE_METHOD = "ServiceInstanceMethod";
    const BY_TYPE_SERVICE_ACCOUNT_METHOD = "ServiceAccountMethod";
    const BY_TYPE_SERVICE_METHOD = "ServiceMethod";

    //const BY_TYPE_CLIENT_INSTANCE = "ClientInstance";
    const BY_TYPE_INSTANCE = "Instance";
    const BY_TYPE_ACCOUNT = "Account";

    //const BY_TYPE_EXP  = "Expression";

    const TO_TYPE_DB = "DB";
    const TO_TYPE_SERVICE_METHOD = "ServiceMethod";
    const TO_TYPE_CONSOLE = "Console";
    const TO_TYPE_FILE = "File";

    const PREFIX_TOTAL = "total";
    const PREFIX_TOTAL_PERCENT = "totalPercent";
    const PREFIX_QPS = "qps";
    const PREFIX_CUR = "cur";
    const PREFIX_CUR_PERCENT = "curPercent";

    const REMOTE_KEYS = [ window.jm.rpc.Constants.SERVICE_METHODS, window.jm.rpc.Constants.SERVICE_NAMESPACES,
        window.jm.rpc.Constants.SERVICE_VERSIONS,window.jm.rpc.Constants.INSTANCES ];

   /* const EXP_TYPE_SERVICE = 1;
    const EXP_TYPE_ACCOUNT = 2;
    const EXP_TYPE_INSTANCE = 3;*/

    const cid = 'statisConfig';

    const sn = 'cn.jmicro.monitor.statis.api.IStatisConfigService';
    const ns = 'mng';
    const v = '0.0.1';

    //const LOGS = ['No','Trance','Debug','Info','Warn','Error','Final'];

    export default {
        name: cid,
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
                timeUnits:[ UNIT_SE,UNIT_MU,UNIT_HO,UNIT_DA,UNIT_MO ],
                toTypes:[ TO_TYPE_DB,TO_TYPE_SERVICE_METHOD,TO_TYPE_CONSOLE,TO_TYPE_FILE ],
                byTypes:[ BY_TYPE_SERVICE_METHOD, BY_TYPE_SERVICE_INSTANCE_METHOD,BY_TYPE_SERVICE_ACCOUNT_METHOD ,
                    BY_TYPE_INSTANCE,BY_TYPE_ACCOUNT],
                statisIndex:[PREFIX_TOTAL,PREFIX_TOTAL_PERCENT,PREFIX_QPS,PREFIX_CUR,PREFIX_CUR_PERCENT],

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
            }
        },

        components: {

        },

        methods: {

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
                this.updateMode=false;
                if(!this.cfg.timeUnit || this.cfg.timeUnit.length == 0) {
                    this.cfg.timeUnit = UNIT_MU;
                }

                if(!this.cfg.timeCnt <= 0) {
                    this.cfg.timeCnt = 1;
                }

                //this.cfg = {};
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

            update(cfg) {

                let self = this;
                this.updateMode = true;
                this.cfg = cfg;

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

                this.addStatisConfigDialog = true;
            },

            doSave() {
                let self = this;

                this.$refs.addNodeDialog.buttonLoading = false;

                if(!this.cfg.statisIndexs || this.cfg.statisIndexs.length == 0) {
                    this.errMsg = '统计指标不能为空';
                    return;
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
                } /*else if(this.cfg.byType == BY_TYPE_EXP) {
                    if(!this.cfg.byKey || this.cfg.byKey.length == 0) {
                        this.errMsg = '表达式键值不能为空';
                        return;
                    }
                    if(!this.cfg.expForType || this.cfg.expForType.length == 0) {
                        this.errMsg = '表达式类型不能为空';
                        return;
                    }
                    if(!this.cfg.byKey || this.cfg.byKey.length == 0) {
                        this.errMsg = '表达式键值不能为空';
                        return;
                    }
                }*/else {
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
                        this.errMsg = '统计方法不能为空';
                        return;
                    }

                    if(!this.byKey.ins || this.byKey.ins.length == 0) {
                        this.byKey.ins='*';
                    }

                    this.cfg.byKey = this.byKey.sn+'##'+this.byKey.ns+'##'+this.byKey.ver+'##'+this.byKey.ins+'######'+this.byKey.sm+'##';
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
                    this.cfg.toParams = this.smToKey.sn+'##'+this.smToKey.ns+'##'+this.smToKey.ver+'########'+this.smToKey.sm+'##';
                }else if(self.cfg.toType == TO_TYPE_FILE ) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.errMsg =  self.cfg.type == 2 ? '数据库表名不能为空':'文件名不能为空';
                        return;
                    }
                }else if(self.cfg.toType == TO_TYPE_DB) {
                    if(!self.cfg.toParams || self.cfg.toParams.length == 0) {
                        self.cfg.toParams = 't_statis_data';
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
                }else {
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

            getServiceNames() {
                let self = this;
                window.jm.mng.comm.getDicts([window.jm.rpc.Constants.SERVICE_NAMES,
                    window.jm.rpc.Constants.NAMED_TYPES,window.jm.rpc.Constants.ALL_INSTANCES,],'')
                    .then((opts)=>{
                        if(opts) {
                            self.serviceNames = opts[window.jm.rpc.Constants.SERVICE_NAMES];
                            self.namedTypeNames = opts[window.jm.rpc.Constants.NAMED_TYPES];
                            self.allInstances = opts[window.jm.rpc.Constants.ALL_INSTANCES];
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