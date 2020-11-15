<template>
    <div class="JDeploymentDesc">
       <!-- <div style="height:30px;line-height: 30px">
            <a @click="addDeploy()">ADD</a>&nbsp;&nbsp;&nbsp;
            <a @click="refresh()">REFRESH</a>
        </div>-->

        <table v-if="isLogin" class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>JAR FILE</td><td>ENABLE</td><td>INSTANCE NUM</td><td>STRATEGY</td>
            <td>STRATEGY ARGS</td><td>ARGS</td><td>OPERATION</td></tr>
            </thead>
            <tr v-for="c in deployList" :key="c.id">
                <td>{{c.id}}</td><td>{{c.jarFile}}</td><td>{{c.enable}}</td><td>{{c.instanceNum}}</td>
                <td>{{c.assignStrategy}}</td><td>{{c.strategyArgs}}</td><td>{{c.args}}</td>
                <td>&nbsp;
                    <a v-if="isLogin" @click="deleteDeployment(c)">DELETE</a>&nbsp;&nbsp;&nbsp;
                    <a v-if="isLogin" @click="updateDeployment(c)">UPDATE</a>
                </td>
            </tr>
        </table>

        <div v-if="!isLogin">not login</div>

        <Modal v-model="addResourceDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="onAddOk()">
            <div>
                <Checkbox v-model="deployment.enable">ENABLE</Checkbox>
                <br/>
                <div style="color:red">{{errMsg}}</div>

                <Label for="jarFile">JAR FILE</Label>
                <Input id="jarFile"  v-model="deployment.jarFile"/>

                <Label for="instanceNum">INSTANCE NUM</Label>
                <Input id="instanceNum" v-model="deployment.instanceNum"/>

                <Label for="assignStrategy">STRATEGY</Label>
                <Input id="assignStrategy" v-model="deployment.assignStrategy"/>

                <Label for="strategyArgs">STRATEGY ARGS</Label>
                <Input id="strategyArgs"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                       type="textarea" v-model="deployment.strategyArgs"/>

                <Label for="args">ARGS</Label>
                <Input id="args"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                       type="textarea" v-model="deployment.args"/>

              </div>
        </Modal>

    </div>
</template>

<script>

    const cid = 'deploymentDesc';
    export default {
        name: 'JDeploymentDesc',
        data () {
            return {
                deployList:[],
                addResourceDialog:false,
                errMsg:'',
                doUpdate:false,
                isLogin:false,

                deployment:{
                    id:null,
                    jarFile:'',
                    instanceNum:1,
                    args:'',
                    enable:true
                }
            }
        },
        methods: {

            addDeploy() {
                this.doUpdate = false;
                this.addResourceDialog = true;
            },

            updateDeployment(dep) {
                this.deployment = dep;
                this.doUpdate = true;
                this.addResourceDialog = true;
            },

            resetDeployment() {
                this.deployment = {
                    id : null,
                    jarFile:'',
                    instanceNum:1,
                    args:'',
                    enable:true
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

                if(self.doUpdate) {
                    window.jm.mng.choy.updateDeployment(self.deployment).then((resp)=>{
                        if( resp.code != 0 || !resp.data ) {
                            self.$Message.success(resp.msg);
                            this.addResourceDialog = false;
                        }
                        self.resetDeployment();
                    }).catch((err)=>{
                        window.console.log(err);
                        self.resetDeployment();
                    });
                }else {
                    window.jm.mng.choy.addDeployment(self.deployment).then((resp)=>{
                        if( resp.code == 0 ) {
                            self.deployList.push(resp.data);
                            self.resetDeployment();
                            this.addResourceDialog = false;
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