<template>
    <div class="JProcess">
        <a @click="refresh()">REFRESH</a>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" v-model="showAll"/>ALL
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>NAME</td><td>HOST</td><td>PROCESS ID</td>
                <td>AGENT HOST</td><td>AGENT NAME</td><td>AGENT ID</td> <td>AGENT PROCESS ID</td>
                <td>DEP ID</td>  <td>WORK DIR</td><td>ACTIVE</td>
                <td>OPERATION</td></tr>
            </thead>
            <tr v-for="a in processList" :key="a.id">
                <td>{{a.id}}</td> <td>{{a.instanceName}}</td> <td>{{a.host}}</td> <td>{{a.pid}}</td>
                <td>{{a.agentHost}}</td> <td>{{a.agentInstanceName}}</td> <td>{{a.agentId}}</td>
                <td>{{a.agentProcessId}}</td>
                <td>{{a.depId}}</td> <td><span :title="a.workDataDir">dir</span></td> <td>{{a.active}}</td>
                <td>&nbsp;
                   <!-- <a @click="deleteRes(a)">DELETE</a>-->
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
            }
        },
        methods: {

            refresh(){
                let self = this;
                window.jm.mng.conf.getChildren(window.jm.mng.INSTANCE_ROOT,true).then((processList)=>{
                    if(!processList || processList.length == 0 ) {
                        self.$Message.success("No data to show");
                        return;
                    }

                    this.processList =[];
                    for(let i = 0; i < processList.length; i++) {
                        let e = JSON.parse(processList[i].val);
                        //e.startTime0 = new Date(e.startTime).format("yyyy-MM-dd hh:mm:ss");
                        if(self.showAll) {
                            this.processList.push(e);
                        }else if(e.agentProcessId) {
                            this.processList.push(e);
                        }

                    }

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
    .JAgent{

    }



</style>