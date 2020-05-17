<template>
    <div class="JDeploymentDesc">
        <a @click="addDeploy()">ADD</a>&nbsp;&nbsp;&nbsp;
        <a @click="refresh()">REFRESH</a>
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>JAR FILE</td><td>INSTANCE NUM</td><td>ARGS</td><td>ENABLE</td><td>OPERATION</td></tr></thead>
            <tr v-for="c in deployList" :key="c.id">
                <td>{{c.id}}</td><td>{{c.jarFile}}</td><td>{{c.instanceNum}}</td><td>{{c.args}}</td><td>{{c.enable}}</td>
                <td>&nbsp;
                    <a @click="deleteDeployment(c)">DELETE</a>&nbsp;&nbsp;&nbsp;
                    <a @click="updateDeployment(c)">UPDATE</a>
                </td>
            </tr>
        </table>

        <Modal v-model="addResourceDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="onAddOk()">
            <div>
                <Label for="jarFile">JAR FILE</Label>
                <Input id="jarFile"  v-model="deployment.jarFile"/>

                <Label for="instanceNum">INSTANCE NUM</Label>
                <Input id="instanceNum" v-model="deployment.instanceNum"/>

                <Label for="args">ARGS</Label>
                <Input id="args"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                       type="textarea" v-model="deployment.args"/>

                <Checkbox v-model="deployment.enable">ENABLE</Checkbox>
                <div style="color:red">{{errMsg}}</div>
              </div>
        </Modal>

    </div>
</template>

<script>

    export default {
        name: 'JDeploymentDesc',
        data () {
            return {
                deployList:[],
                addResourceDialog:false,
                errMsg:'',
                doUpdate:false,

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
                    window.jm.mng.deployment.updateDeployment(self.deployment).then((success)=>{
                        if( success ) {
                            self.$Message.success("Fail to add deployment ");
                            this.addResourceDialog = false;
                        }
                        self.resetDeployment();
                    }).catch((err)=>{
                        window.console.log(err);
                        self.resetDeployment();
                    });
                }else {
                    window.jm.mng.deployment.addDeployment(self.deployment).then((dep)=>{
                        if( dep ) {
                            self.deployList.push(dep);
                            self.resetDeployment();
                            this.addResourceDialog = false;
                        } else {
                            self.$Message.fail("Fail to add deployment ");
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }
            },

            deleteDeployment(res){
                let self = this;
                window.jm.mng.deployment.deleteDeployment(res.id).then((rst)=>{
                    if(rst ) {
                        for(let i = 0; i < self.deployList.length; i++) {
                            if(self.deployList[i].id == res.id) {
                                self.deployList.splice(i,1);
                                return;
                            }
                        }
                    }else {
                        self.$Message.fail("Fail to delete resource "+res.name);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            refresh(){
                window.jm.mng.deployment.getDeploymentList().then((deployList)=>{
                    if(!deployList || deployList.length == 0 ) {
                        return;
                    }
                    this.deployList = deployList;
                }).catch((err)=>{
                    window.console.log(err);
                });
            }
        },

        mounted () {
            this.refresh();
        },
    }
</script>

<style>
    .JDeploymentDesc{

    }

</style>