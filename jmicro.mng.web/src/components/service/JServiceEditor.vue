<template>
  <div class="JMainContentEditor">
    <div>
      <Tabs :value="!!selectNode ? selectNode.id:''" type="card" :closable="allowMany" @on-tab-remove="handleTabRemove"
            :animated="false">
            <TabPane v-for="(item,index) in items"  :name="item.id" :label="item.type=='method' ? item.title: (item.type + index)"
                     v-bind:key="item.id">
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

    export default {
        name: 'JServiceEditor',
        components: {
            JServiceItem,
            JMethodItem,
            JInstanceItem,
        },

        data () {
            return {
                items:[],
                selectNode:null,
            }
        },

        props:{
          allowMany: {
              type: Boolean,
              default: false
          },
        },
        mounted:function() {
            var self = this;
            //console.log(window.jm.utils.isBrowser('ie'));
            window.jm.vue.$on('servieNodeSelect',(nodes) => {
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

  .configItemTalbe {
      border-collapse: collapse;
      margin: 0 auto;
      text-align: left;
  }

  .configItemTalbe th {
      font-size: medium;
      font-family: "Microsoft Yahei", "微软雅黑", Tahoma, Arial, Helvetica, STHeiti;

  }



  .configItemTalbe td, table th {
      border: 1px solid #cad9ea;
      color: #666;
      height: 30px;
      max-width: 95px;
      max-height: 50px;
      overflow: hidden; /*超过区域就隐藏*/
      /*display: -webkit-box;*/ /*-webkit- 是浏览器前缀，兼容旧版浏览器的 即为display: box;*/
      -webkit-line-clamp: 2; /*限制在一个块元素显示的文本的行数*/
      -webkit-box-orient: vertical; /*box-orient 属性规定框的子元素应该被水平或垂直排列。horizontal：水平，vertical：垂直*/
      word-break: break-all; /*word-break 属性规定自动换行的处理方法 ，break-all：允许在单词内换行。*/
  }

  .configItemTalbe thead th {
      background-color: #CCE8EB;
      width: 100px;
  }

  .configItemTalbe tr:nth-child(odd) {
      background: #fff;
  }

  .configItemTalbe tr:nth-child(even) {
      background: #F5FAFA;
  }

  .opBar{
      width:60px;
      overflow: hidden;
  }

</style>
