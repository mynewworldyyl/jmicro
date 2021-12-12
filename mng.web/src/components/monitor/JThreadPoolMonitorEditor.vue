<template>
    <div class="JThreadPoolMonitorEditor">
        <div  v-if="isLogin">
            <a @click="refresh()">REFRESH</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
            <br/>
            <p>{{item.key}}</p>
        </div>

        <table v-if="isLogin && itemList && itemList.length > 0" class="configItemTalbe" width="99%">
            <thead><tr><td>instanceName</td><td>activeCount</td> <td>completedTaskCount</td><td>largestPoolSize</td><td>poolSize</td>
                <td>taskCount</td><td>curQueueCnt</td><td>startCnt</td><td>endCnt</td><td>terminal</td><td>coreSize</td><td>maxPoolSize</td><td>taskQueueSize</td></tr></thead>
           <tr v-for="it in itemList" :key="it.id">
                <td>{{ it.val.ec.threadNamePrefix }}</td> <td>{{ it.val.activeCount }}</td><td>{{it.val.completedTaskCount}}</td>
                <td>{{it.val.largestPoolSize}}</td>
                <td>{{ it.val.poolSize }}</td><td>{{ it.val.taskCount }}</td>
               <td>{{ it.val.curQueueCnt }}</td><td>{{ it.val.startCnt }}</td>
                <td>{{ it.val.endCnt }}</td><td>{{ it.val.terminal }}</td>
                <td>{{ it.val.ec.msCoreSize }}</td><td>{{ it.val.ec.msMaxSize }}</td><td>{{ it.val.ec.taskQueueSize }}</td>
            </tr>

        </table>

        <div v-if="isLogin && itemList && itemList.length == 0">No data</div>
        <div v-if="!isLogin">Not login</div>

    </div>
</template>

<script>
    import TreeNode from "../common/JTreeNode.js"
    import threadPoolSrv from "@/rpcservice/threadPoolSrv"
    
    const cid  = 'JThreadPoolMonitorEditor';
    const GROUP = 'threadPool';
    export default {
        name: cid,

        data () {
            let its =  this.parseItem();
            return {
                isLogin : false,
                itemList : its,
            }
        },

        props:{
            item : {
                type:Object,
            },
        },

        methods: {

            parseItem() {
                let its = null;
                if(this.item.type == 'mo') {
                        its = [],
                        its.push(this.item);
                }else {
                    its = this.item.children;
                }
                return its;
            },

            refresh(){
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                if(!this.isLogin) {
                    self.itemList = [];
                    return;
                }
                threadPoolSrv.getInfo(this.item.id,this.item.type).then((resp)=>{
                    if(resp.code != 0 && resp.data.length > 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }

                    let its = resp.data;
                    self.item.children = null;
                    if(self.item.type == 'ins' ) {
                        self.item.children = [];
                        for(let i = 0; i < its.length; i++) {
                            let n = its[i];
                            let t =  n.ec.threadNamePrefix;
                            let s = new TreeNode(n.key, t,null,self.item,n,t);
                            s.group = GROUP;
                            s.type = 'mo';
                            self.item.addChild(s);
                            self.itemList = self.item.children;
                        }
                    } else {
                        self.item.val = resp.data[0];
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            let self = this;
            this.$jr.auth.addActListener(cid,()=>{
                self.refresh();
            });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);
            self.refresh();
        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },

    }
</script>

<style>
    .JThreadPoolMonitorEditor{

    }

    .configItemTalbe tr:hover {
        background-color:yellow;
    }


</style>