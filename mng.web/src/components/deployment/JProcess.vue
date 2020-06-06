<template>
    <div class="JProcess">
        <a @click="refresh()">REFRESH</a>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" v-model="showAll"/>ALL
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>NAME</td><td>HOST</td><td>PROCESS ID</td>
                <td>HA ENABLE</td><td>IS MASTER</td><td>AGENT ID</td> <td>AGENT PROCESS ID</td>
                <td>DEP ID</td>  <td>WORK DIR</td><td>ACTIVE</td>
                <td>OPERATION</td></tr>
            </thead>
            <tr v-for="a in processList" :key="a.id">
                <td>{{a.id}}</td> <td>{{a.instanceName}}</td> <td>{{a.host}}</td> <td>{{a.pid}}</td>
                <td>{{a.haEnable}}</td> <td>{{a.master}}</td> <td>{{a.agentId}}</td>
                <td>{{a.agentProcessId}}</td>
                <td>{{a.depId}}</td> <td><span :title="a.workDir">dir</span></td> <td>{{a.active}}</td>
                <td>&nbsp;
                   <a v-if="adminPer" @click="stopProcess(a.id)"> STOP </a>
                </td>
            </tr>
        </table>
    </div>
</template>

<script>

    export default {
        name: 'JProcess',
        data () {
            return {
                showAll:false,
                processList:[],
                adminPer : false,
            }
        },
        methods: {

            refresh(){
                let self = this;
                this.hasPer(1);
                window.jm.mng.choy.getProcessInstanceList(self.showAll).then((processList)=>{
                    if(!processList || processList.length == 0 ) {
                        self.$Message.success("No data to show");
                        this.processList = [];
                        return;
                    }
                    this.processList =processList;
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            stopProcess(insId) {
                let self = this;
                window.jm.mng.choy.stopProcess(insId).then((rst)=>{
                    if(rst) {
                        self.$Message.success("Success stop process");
                    }else {
                        self.$Message.success("Failure to stop process");
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            hasPer(perId) {
                let self = this;
                window.jm.mng.comm.hasPermission(perId).then((rst)=>{
                    self.adminPer = rst;
                }).catch((err)=>{
                    window.console.log(err);
                });
            }
        },

        mounted () {
            //has admin permission, only control the show of the button
            this.refresh();
        },
    }
</script>

<style>
    .JAgent{

    }



</style>