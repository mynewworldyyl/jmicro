<template>
  <div class="JMainContentEditor">
    <div>
      <Tabs :value="!!selectNode ? selectNode.id:''" type="card" :closable="allowMany"
            @on-tab-remove="handleTabRemove" :animated="false">
            <TabPane v-for="(item,index) in items"  :name="item.id"
                     :label="item.type=='method' ? item.title: (item.type + index)"  v-bind:key="item.id">
                <p stype="word-break: break-all;padding: 0px 10px;font-size: medium;">{{item.id}}</p>
                <JServiceItem v-if="item.type == 'sn'" :item="item"></JServiceItem>
                <JInstanceItem v-else-if="item.type == 'ins'" :item="item"></JInstanceItem>
                <JMethodItem v-else-if="item.type == 'method'" :meth="item"></JMethodItem>
            </TabPane>
        </Tabs>
    </div>

  </div>
</template>

<script>
    //import jm from '../../public/js/jm.js'
    import JServiceItem from './JServiceItem.vue'
    import JMethodItem from './JSMethodItem.vue'
    import JInstanceItem from './JInstanceItem.vue'

    import config from "@/rpc/config"

    export default {
        name: 'JServiceEditor',
        components: {
            JServiceItem,
            JMethodItem,
            JInstanceItem,
        },

        data () {
            let d = config.cache[this.dataId];
            if( !d) {
                d = config.cache[this.dataId] = {
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

        mounted : function() {
            var self = this;
            //console.log(window.jm.utils.isBrowser('ie'));
            this.$bus.$on('serviceNodeSelect',(nodes) => {
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
  .JMainContentEditor{
      height:auto;
     /* min-height: 500px;*/
      overflow:hidden;
  }

</style>
