<template>
    <div class="JThreadPoolMonitorEditor">
        <a @click="refresh()">REFRESH</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <br/>
        <p>{{item.key}}</p>
        <table class="configItemTalbe" width="99%">
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

    </div>
</template>

<script>
    import TreeNode from "../common/JTreeNode.js"
    const cid  = 'JThreadPoolMonitorEditor';
    const GROUP = 'threadPool';
    export default {
        name: cid,

        data () {
            let its =  this.parseItem();
            return {
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
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                window.jm.mng.threadPoolSrv.getInfo(this.item.id,this.item.type).then((resp)=>{
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
            let self = this;
            window.jm.mng.act.addListener(cid,()=>{
                self.refresh();
            });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

        },

        beforeDestroy() {
            window.jm.mng.act.removeActListener(cid);
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