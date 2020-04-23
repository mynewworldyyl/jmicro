<template>
  <div class="JMainContentEditor">
    <div>
                <table class="configItemTalbe" width="99%">
                    <thead><tr><td>KEY</td><td>VALUE</td><td>OPERATION</td></tr></thead>

                    <tr>
                        <td></td><td></td><td> <a @click="addNode()">ADD</a></td>
                    </tr>

                    <tr v-for="c in item.children" :key="c.id">
                        <td>{{c.val.name}}</td><td>{{c.val.val}}</td>
                        <td>
                            <a @click="updateNode(c,true)">MODIFY</a>
                        </td>
                    </tr>

                    <tr v-for="c in item.val.children" :key="c.id">
                        <td>{{c.name}}</td><td>{{c.val}}</td>
                        <td>
                            <a @click="updateNode(c,false)">MODIFY</a>&nbsp;&nbsp;
                            <a @click="deleteNode(c)">DELETE</a>
                        </td>
                    </tr>

                </table>
    </div>

      <Modal v-model="addNodeDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="onAddOk()">
          <table>
              <tr><td>NAME</td><td><input type="input" id="nodeName" v-model="inputName"/></td></tr>
              <tr><td>VALUE</td><td><input type="input" id="nodeValue" v-model="inputVal" /></td></tr>
              <tr><td>DIRECTORY</td><td><input type="checkbox" id="idDir" v-model="isDir"/></td></tr>
              <tr><td colspan="2" style="color:red">{{errMsg}}</td></tr>
          </table>
      </Modal>

      <Modal v-model="updateNodeDialog" width="360" @on-ok="onUpdateOk()">
          <table>
              <tr><td>NAME</td><td><input type="input" disabled  v-model="inputName"/></td></tr>
              <tr><td>VALUE</td><td><input type="input"  v-model="inputVal"/></td></tr>
          </table>
      </Modal>

  </div>
</template>

<script>
    //import jm from '../../public/js/jm.js'

    import TreeNode from  "../common/JTreeNode.js"

    export default {
        name: 'JConfigItem',
        props:{
          item: { type: Object,required: true },
        },

        data () {
            return {

                addNodeDialog:false,
                deleteNodeDialog:false,
                updateNodeDialog:false,

                curNode:null,

                isDir:false,
                inputName:'',
                inputVal:'',
                errMsg:''
            }
        },
        methods: {
            addNode() {
                this.addNodeDialog = true;
            },

            onAddOk() {
                let self = this;
                this.$refs.addNodeDialog.buttonLoading = false;
                if(!this.isDir && !this.inputVal) {
                    self.errMsg='Value cannot be null!';
                    return false;
                }

                if(this.isDir && !this.inputVal) {
                    this.inputVal = 'host';
                }

                let path = self.item.val.path + '/' + self.inputName;
                window.jm.mng.conf.add(path,self.inputVal,self.isDir)
                    .then(function(result){
                        if(result) {
                            let val = {name:self.inputName, val:self.inputVal, path:path};
                            if(self.isDir) {
                                let r = new TreeNode(val.path, val.name, [],self.item, val, val.name);
                                r.group = 'config';
                                self.item.addChild(r);
                            }else {
                                //r.children = null;
                                self.item.val.children.push(val);
                            }

                            self.$Message.success('Successfully add');

                            self.addNodeDialog = false;
                            self.errMsg='';
                            self.inputName = '';
                            self.inputVal = '';
                            self.isDir = false;

                        }else {
                            self.$Message.error('fail');
                        }
                    }).catch(function(err){
                    self.$Message.error('fail:'+err);
                    self.inputName = '';
                    self.inputVal = '';
                    self.isDir = false;

                });
            },

            updateNode(node,isDir) {
                let self = this;
                self.isDir = isDir;

                self.curNode = node;

                if(isDir) {
                    self.inputName = node.val.name;
                    self.inputVal = node.val.val;
                }else {
                    self.inputName = node.name;
                    self.inputVal = node.val;
                }
                self.updateNodeDialog = true;
            },

            onUpdateOk() {
                let self = this;

                let valNode = null;
                if(this.isDir) {
                    valNode = self.curNode.val;
                } else  {
                    valNode = self.curNode;
                }

                if(valNode.val == self.inputVal) {
                    return;
                }

                window.jm.mng.conf.update(valNode.path, self.inputVal)
                    .then(function(result){
                        if(result) {
                            valNode.val = self.inputVal;
                            self.$Message.success('Successfully add');
                        }else {
                            self.$Message.error('fail');
                        }
                        self.inputName = '';
                        self.inputVal = '';
                        self.curNode = null;
                    }).catch(function(err){
                    self.$Message.error('fail:'+err);
                    self.inputName = '';
                    self.inputVal = '';
                    self.curNode = null;
                });

                self.addNodeDialog = false;
            },

            deleteNode(delNode) {
                let self = this;
                let idx = -1;
                for(let i = 0; i < self.item.val.children.length; i++) {
                    let l = self.item.val.children[i];
                    if(delNode.path == l.path) {
                        idx = i;
                        break;
                    }
                }

                if(idx == -1) {
                    return;
                }
                window.jm.mng.conf.delete(delNode.path)
                    .then(function(result){
                        if(result) {
                            self.item.val.children.splice(idx,1);
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
