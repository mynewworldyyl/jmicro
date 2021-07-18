<template>
    <div class="JDeploymentDesc">
        <table v-if="isLogin" class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td style="width:390px;">{{'jarFile'|i18n}}</td>
                <td>{{'Status'|i18n}}</td><td style="width:80px;">{{'instanceNum'|i18n}}</td>
                <td>{{'ClientId'|i18n}}</td><td>{{'ResId'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td>
                <td style="width:110px">{{'Operation'|i18n}}</td></tr>
            </thead>
            <tr v-for="c in deployList" :key="c.id">
                <td>{{c.id}}</td><td>{{c.jarFile}}</td><td>{{ statusMap[c.status] | i18n }}</td><td>{{c.instanceNum}}</td>
                <td>{{c.clientId}}</td><td>{{c.resId}}</td><td>{{c.createdTime | formatDate(1)}}</td>
                <td>&nbsp;
                    <a v-if="isLogin" @click="viewDetail(c)">{{'Detail'|i18n}}</a>&nbsp;&nbsp;
                    <a v-if="isLogin" @click="updateDeployment(c)">{{'Update'|i18n}}</a>&nbsp;&nbsp;
                    <a v-if="isLogin" @click="deleteDeployment(c)">{{'Delete'|i18n}}</a>
                </td>
            </tr>
        </table>

        <div v-if="!isLogin">not login</div>

        <Drawer  v-if="isLogin && deployment"  v-model="drawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="50" @close="closeDrawer()">
            <div><i-button v-if="drawerModel!=3" @click="onAddOk()">{{'Confirm'|i18n}}</i-button></div>

           <!-- <Checkbox :disabled="drawerModel==3" v-model="deployment.enable">{{'Enable'|i18n}}</Checkbox>-->
            <Label for="status">{{"Status"|i18n}}</Label>
            <Select id="status" ref="levelSelect" :label-in-value="true" v-model="deployment.status">
                <Option :value="k" v-for="(v,k) in statusMap" v-bind:key="k">{{v}}</Option>
            </Select>

            <div style="color:red">{{errMsg}}</div>

            <Label for="jarFile">JAR FILE &nbsp;&nbsp;&nbsp;
                <a @click="getJarFiles()">{{"Refresh"|i18n}}</a></Label>
            <!--<Input :disabled="true"  v-model="deployment.jarFile"/>-->
            <Select id="jarFile" :label-in-value="true" v-model="deployment.resId">
                <Option :value="j.id" v-for="j in jarFiles" v-bind:key="j.id">{{j.name}}</Option>
            </Select>

            <Label for="instanceNum">INSTANCE NUM</Label>
            <Input :disabled="drawerModel==3" id="instanceNum" v-model="deployment.instanceNum"/>

            <Label for="assignStrategy">STRATEGY</Label>
            <Input :disabled="drawerModel==3" id="assignStrategy" v-model="deployment.assignStrategy"/>

            <Label for="programargs">{{'programArgs'|i18n("Program Args")}}</Label>
            <Input :disabled="drawerModel==3" id="programargs"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="deployment.args"/>

            <Label for="jvmArgs">{{'jvmArgs'|i18n("JVM Args")}}</Label>
            <Input :disabled="drawerModel==3" id="jvmArgs"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="deployment.jvmArgs"/>

            <Label for="strategyArgs">STRATEGY ARGS</Label>
            <Input :disabled="drawerModel==3" id="strategyArgs"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="deployment.strategyArgs"/>

            <Label for="desc">{{"Desc"|i18n}}</Label>
            <Input id="desc"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="deployment.desc"/>

            <Label for="clientId">{{"ClientId"|i18n}}</Label>
            <Input id="clientId" :disabled="true"  v-model="deployment.clientId"/>

            <Label>{{"Dependencies"|i18n}}&nbsp;&nbsp;&nbsp;
                <a @click="dependencyList()">{{"Refresh"|i18n}}</a></Label>
            <p v-for="(d,i) in depIds"  :key="i" v-html="showDep(d)">
            </p>

            <br/>

            <Label for="createdTime">{{"CreatedTime"|i18n}}</Label>
            <Input id="createdTime" :disabled="true"  :value="getDateStr(deployment.createdTime)"/>

            <Label for="updatedTime">{{"UpdatedTime"|i18n}}</Label>
            <Input id="updatedTime" :disabled="true"  :value="getDateStr(deployment.createdTime)"/>


        </Drawer>

    </div>
</template>

<script>

    import {formatDate} from "../common/JFilters.js";

    import rep from "@/rpcservice/repository"
    import choy from "@/rpcservice/choy"
    import rpc from "@/rpc/rpcbase"
    import jmconfig from "@/rpcservice/jm"
    
    const cid = 'deploymentDesc';
    export default {
        name: 'JDeploymentDesc',
        data () {
            return {
                statusMap:jmconfig.DEP_STATUS,
                deployList:[],
                jarFiles:[],
                resMap:{},

                errMsg:'',
                drawerModel:0,//0无效，1:新增，2：更新，3：查看明细
                isLogin:false,
                res:0,

                depIds:[],

                deployment:{
                    id:null,
                    jarFile:'',
                    instanceNum:1,
                    args:'',
                    status:'1',
                    desc:'',
                    createdTime:new Date().getTime(),
                    updatedTime:new Date().getTime(),
                },

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                resStatus: jmconfig.RES_STATUS,

            }
        },

        watch:{
            "deployment.resId":function(resid){
                if(resid && this.resMap[resid]) {
                    this.deployment.jarFile = this.resMap[resid].name;
                    this.deployment.depIds =[];

                    if(this.resMap[resid].depIds) {
                        this.depIds = this.resMap[resid].depIds;
                    }else {
                        this.depIds =[];
                    }
                }
            }
        },

        methods: {
            getDateStr(time) {
                return formatDate(time,2);
            },

            dependencyList() {
                if(!this.deployment.resId) {
                    return;
                }
                let self = this;
                self.errMsg = "";

                rep.dependencyList(self.deployment.resId)
                    .then((resp)=>{
                    if(resp.code == 0) {
                        self.depIds = [];
                        for(let i = 0; i < resp.data.length; i++) {
                            self.depIds.push(resp.data[i]);
                        }
                        self.resMap[self.deployment.resId].depIds = self.depIds;
                    }else {
                        self.errMsg = resp.msg;
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            showDep(d) {
                if(typeof d == 'object') {
                    let msg = d.id + ":" + d.name + ":";
                    let sty = "";
                    if(d.status == 4) {
                        sty = "color:red";
                    }else if(d.status == 5) {
                        sty = "color:yellow";
                    }else if(d.status == 6) {
                        sty = "color:chocolate";
                    }else if(d.status == 3) {
                        sty = "color:green";
                    }
                    msg += "<span style='"+sty+"'>"+this.resStatus[d.status]+"</span>";
                    return msg;
                }else {
                    return d;
                }
            },

            viewDetail(pi) {
                this.drawerModel = 3;
                this.deployment = pi;
                pi.status = pi.status +'';
                this.drawer.drawerStatus = true;
            },

            closeDrawer(){
                this.drawer.drawerStatus = false;
                this.drawerModel = 0;
                this.resetDeployment();
            },

            addDeploy() {
                this.resetDeployment();
                this.drawer.drawerStatus = true;
                this.drawerModel = 1;
            },

            updateDeployment(dep) {
                this.deployment = dep;
                dep.status = dep.status +'';
                this.drawer.drawerStatus = true;
                this.drawerModel = 2;

            },

            resetDeployment() {
                this.errMsg = '';
                this.deployment = {
                    id : null,
                    jarFile:'',
                    instanceNum:1,
                    args:'',
                    status:'1',
                    desc:'',
                    clientId:rpc.actInfo.id,
                    createdTime:new Date().getTime(),
                    updatedTime:new Date().getTime(),
                }
            },

            onAddOk(){

                let self = this;
                self.errMsg = '';

                    self.deployment.jarFile = self.deployment.jarFile.trim();
                if(self.deployment.jarFile.length == 0) {
                    self.errMsg = 'Jar File cannot be NULL';
                    return;
                }

                if(!self.deployment.instanceNum) {
                    self.errMsg = 'invalid'+ self.deployment.instanceNum;
                    return;
                }

                if(self.drawerModel == 2) {
                    choy.updateDeployment(self.deployment).then((resp)=>{
                        if( resp.code == 0 ) {
                            self.deployment.status = resp.data.status;
                            self.deployment.desc = resp.data.desc;
                            self.deployment.args = resp.data.args;
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
                    choy.addDeployment(self.deployment).then((resp)=>{
                        if( resp.code == 0 ) {
                            self.deployList.push(resp.data);
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

            deleteDeployment(res){
                let self = this;
                choy.deleteDeployment(res.id).then((resp)=>{
                    if(resp.code == 0 ) {
                        for(let i = 0; i < self.deployList.length; i++) {
                            if(self.deployList[i].id == res.id) {
                                self.deployList.splice(i,1);
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
                this.isLogin = rpc.isLogin();
                if(!this.isLogin) {
                    this.deployList = [];
                    return;
                }
                choy.getDeploymentList().then((resp)=>{
                    if(resp.code != 0 ) {
                        self.$Message.error(resp.msg);
                        return;
                    }
                    self.deployList = resp.data;
                }).catch((err)=>{
                    window.console.log(err);
                    if(err && err.errorCode && err.msg) {
                        self.$Message.error(err.msg);
                    }else {
                        self.$Message.error(err);
                    }
                });
            },

            getJarFiles() {
                let self = this;
                rpc.callRpcWithParams(rpc.sn, rpc.ns, rpc.v, 'getResourceListForDeployment', [{}]))
                    .then((resp)=>{
                        if(resp.code == 0){
                            self.jarFiles = resp.data;
                            self.jarFiles.forEach(e=>{
                                self.resMap[e.id] = e;
                            });
                        } else {
                            window.console.log(resp.msg);
                        }
                    }).catch((err)=>{
                    window.console.log(err);
                });
            }
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            let self = this;
            self.refresh();
            self.getJarFiles();

            rpc.addActListener(cid,self.refresh);
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"ADD",label:"Add",icon:"ios-cog",call:self.addDeploy},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
             });

            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

        },
    }
</script>

<style>
    .JDeploymentDesc{
        height:auto;
    }

</style>