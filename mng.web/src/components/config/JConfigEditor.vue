<template>
  <div class="JMainContentEditor">
    <div>
      <Tabs :value="!!selectNode ? selectNode.path:''" type="card" :closable="allowMany" @on-tab-remove="handleTabRemove()"
            :animated="false">
            <TabPane v-for="item in items"  :name="item.id" :label="item.label" v-bind:key="item.id">
                {{selectNode.path}}
                <table class="configItemTalbe" width="99%">
                    <caption style="text-align: left;padding-bottom: 3px;">{{selectNode.id}}</caption>
                    <thead><tr><td>KEY</td><td>VALUE</td><td>OPERATION</td></tr></thead>

                    <tr>
                        <td></td><td></td><td> <a @click="addNode()">ADD</a></td>
                    </tr>

                    <tr v-for="c in item.children" :key="c.path">
                        <td>{{c.name}}</td><td>{{c.val}}</td>
                        <td>
                            <a @click="updateNode(c.path)">MODIFY</a>
                            <a v-if="!item.children" @click="deleteNode(c.path)">DELETE</a>
                        </td>
                    </tr>

                   <!-- <tr v-for="c in item.leaf" :key="c.path">
                        <td width="20%">{{c.name}}</td><td width="70%">{{c.val}}</td><td width="8%" class="opBar">
                        <a @click="updateNode(c.path)">MODIFY</a> &nbsp; <a @click="deleteNode(c.path)">DELETE</a></td>
                    </tr>-->

                </table>
            </TabPane>
        </Tabs>
    </div>

      <Modal v-model="addNodeDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="onAddOk()">
          <table>
              <tr><td>NAME</td><td><input type="input" id="nodeName" v-model="inputName"/></td></tr>
              <tr><td>VALUE</td><td><input type="input" id="nodeValue" v-model="inputVal" :disabled="isDir" /></td></tr>
              <tr><td>DIRECTORY</td><td><input type="checkbox" id="idDir" v-model="isDir"/></td></tr>
              <tr><td colspan="2" style="color:red">{{errMsg}}</td></tr>
          </table>
      </Modal>

      <Modal v-model="deleteNodeDialog" width="360" @on-ok="onUpdateOk()">
          <table>
              <tr><td>NAME</td><td><input type="input" disabled  v-model="inputName"/></td></tr>
              <tr><td>VALUE</td><td><input type="input"  v-model="inputVal"/></td></tr>
          </table>
      </Modal>



  </div>
</template>

<script>
    //import jm from '../../public/js/jm.js'

    import conf from "@/rpcservice/conf"

    export default {
        name: 'JConfigEditor',
        props:{
          allowMany: {
              type: Boolean,
              default: false
          },
        },
        mounted:function() {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            var self = this;
            //console.log(window.jm.utils.isBrowser('ie'));
            this.$bus.$on('configNodeSelect',function(nodes) {
                if(!nodes || nodes.length ==0) {
                    return;
                }

                let node = nodes[0];

                if(!!self.selectNode && self.selectNode.path == node.path) {
                    return;
                }

                let is = self.items;
                let it = null;

                if(self.allowMany ) {
                    for(let i = 0; i < self.items.length; i++) {
                        if(self.items[i].path == node.path) {
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

        data () {
            return {
                items:[],
                selectName:'',
                selectNode:null,
                addNodeDialog:false,
                deleteNodeDialog:false,
                updateNodeDialog:false,

                isDir:false,
                inputName:'',
                inputVal:'',
                errMsg:''
            }
        },
        methods: {
            handleTabRemove (evt,name) {
                this['tab' + name] = false;
            },

            addNode() {
               if(!this.selectNode) {
                   return;
               }
               let self = this;
               self.addNodeDialog = true;
            },

            onAddOk() {
                let self = this;
                this.$refs.addNodeDialog.buttonLoading = false;
                if(!this.isDir && !this.inputVal) {
                    self.errMsg='Value cannot be null!';
                    return false;
                }
                conf.add(self.selectNode.path+'/'+self.inputName,self.inputVal,self.isDir)
                    .then(function(res){
                        let result = res.data
                        if(result) {
                            let p = self.selectNode.path+'/'+self.inputName;
                            let newNode = {id:p,name:self.inputName, val:self.inputVal,path:p};
                            self.selectNode.leaf.push(newNode);
                            self.items[0] = self.selectNode;
                            self.$Message.success('Successfully add');

                            self.addNodeDialog = false;
                            self.errMsg='';
                            self.inputName = '';
                            self.inputVal = '';
                            self.isDir = false;

                        }else {
                            self.$Message.fail('fail');
                        }
                    }).catch(function(err){
                    self.$Message.fail('fail:'+err);
                    self.inputName = '';
                    self.inputVal = '';
                    self.isDir = false;

                });
            },

            updateNode(path) {
                let self = this;

                let lf = null;
                for(let i = 0; i < self.selectNode.leaf.length; i++) {
                    let l = self.selectNode.leaf[i];
                    if(path == l.path) {
                        lf = l;
                        break;
                    }
                }

                if(lf == null) {
                    return;
                }

                self.inputName = lf.name;
                self.inputVal = lf.val;

                self.deleteNodeDialog = true;
            },

            onUpdateOk() {
                let self = this;

                let lf = null;
                for(let i = 0; i < self.selectNode.leaf.length; i++) {
                    let l = self.selectNode.leaf[i];
                    if(self.inputName == l.name) {
                        lf = l;
                        break;
                    }
                }

                if(lf == null) {
                    return;
                }

                conf.update(self.selectNode.path+'/'+self.inputName,self.inputVal)
                    .then(function(res){
                        let result = res.data
                        if(result) {
                            lf.val = self.inputVal;
                            self.items[0] = self.selectNode;
                            self.$Message.success('Successfully add');
                        }else {
                            self.$Message.error('fail');
                        }
                        self.inputName = '';
                        self.inputVal = '';
                    }).catch(function(err){
                    self.$Message.error('fail:'+err);
                    self.inputName = '';
                    self.inputVal = '';
                });

                self.addNodeDialog = false;
            },

            deleteNode(path) {
                let lf = null;
                let self = this;
                let idx = -1;
                for(let i = 0; i < self.selectNode.leaf.length; i++) {
                    let l = self.selectNode.leaf[i];
                    if(path == l.path) {
                        lf = l;
                        idx = i;
                        break;
                    }
                }

                if(lf == null) {
                    return;
                }
                conf.delete(lf.path)
                    .then(function(res){
                        let result = res.data
                        if(result) {
                            self.selectNode.leaf.splice(idx,1);
                            self.items[0] = self.selectNode;
                            self.$Message.success('Successfully add');
                        }else {
                            self.$Message.fail('fail');
                        }
                        self.inputName = '';
                        self.inputVal = '';
                    }).catch(function(err){
                        self.$Message.error('fail:'+err);
                        self.inputName = '';
                        self.inputVal = '';
                });
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
