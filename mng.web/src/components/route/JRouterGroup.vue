<template>
  <div class="JRouterGroup">
      <div style="height:35px;margin-left:5px">
          <Button @click="createNode">Create</Button>
      </div>
      <table class="configItemTalbe" width="99%">
          <!--<caption style="text-align: left;padding-bottom: 3px;">{{item.id}}</caption>-->
          <thead><tr><td>TYPE</td><td>SRC</td><td>TARGET</td><td>STATUS</td><td>PRIORITY</td><td>OP</td></tr></thead>
          <tr v-for="(c,index) in item.val" :key="c.path">
              <td>{{c.val.type}}</td><td>{{c.val.from}}</td><td>{{c.val.to}}</td>
              <td>{{c.val.enable}}</td><td>{{c.val.priority}}</td>
              <td><a @click="modifyNode(c)">Modify</a>&nbsp; &nbsp; <a @click="deleteNode(c,index)">Delete</a></td>
          </tr>
      </table>

      <Modal title="Create Router" :draggable="true" v-model="createRouterDialog" width="360"
             @on-ok="onCreateOk()" @on-cancel="onCreateCancel()" ref="createRouterDialog">
          <table>
              <tr><td>Type</td><td>
                  <Select id="createRouterType" v-model="createRouter.type">
                      <!--<Option value="">Default</Option>-->
                      <Option value="ipRouter">IP Router</Option>
                      <Option value="tagRouter">Tag Router</Option>
                      <Option value="serviceRouter">Service Router</Option>
                  </Select></td>
              </tr>

              <tr><td>From</td><td></td></tr>

              <tr v-if="createRouter.type=='ipRouter' || !createRouter.type"><td style="text-align: end;">IP And Port</td><td><input   type="input"
                                                                                                                                       v-model="createRouter.from.ipPort"/></td></tr>
              <tr v-if="createRouter.type=='tagRouter' || !createRouter.type"><td  style="text-align: end;">Tag Key</td><td><input  type="input"
                                                                                                                                    v-model="createRouter.from.tagKey"/></td></tr>
              <tr v-if="createRouter.type=='tagRouter' || !createRouter.type"><td style="text-align: end;">Tag Value</td><td><input   type="input"
                                                                                                                                      v-model="createRouter.from.tagVal"/></td></tr>
              <tr v-if="createRouter.type=='serviceRouter' || !createRouter.type"><td style="text-align: end;">Service Name</td><td><input   type="input"
                                                                                                                                             v-model="createRouter.from.serviceName"/></td></tr>
              <tr v-if="createRouter.type=='serviceRouter' || !createRouter.type"><td style="text-align: end;">Namespace</td><td><input   type="input"
                                                                                                                                          v-model="createRouter.from.namespace"/></td></tr>
              <tr v-if="createRouter.type=='serviceRouter' || !createRouter.type"><td style="text-align: end;">Version</td><td><input   type="input"
                                                                                                                                        v-model="createRouter.from.version"/></td></tr>
              <tr v-if="createRouter.type=='serviceRouter' || !createRouter.type"><td style="text-align: end;">Method</td><td><input   type="input"
                                                                                                                                       v-model="createRouter.from.method"/></td></tr>

              <tr><td>Target</td><td></td></tr>
              <tr><td style="text-align: end;">IP And Port</td><td><input type="input"  v-model="createRouter.to.ipPort"/></td></tr>

             <!-- <tr><td>ID</td><td><input type="input"   v-model="createRouter.uniqueId"/></td></tr>-->
              <tr><td>Enable</td><td><input type="checkbox"   v-model="createRouter.enable"/></td></tr>
              <tr><td>Group</td><td><input type="input"   v-model="createRouter.group"/></td></tr>
              <tr><td>Priority</td><td><input type="input"   v-model="createRouter.priority"/></td></tr>
              <p>{{errorMsg}}</p>
          </table>
      </Modal>
  </div>
</template>

