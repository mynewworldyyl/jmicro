<template>
  <div class="JMainContentEditor">
    <div>
      <Tabs :value="!!selectNode ? selectNode.id:''" type="card" :closable="allowMany" @on-tab-remove="handleTabRemove"
            :animated="false">
            <TabPane v-for="(item) in items"  :name="item.id" :label="item.id" v-bind:key="item.id">
                <JRouterType v-if="item.type == 't'" :selectItem="item"></JRouterType>
                <JRouterGroup v-else-if="item.type == 'g'" :selectItem="item"></JRouterGroup>
            </TabPane>
      </Tabs>
    </div>

  </div>
</template>

<script>

    import JRouterType from './JRouterType.vue'
    import JRouterGroup from './JRouterGroup.vue'

    export default {
        name: 'JRouterEditor',
        components: {
            JRouterGroup,
            JRouterType
        },

        data () {
            return {
                items:[],
                selectNode:null,
            }
        },

        props:{
          allowMany: {type: Boolean, default: false},
        },

        mounted:function() {
            let self = this;
            window.jm.vue.$on('routerNodeSelect',(nodes) => {
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
  }

</style>
