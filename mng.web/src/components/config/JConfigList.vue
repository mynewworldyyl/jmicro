cn.jmicro.api.mng.IConfi<template>
  <div class="JLeftView">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">
                  <DropdownItem  name="refresh">Refresh</DropdownItem>
              </DropdownMenu>
          </Dropdown>
      </div>
      <Tree  v-if="isLogin && configs" :data="configs" :load-data="loadChildren" @on-select-change="nodeSelect($event)"></Tree>
      <span v-if="!isLogin">No permission</span>
      <span v-else-if="!configs">No data</span>
  </div>
</template>

<script>

    import TreeNode from  "../common/JTreeNode.js"

    import rpc from "@/rpc/rpcbase"
    import conf from "@/rpcservice/conf"
    import jmconfig from "@/rpcservice/jm"
    
    const GROUP = 'config';

    const cid = 'JConfigList';

    export default {
        name: 'JConfigList',

        mounted(){

            let self = this;
            rpc.addActListener(cid,()=>{
                self.isLogin = rpc.isLogin();
                if( self.isLogin) {
                    self.refresh();
                }
            });

            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

        },

        beforeDestroy(it) {
            if(GROUP != it.id) {
                return;
            }
            window.jm.vue.$off('editorClosed',this.editorRemove);
            rpc.removeActListener(cid);
        },

        methods:{

            refresh() {
                this.isLogin = rpc.isLogin();
                if(this.isLogin) {
                    let self = this;
                    this.__getChildren(null,jmconfig.ROOT,function(data){
                        self.configs = data;
                    });
                }
            },

            nodeSelect(evt){
               window.jm.vue.$emit('configNodeSelect',evt);
            },

            loadChildren(item,cb){
                this.__getChildren(item,item.path,cb);
            },

            __getChildren(parent,path,cb) {
                let parseNode = function(valNode) {

                    if(!valNode || !valNode.children || valNode.children.length == 0) {
                        return null;
                    }

                    let r = new TreeNode(valNode.path, valNode.name, [],null, valNode, valNode.name);
                    r.group = GROUP;
                    r.label = valNode.name;

                    let leaf = [];
                    for(let i = 0; i < valNode.children.length; i++) {
                        let n = valNode.children[i];
                        let c = parseNode(n);
                         if(c) {
                             c.parent = r;
                             r.addChild(c);
                             // valNode.children.splice(i,1);
                         } else {
                             leaf.push(n);
                         }
                    }
                    valNode.children = leaf;
                    return r;
                }

                conf.getChildren(path,true)
                    .then(function(resp){
                        if(!resp || resp.code != 0 || !resp.data) {
                            window.console.log(resp.msg);
                            cb(null);
                            return;
                        }

                    let nodes = resp.data;
                    let ch = [];
                    let valNode = { path: path, name:path, val:null,children:[]};
                    let root = new TreeNode(path, path, [], null, valNode, valNode.name);
                        root.group = GROUP;
                        root.label = valNode.name;
                        root.expand = true;

                    for(let i = 0; i < nodes.length; i++) {
                        if(!nodes[i]) {
                            continue;
                        }
                        let o = parseNode(nodes[i]);
                        if(o) {
                           root.addChild(o);
                        } else {
                            valNode.children.push(nodes[i]);
                        }
                    }

                    if(valNode.children.length > 0){
                        ch.push(root);
                    }
                    cb([root]);
                }).catch(function(err){
                    window.console.log(err);
                });
            }

            ,menuSelect(name){
                if(name == 'refresh') {
                    this.__getChildren(null,jmconfig.CONFIG_ROOT,(data)=>{
                        if(data) {
                            this.configs = data;
                        }else {
                            this.configs = [];
                        }
                    });
                }
            }
        },

        data () {
            return {
                configs: [],
                isLogin:false,
            }
        },


    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JLeftView{
      height:auto;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
  }

</style>
