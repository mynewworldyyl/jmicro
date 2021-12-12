<template>
    <div class="JRouteRuleEditor">
        <table v-if="isLogin" class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>{{'clientId'|i18n}}</td>
                <td>{{'InstanceName'|i18n}}</td>
                <td>{{'enable'|i18n}}</td><td>{{'priority'|i18n}}</td>
                <td>{{'type'|i18n}}</td><td>{{'from'|i18n}}</td><td>{{'toType'|i18n}}</td>
                <td>{{'toValue'|i18n}}</td>
                <td>{{'serviceName'}}</td>
                <td>{{'method'|i18n}}</td>
                <td>{{'namespace'|i18n}}</td>
                <td>{{'version'|i18n}}</td>
                <td style="width:110px">{{'Operation'|i18n}}</td></tr>
            </thead>
            <tr v-for="c in routeList" :key="c.uniqueId">
                <td>{{c.uniqueId}}</td><td>{{c.clientId}}</td><td>{{c.forIns}}</td>
               <td>{{c.enable}}</td><td>{{c.priority}}</td>
                <td>{{c.from.type}}</td><td>{{getFrom(c)}}</td><td>{{c.targetType}}</td><td>{{c.targetVal}}</td>
                <td>{{c.from.serviceName}}</td><td>{{c.from.method}}</td>
                <td>{{c.from.namespace}}</td><td>{{c.from.version}}</td>
                <td>&nbsp;
                    <a v-if="isLogin" @click="viewDetail(c)">{{'Detail'|i18n}}</a>&nbsp;&nbsp;
                    <a v-if="isLogin" @click="updateRule(c)">{{'Update'|i18n}}</a>&nbsp;&nbsp;
                    <a v-if="isLogin" @click="deleteRule(c)">{{'Delete'|i18n}}</a>
                </td>
            </tr>
        </table>

        <div v-if="!isLogin">not login</div>

        <Drawer  v-if="isLogin && rule"  v-model="drawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="50" @close="closeDrawer()">

            <div><i-button v-if="drawerModel!=3" @click="onAddOk()">{{'Confirm'|i18n}}</i-button></div>

            <div style="color:red">{{errMsg}}</div>

            <Label for="enable">{{"Enable"|i18n}}</Label>
            <Checkbox id="enable"  v-model="rule.enable"/><br/>

            <Card style="width:100%">
                <Label for="instanceName">{{"instanceName"|i18n}}</Label>
                <Input id="instanceName" :disabled="drawerModel==3 || drawerModel==2" v-model="rule.forIns"/>

                <Label for="Priority">{{"Priority"|i18n}}</Label>
                <Input id="Priority" :disabled="drawerModel==3"  v-model="rule.priority"/>


            </Card>

            <Card style="width:100%">
                <p slot="title">
                    <Icon type="ios-film-outline"></Icon>
                    {{"From"|i18n}}
                </p>

                <Label for="type">{{"FromType"|i18n}}</Label>
                <Select id="type" v-model="rule.from.type" :disabled="drawerModel==3">
                    <Option value="ipRouter">{{"IPRouter"|i18n}}</Option>
                    <Option value="tagRouter">{{"TagRouter"|i18n}}</Option>
                    <Option value="serviceRouter">{{"ServiceRouter"|i18n}}</Option>
                    <Option value="instancePrefixRouter">{{"instancePrefixRouter"|i18n}}</Option>
                    <Option value="accountRouter">{{"accountRouter"|i18n}}</Option>
                    <Option value="instanceRouter">{{"instanceRouter"|i18n}}</Option>
                    <Option value="guestRouter">{{"guestRouter"|i18n}}</Option>
                    <Option value="notLoginRouter">{{"notLoginRouter"|i18n}}</Option>

                </Select>

                <Label for="serviceName">{{"serviceName"|i18n}}</Label>
                <Input  id="serviceName" v-model="rule.from.serviceName" :disabled="drawerModel==3"/>

                <Label for="namespace">{{"namespace"|i18n}}</Label>
                <Input id="namespace"  v-model="rule.from.namespace" :disabled="drawerModel==3"/>

                <Label for="version" >{{"version"|i18n}}</Label>
                <Input  id="version"   v-model="rule.from.version" :disabled="drawerModel==3"/>

                <Label for="method" >{{"method"|i18n}}</Label>
                <Input  id="method" v-model="rule.from.method" :disabled="drawerModel==3"/>

                <Label for="tagKey" v-if="rule.from.type=='tagRouter'">{{"tagKey"|i18n}}</Label>
                <Input id="tagKey" v-if="rule.from.type=='tagRouter'" v-model="rule.from.tagKey" :disabled="drawerModel==3"/>

                <Label v-if="rule.from.type!='serviceRouter'" for="fromLabelVal">{{fromLabel}}</Label>
                <Input  v-if="rule.from.type!='serviceRouter'" id="fromLabelVal" v-model="rule.from.val" :disabled="drawerModel==3"/>
            </Card>

            <Card style="width:100%">
                <p slot="title">
                    <Icon type="ios-film-outline"></Icon>
                    {{"To"|i18n}}
                </p>
                <Label for="targetType">{{"targetType"|i18n}}</Label>
                <Select id="targetType" v-model="rule.targetType" :disabled="drawerModel==3">
                    <Option value="instanceName">{{"instanceName"|i18n}}</Option>
                    <Option value="instancePrefix">{{"instancePrefix"|i18n}}</Option>
                    <Option value="ipPort">{{"ipPort"|i18n}}</Option>
                </Select>

                <Label for="tipPort">{{targetLabel}}</Label>
                <Input  id="tipPort" v-model="rule.targetVal" :disabled="drawerModel==3"/>
            </Card>

            <Card>
                <Label for="clientId">{{"ClientId"|i18n}}</Label>
                <Input id="clientId" :disabled="!isAdmin"  v-model="rule.clientId"/>

                <Label for="createdBy">{{"createdBy"|i18n}}</Label>
                <Input id="createdBy" :disabled="true"  :value="rule.createdBy"/>

                <Label for="updatedBy">{{"updatedBy"|i18n}}</Label>
                <Input id="updatedBy" :disabled="true"  :value="rule.updatedBy"/>

                <Label for="createdTime">{{"CreatedTime"|i18n}}</Label>
                <Input id="createdTime" :disabled="true"  :value="getDateStr(rule.createdTime)"/>

                <Label for="updatedTime">{{"UpdatedTime"|i18n}}</Label>
                <Input id="updatedTime" :disabled="true"  :value="getDateStr(rule.createdTime)"/>
            </Card>
        </Drawer>

    </div>
