<template>
    <div class="JAgent">
        <a @click="refresh()">REFRESH</a>
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>NAME</td><td>HOST</td><td>START TIME</td><td>DEPS</td><td>OPERATION</td></tr></thead>
            <tr v-for="a in agentList" :key="a.id">
                <td>{{a.id}}</td><td>{{a.name}}</td><td>{{a.host}}</td><td>{{a.startTime0}}</td><td>{{a.runningDeps}}</td>
                <td>&nbsp;
                   <!-- <a @click="deleteRes(a)">DELETE</a>-->
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
            }
        },
        methods: {

            refresh(){
                window.jm.mng.conf.getChildren(window.jm.mng.AGENT_ROOT,true).then((agentList)=>{
                    if(!agentList || agentList.length == 0 ) {
                        return;
                    }

                    this.agentList =[];
                    for(let i = 0; i < agentList.length; i++) {
                        let e = JSON.parse(agentList[i].val);
                        e.startTime0 = new Date(e.startTime).format("yyyy-MM-dd hh:mm:ss");
                        this.agentList.push(e);
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