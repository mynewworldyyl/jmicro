<template>
    <div class="JDeploymentDesc">
        <table v-if="isLogin" class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td style="width:390px;">{{'jarFile'|i18n}}</td><td>{{'Enable'|i18n}}</td>
                <td style="width:80px;">{{'instanceNum'|i18n}}</td><td>{{'Stragety'|i18n}}</td>
            <!--<td>STRATEGY ARGS</td><td>ARGS</td>--><td style="width:110px">{{'Operation'|i18n}}</td></tr>
            </thead>
            <tr v-for="c in deployList" :key="c.id">
                <td>{{c.id}}</td><td>{{c.jarFile}}</td><td>{{c.enable}}</td><td>{{c.instanceNum}}</td>
                <td>{{c.assignStrategy}}</td><!--<td>{{c.strategyArgs}}</td><td>{{c.args}}</td>-->
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
            <Checkbox :disabled="drawerModel==3" v-model="deployment.enable">{{'Enable'|i18n}}</Checkbox>
            <br/>
            <div style="color:red">{{errMsg}}</div>

            <Label for="jarFile">JAR FILE</Label>
            <Input id="jarFile" :disabled="drawerModel==3"  v-model="deployment.jarFile"/>

            <Label for="instanceNum">INSTANCE NUM</Label>
            <Input :disabled="drawerModel==3" id="instanceNum" v-model="deployment.instanceNum"/>

            <Label for="assignStrategy">STRATEGY</Label>
            <Input :disabled="drawerModel==3" id="assignStrategy" v-model="deployment.assignStrategy"/>

            <Label for="strategyArgs">STRATEGY ARGS</Label>
            <Input :disabled="drawerModel==3" id="strategyArgs"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="deployment.strategyArgs"/>

            <Label for="args">ARGS</Label>
            <Input :disabled="drawerModel==3" id="args"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="deployment.args"/>
        </Drawer>

    </div>
</template>

<script>

    const cid = 'deploymentDesc';
    export default {
        name: 'JDeploymentDesc',
        data () {
            return {
                deployList:[],
                errMsg:'',
                drawerModel:0,//0无效，1:新增，2：更新，3：查看明细
                isLogin:false,

                deployment:{
                    id:null,
                    jarFile:'',
                    instanceNum:1,
                    args:'',
                    enable:false
                },

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

            }
        },
        methods: {

            viewDetail(pi) {
                this.drawerModel = 3;
                this.deployment = pi;
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
                this.drawer.drawerStatus = true;
                this.drawerModel = 2;
            },

            resetDeployment() {
                this.deployment = {
                    id : null,
                    jarFile:'',
                    instanceNum:1,
                    args:'',
                    enable:false
                }
            },

            onAddOk(){

                let self = this;

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
                    window.jm.mng.choy.updateDeployment(self.deployment).then((resp)=>{
                        if( resp.code != 0 || !resp.data ) {
                            self.$Message.success(resp.msg);
                            this.closeDrawer();
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }else if(self.drawerModel == 1) {
                    window.jm.mng.choy.addDeployment(self.deployment).then((resp)=>{
                        if( resp.code == 0 ) {
                            self.deployList.push(resp.data);
                            this.closeDrawer();
                        } else {
                            self.$Message.error(resp.msg);
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                        self.$Message.error(err);
                    });
                }
            },

            deleteDeployment(res){
                let self = this;
                window.jm.mng.choy.deleteDeployment(res.id).then((resp)=>{
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
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    this.deployList = [];
                    return;
                }
                window.jm.mng.choy.getDeploymentList().then((resp)=>{
                    if(resp.code != 0 ) {
                        self.$Message.error(resp.msg);
                        return;
                    }
                    this.deployList = resp.data;
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
            self.refresh();
            window.jm.rpc.addActListener(cid,self.refresh);
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"ADD",label:"Add",icon:"ios-cog",call:self.addDeploy},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
             });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
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