</template>

<script>

    import {formatDate} from "../common/JFilters.js";
    import cons from "@/rpc/constants"

    const cid = 'routeRuleEditor';

    const sn = 'cn.jmicro.mng.api.IRouteRuleConfigServiceJMSrv';
    const ns = cons.NS_MNG;
    const v = '0.0.1';

    export default {
        name: 'JRouteRuleEditor',
        data () {
            return {
                routeList:[],
                errMsg:'',
                drawerModel:0,//0无效，1:新增，2：更新，3：查看明细
                isLogin:false,
                isAdmin:false,
                rule:this.resetRule(),
                fromLabel:"",
                targetLabel:"",
                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },
            }
        },

        watch:{
           "rule.from.type":function(){
               this.fromLabel = this.getFromLabel();
            },
            "rule.targetType":function(){
                this.targetLabel = this.getTargetLabel();
            }
        },

        methods: {
            getDateStr(time) {
                return formatDate(time,2);
            },

            getTo(r) {
                return r.targetVal;
            },

            getTargetLabel(){
                if(!this.rule.targetType) {
                    return "instanceName";
                }else {
                    return this.rule.targetType;
                }
            },

            getFromLabel(){
                if(!this.rule.from.type) {
                    return "TagRouter";
                }else {
                    return this.rule.from.type;
                }
            },

            getFrom(r) {
                if(r.type=='tagRouter') {
                    return r.from.tagKey+"="+r.from.val;
                }

                if(r.type=='serviceRouter') {
                    return r.from.serviceName+":"+r.from.namespace+":"+r.from.version+":" + r.from.method;
                }
                return  r.from.val;
            },

            viewDetail(pi) {
                this.drawerModel = 3;
                this.rule = pi;
                this.drawer.drawerStatus = true;
            },

            closeDrawer(){
                this.drawer.drawerStatus = false;
                this.drawerModel = 0;
                this.resetRule();
            },

            addRule() {
                this.resetRule();
                this.drawer.drawerStatus = true;
                this.drawerModel = 1;
            },

            updateRule(r) {
                this.rule = r;
                this.drawer.drawerStatus = true;
                this.drawerModel = 2;
            },

            resetRule() {
                this.errMsg = '';
                return this.Rule = {
                    from:{type:"ipRouter"},
                    enable:false,
                    targetType:"instanceName",
                    targetVal:"",
                    clientId:this.$jr.auth.actInfo.id,
                    createdTime:new Date().getTime(),
                    updatedTime:new Date().getTime(),
                    createdBy:this.$jr.auth.actInfo.id,
                    updatedBy:this.$jr.auth.actInfo.id,
                }
            },

            onAddOk(){

                let self = this;
                self.errMsg = '';

                if(!self.rule.forIns) {
                    self.errMsg = "Instance name cannot be NULL";
                    return;
                }

                if(self.rule.enable) {

                    if(!this.rule.from.serviceName) {
                        self.errMsg = "Service name cannot be NULL";
                        return;
                    }

                    if(this.rule.from.type=='ipRouter' && !this.rule.from.val) {
                        self.errMsg = "IP and Port cannot be NULL";
                        return;
                    }

                    if(this.rule.from.type=='tagRouter' && (!this.rule.from.tagKey
                        || !this.rule.from.tagVal)) {
                        self.errMsg = "TagKey and TagVal cannot be NULL";
                        return;
                    }

                    if(!this.rule.targetVal) {
                        self.errMsg = "Target value cannot be NULL";
                        return;
                    }

                    if(!this.rule.priority) {
                        this.rule.priority = 1;
                    }
                }

                if(self.drawerModel == 2) {
                    this.$jr.rpc.callRpcWithParams(sn,ns,v,'update',[self.rule])
                        .then((resp)=>{
                        if( resp.code == 0 ) {
                            self.errMsg = '';
                            self.$Message.success(resp.msg);
                            self.closeDrawer();
                        }else {
                            self.errMsg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }else if(self.drawerModel == 1) {
                    this.$jr.rpc.callRpcWithParams(sn,ns,v,'add',[self.rule])
                        .then((resp)=>{
                        if( resp.code == 0 ) {
                            self.routeList.push(resp.data);
                            self.closeDrawer();
                            self.errMsg = '';
                        } else {
                            //self.$Message.error(resp.msg);
                            self.errMsg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                        self.$Message.error(err);
                    });
                }
            },

            deleteRule(res){
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn,ns,v,'delete',[res.forIns,res.uniqueId])
                    .then((resp)=>{
                    if(resp.code == 0 ) {
                        for(let i = 0; i < self.routeList.length; i++) {
                            if(self.routeList[i].id == res.id) {
                                self.routeList.splice(i,1);
                                return;
                            }
                        }
                    }else {
                        self.$Message.error(resp.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    if(err && err.errorCode && err.msg) {
                        self.$Message.error(err.msg);
                    }else {
                        self.$Message.error(err);
                    }
                });
            },

            refresh(){
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                this.isAdmin= this.$jr.rpcisAdmin();
                if(!this.isLogin) {
                    this.routeList = [];
                    return;
                }
                this.$jr.rpc.callRpcWithParams(sn,ns,v,'query',[])
                    .then((resp)=>{
                    if(resp.code != 0 ) {
                        self.$Message.error(resp.msg);
                        return;
                    }
                    self.routeList = resp.data;
                }).catch((err)=>{
                    window.console.log(err);
                    if(err && err.errorCode && err.msg) {
                        self.$Message.error(err.msg);
                    }else {
                        self.$Message.error(err);
                    }
                });
            },

        },

        mounted () {

            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            let self = this;
            self.errMsg = "";

            self.refresh();

            this.$jr.auth.addActListener(cid,self.refresh);
            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"ADD",label:"Add",icon:"ios-cog",call:self.addRule},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
             });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);

        },
    }
</script>

<style>
    .JRouteRuleEditor{
        height:auto;
    }

</style>