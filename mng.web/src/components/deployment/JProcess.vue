<template>
    <div class="JProcess">
        <!--<a @click="refresh()">REFRESH</a>
        <input type="checkbox" v-model="showAll"/>ALL-->
        <div v-if="msg">{{msg}}</div>
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>NAME</td><td>ACTIVE</td><td>HA ENABLE</td><td>IS MASTER</td>
                <td>Log Level</td><td>PROCESS ID</td><td>START TIME</td><td>CONTINUTE</td><td>HOST</td>
                <td>AGENT ID</td> <!--<td>AGENT PROCESS ID</td>-->
                <td>DEP ID</td>
                <td>OPERATION</td></tr>
            </thead>
            <tr v-for="a in processList" :key="a.id">
                <td>{{a.id}}</td> <td>{{a.instanceName}}</td>
                <td>{{a.active}}</td><td>{{a.haEnable}}</td> <td>{{a.master}}</td><td>{{logLevels[a.logLevel]}}</td>
                <td>{{a.pid}}</td><td>{{ a.startTime0 }}</td><td>{{ a.continue }}</td><td>{{a.host}}</td>
                <td>{{a.agentId}}</td><!--<td>{{a.agentProcessId}}</td>--><td>{{a.depId}}</td>
                <td>&nbsp;
                   <a v-if="isLogin" @click="stopProcess(a)"> {{ "Stop" |i18n }} </a>
                    <a v-if="isLogin" @click="editProcessDrawer(a)"> {{ "Edit" |i18n }} </a>
                </td>
            </tr>
        </table>

        <Drawer  v-if="isLogin && editPi"  v-model="drawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="50">
            <div><i-button @click="saveProcessInfo()">{{'Confirm'|i18n}}</i-button></div>
            <table>
                <tr>
                    <td>{{'logLevel' | i18n}}</td>
                    <td>
                        <Select  ref="levelSelect" :label-in-value="true" v-model="editPi.logLevel">
                            <Option :value="k" v-for="(v,k) in logLevels" v-bind:key="k">{{v}}</Option>
                        </Select>
                    </td>
                    <td></td>
                    <td></td>
                </tr>

                <tr>
                    <td>{{'workDir' | i18n}}</td>
                    <td colspan="3">
                        {{editPi.workDir}}
                    </td>
                </tr>

            </table>
        </Drawer>

    </div>
</template>

<script>

    const cid = 'process';

    export default {
        name: 'JProcess',
        data () {
            return {
                msg:null,
                showAll:true,
                processList:[],
                isLogin : false,

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                logLevels: window.jm.mng.LOG2LEVEL,

                editPi: null,

            }
        },

        methods: {

            editProcessDrawer(pi) {
                this.editPi = pi;
                pi.logLevel = '' + pi.logLevel;

                this.drawer.drawerStatus = true;
                this.drawer.drawerBtnStyle.zindex = 10000;
                this.drawer.drawerBtnStyle.left = '0px';
            },

            saveProcessInfo(){
                if(!this.editPi) {
                    return;
                }
                let self = this;
                let pi = {id:this.editPi.id, logLevel:parseInt(this.editPi.logLevel)};
                window.jm.mng.choy.updateProcess(pi)
                 .then((resp)=>{
                    if(resp.code == 0) {
                        self.drawer.drawerStatus = false;
                        self.drawer.drawerBtnStyle.zindex = 100;
                        self.$Message.success("Success update process");
                    } else {
                        self.$Message.success(resp.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
            },

            refresh(){
                this.msg = null;
                let self = this;
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    this.processList = [];
                    this.msg = 'Not login';
                    return;
                }
                window.jm.mng.choy.getProcessInstanceList(self.showAll).then((resp)=>{
                    if(resp.code != 0 || !resp.data || resp.data.length == 0 ) {
                        self.$Message.success(resp.msg || "No data to show");
                        this.processList = [];
                        return;
                    }
                    this.processList =[];
                    for(let i = 0; i < resp.data.length; i++) {
                        let e = resp.data[i];
                        let d = new Date(e.startTime);
                        e.startTime0 = d.format("yyyy-MM-dd hh:mm:ss");
                        e.continue = d.toDecDay();
                        this.processList.push(e);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
            },

            stopProcess(pi) {
                let self = this;
                window.jm.mng.choy.stopProcess(pi.id).then((resp)=>{
                    if(resp.code == 0) {
                        pi.active = false;
                        self.$Message.success("Success stop process");
                    }else {
                        self.$Message.success(resp.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
            },

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            //has admin permission, only control the show of the button
            window.jm.rpc.addActListener(cid,this.refresh);
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":'process',
                    "menus":[{name:"ShowAll",label:"Show All",icon:"ios-cog",call: ()=>{
                                self.showAll = !self.showAll;
                                self.refresh();
                            }
                        },
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });
            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

            this.refresh();
        },
    }
</script>

<style>
    .JProcess{
        height:auto;
    }

    .ProcessDrawerBtnStatus{
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
</style>