<template>
    <div class="JAgent">
        <a @click="refresh()">REFRESH</a>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" v-model="showAll"/>SHOW ALL
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>NAME</td> <td>PRIVATE</td><td>ACTIVE</td> <td>DEPS</td><td>INTS</td>
                <td>START TIME</td><td>CONTINUTE</td>
               <td>HOST</td><td>STATIS</td><td>OPERATION</td></tr></thead>
            <tr v-for="a in agentList" :key="a.id">
                <td>{{ a.agentInfo.id }}</td><td>{{ a.agentInfo.name }}</td><td>{{a.agentInfo.privat}}</td>
                <td>{{ a.agentInfo.active }}</td>
                <td>{{ a.depIds ? a.depIds.join(',') : '' }}</td>
                <td>{{ a.intIds ? a.intIds.join(',') : '' }}</td>
                <td>{{ a.agentInfo.startTime0 }}</td><td>{{ a.agentInfo.continue }}</td>

                <td>{{ a.agentInfo.host }}</td>
                <td>{{JSON.stringify(a.agentInfo.ss)}}</td>
                <td>&nbsp;
                    <a v-if="adminPer && ( !a.intIds || a.intIds.length == 0)" @click="privateAgent(a.agentInfo.id)">CHANGE</a>
                </td>
            </tr>
        </table>
    </div>
</template>

<script>

    export default {
        name: 'JRepository',
        data () {
            return {
                agentList:[],
                showAll:false,
                adminPer:false,
            }
        },
        methods: {

            refresh(){
                let self = this;
                this.adminPer = window.jm.mng.comm.adminPer;
                window.jm.mng.choy.getAgentList(this.showAll).then((agentList)=>{
                    if(!agentList || agentList.length == 0 ) {
                        self.$Message.success("No data to show");
                        this.agentList =[];
                        return;
                    }

                    this.agentList =[];
                    for(let i = 0; i < agentList.length; i++) {
                        let e = agentList[i];
                        let d = new Date(e.agentInfo.startTime);
                        e.agentInfo.startTime0 = d.format("yyyy-MM-dd hh:mm:ss");
                        e.agentInfo.continue = d.toDecDay();
                        this.agentList.push(e);
                    }

                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            privateAgent(agentId) {
                let self = this;
                window.jm.mng.choy.changeAgentState(agentId).then((rst)=>{
                    if(rst == '') {
                        this.refresh();
                    } else {
                        self.$Message.success(rst);
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