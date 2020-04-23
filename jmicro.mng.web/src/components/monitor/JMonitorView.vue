<template>
    <div class="JMonitorContentView">
        <div>
            <Tabs :value="!!selectNode ? selectNode.id:''" type="card" :closable="allowMany" @on-tab-remove="handleTabRemove"
                  :animated="false">
                <TabPane v-for="(item) in items"  :name="item.id" :label="item.id" :key="item.id">
                   <!-- <p stype="word-break: break-all;padding: 0px 10px;font-size: medium;">{{item.id}}</p>-->
                    <JMonitorEditor :group="item"> </JMonitorEditor>
                 </TabPane>
            </Tabs>
        </div>

    </div>
</template>

<script>

    import JMonitorEditor from "./JMonitorEditor.vue"

    export default {
        name: 'JMonitorView',
        components: {
            JMonitorEditor
        },

        data () {
            let d = window.jm.mng.cache[this.dataId];
            if( !d) {
                d = window.jm.mng.cache[this.dataId] = {
                     items:[],
                     selectNode:null,
                };
            }
            return d;
        },

        props:{
            allowMany: {
                type: Boolean,
                default: false
            },
            dataId:{
                type:String,
                required:true,
            }
        },
        mounted:function() {
            let self = this;
            //console.log(window.jm.utils.isBrowser('ie'));
            window.jm.vue.$on('monitorNodeSelect',(nodes) => {
                if(!nodes || nodes.length ==0) {
                    return;
                }

                let node = nodes[0];

                if(!!self.selectNode && self.selectNode.id == node.id) {
                    return;
                }

                let is = self.items;
                let it = null;

                if(self.allowMany ) {
                    for(let i = 0; i < self.items.length; i++) {
                        if(self.items[i].id == node.id) {
                            it = self.items[i];
                            break;
                        }
                    }

                    if(!it) {
                        is.push(node);
                        self.items = is;
                        self.selectNode = node;
                    } else {
                        self.selectNode = it;
                    }
                } else {
                    is[0] = node;
                    self.items = is;
                    self.selectNode = node;
                }
            });
        },


        methods: {
            handleTabRemove (id) {
                let i = -1;
                for(let idx = 0;  idx < this.items.length; idx++ ) {
                    if(id == this.items[idx].id) {
                        i = idx;
                        break;
                    }
                }
                if(i > -1) {
                    this.items.splice(i,1);
                }
            },

        }
    }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
    .JMonitorContentView{
        height:auto;
        /* min-height: 500px;*/
        overflow:hidden;
    }

    .JStatisContentView .ivu-tabs-bar{
        margin-bottom: 5px;
    }

</style>
