<template>
    <div class="JAgent">

       <!-- <a @click="refresh()">REFRESH</a>
        <input type="checkbox" v-model="showAll"/>SHOW ALL-->

        <table class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>PRIVATE</td><td>ACTIVE</td> <td>DEPS</td><td>INTS</td>
                <td>START TIME</td><td>CONTINUTE</td>
               <td>HOST</td><td>STATIS</td><td>OPERATION</td></tr></thead>
            <tr v-for="a in agentList" :key="a.id">
                <td>{{ a.agentInfo.id }}</td><td>{{a.agentInfo.privat}}</td>
                <td>{{ a.agentInfo.active }}</td>
                <td>{{ a.depIds ? a.depIds.join(',') : '' }}</td>
                <td>{{ a.intIds ? a.intIds.join(',') : '' }}</td>
                <td>{{ a.agentInfo.startTime0 }}</td><td>{{ a.agentInfo.continue }}</td>

                <td>{{ a.agentInfo.host }}</td>
                <td>{{JSON.stringify(a.agentInfo.ss)}}</td>
                <td>
                    <a v-if="isLogin && ( !a.intIds || a.intIds.length == 0)" @click="privateAgent(a.agentInfo.id)">ChangeStatu</a>
                    &nbsp;&nbsp;&nbsp;<a v-if="isLogin" @click="clearResourceCache(a.agentInfo.id)">ClearRes</a>
                    &nbsp; &nbsp; &nbsp;<a v-if="isLogin" @click="stopAllInstance(a.agentInfo.id)">StopAllInstance</a>
                </td>
            </tr>
        </table>
    </div>
</template>

<script>

    import choy from "@/rpcservice/choy"

    const cid = 'agent';

    export default {
        name: 'JAgent',
        data () {
            return {
                agentList:[],
                showAll:false,
                isLogin:false,
            }
        },
        methods: {

           async refresh(){
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                if(!this.isLogin) {
                    this.agentList =[];
                    return;
                }

                 let resp = await choy.getAgentList(this.showAll);

               if(resp.code != 0 && !resp.data || resp.data.length == 0 ) {
                   self.$Message.success(resp.msg || "no data");
                   this.agentList =[];
                   return;
               }

               this.agentList =[];
               for(let i = 0; i < resp.data.length; i++) {
                   let e = resp.data[i];
                   let d = new Date(e.agentInfo.startTime);
                   e.agentInfo.startTime0 = d.format("yyyy-MM-dd hh:mm:ss");
                   e.agentInfo.continue = d.toDecDay();
                   this.agentList.push(e);
               }

           },

             privateAgent(agentId) {
                let self = this;
                 choy.changeAgentState(agentId).then((resp)=>{
                    if(resp.code == 0) {
                        this.refresh();
                    } else {
                        self.$Message.success(resp.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
            },

            stopAllInstance(agentId) {
                let self = this;
                choy.stopAllInstance(agentId).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                    }else {
                        self.$Message.success("success");
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
            }
            ,clearResourceCache(agentId) {
                let self = this;
                choy.clearResourceCache(agentId).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                    }else {
                        self.$Message.success("success");
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
            }

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.refresh);
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"ShowAll",label:"Show All",icon:"ios-cog",call: ()=>{
                        self.showAll = !self.showAll; self.refresh(); }},
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
    .JAgent{

    }
</style>