<script>

    import conf from "@/rpcservice/conf"
    import cons from "@/rpcservice/jm"

    export default {
        name: 'JRouterGroup',
        components: {
        },

        data () {
            return {
                item:this.selectItem,
                errorMsg:'',
                createRouter:{
                    type:'',
                    from:{},
                    to:{},
                },
                createRouterDialog:false,
                doUpdate:false,
            }
        },

        props:{
            selectItem: {
              type: Object,
              default: null
          },
        },

        mounted:function() {

        },

        methods: {
            modifyNode(node){
                this.doUpdate = true;
                this.createRouter = node.val;
                this.createRouterDialog = true;
                this.errorMsg = '';
            }

            ,deleteNode(node,idx){
                let self = this;
                let path = cons.ROUTER_ROOT + '/' + node.val.uniqueId;
                conf.delete(path)
                    .then((result) => {
                        self.item.val.splice(idx,1);
                        self.$Message.success('successfully'+result);
                    }).catch((err)=>{
                    self.$Message.fail('fail:'+err);
                });
            }

            ,createNode(){
                this.createRouter = {
                    type:'',
                    from:{},
                    to:{},
                    enable:false,
                };
                this.errorMsg = '';
                this.doUpdate = false;
                this.createRouter.group = this.item.title;
                this.createRouter.priority = 1;
                this.createRouter.type = 'ipRouter';
                this.createRouterDialog = true;

            }

            ,validate() {
                if(!this.createRouter.to.ipPort) {
                    this.errorMsg = 'target IP and Port cannot be NULL';
                    return false;
                }

                if(this.createRouter.type == 'ipRouter') {
                    if(!this.createRouter.from.ipPort) {
                        this.errorMsg = 'Source IP and Port cannot be NULL';
                        return false;
                    }
                }

                if(this.createRouter.type == 'tagRouter') {
                    if(!this.createRouter.from.tagKey) {
                        this.errorMsg = 'Source tag key cannot be NULL';
                        return false;
                    }

                    if(!this.createRouter.from.tagVal) {
                        this.errorMsg = 'Source tag val cannot be NULL';
                        return false;
                    }
                }

                if(this.createRouter.type == 'serviceRouter') {
                    if(!this.createRouter.from.serviceName) {
                        this.errorMsg = 'Source service name cannot be NULL';
                        return false;
                    }

                    if(!this.createRouter.from.namespace) {
                        this.errorMsg = 'Source namespace cannot be NULL';
                        return false;
                    }

                    if(!this.createRouter.from.version) {
                        this.errorMsg = 'Source service version cannot be NULL';
                        return false;
                    }

                    if(!this.createRouter.from.method) {
                        this.errorMsg = 'Source service method cannot be NULL';
                        return false;
                    }
                }

                return true;
            }

            ,onCreateOk(){
                this.$refs.createRouterDialog.buttonLoading = false;
                if(!this.validate()) {
                    return;
                }

                this.errorMsg ='';
                let self = this;

                if(this.doUpdate) {
                    //更新路由
                    let path = cons.ROUTER_ROOT + '/' + self.createRouter.uniqueId;
                    let val = JSON.stringify(self.createRouter);
                    conf.update(path,val)
                        .then((result) => {
                            if(result) {
                                self.createRouterDialog = false;
                                self.$Message.success('successfully');
                            }else {
                                self.$Message.fail('fail');
                            }
                        }).catch((err)=>{
                        self.$Message.fail('fail:'+err);

                    });
                } else {
                    //创建新路由
                    rpc.getId(cons.RULE_ID).then((id)=>{
                        let path = cons.ROUTER_ROOT + '/' + id;
                        self.createRouter.uniqueId = id;
                        let val = JSON.stringify(self.createRouter);
                        conf.add(path,val,false)
                            .then((res) => {
                                let result = res.data
                                if(result) {
                                    self.createRouterDialog = false;
                                    self.$Message.success('successfully');
                                    let n = { path:path,val:val,name:id };
                                    let is = self.item;
                                    n.val = JSON.parse(n.val);
                                    is.val.push(n);
                                    self.item = is;
                                }else {
                                    self.$Message.fail('fail');
                                }
                            }).catch((err)=>{
                            self.$Message.fail('fail:'+err);

                        });
                    }).catch((err)=>{
                        self.$Message.fail('fail:'+err);
                    })
                }
            }

            ,onCreateCancel(){
                this.createRouter.type="";

            }
        }
    }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JRouterGroup{
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
