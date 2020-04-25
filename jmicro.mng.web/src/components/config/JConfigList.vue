<template>
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
      <Tree :data="configs" :load-data="loadChildren" @on-select-change="nodeSelect($event)"></Tree>
  </div>
</template>

<script>

    import TreeNode from  "../common/JTreeNode.js"

    const GROUP = 'config';

    export default {
        name: 'JConfigList',

        mounted(){
            let self = this;
            this.__getChildren(null,window.jm.mng.CONFIG_ROOT,function(data){
                self.configs = data;
            });
        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit('configNodeSelect',evt);
            },

            loadChildren(item,cb){
                this.__getChildren(item,item.path,cb);
            },

            __getChildren(parent,path,cb) {
                let parseNode = function(valNode) {

                    if(!valNode.children || valNode.children.length == 0) {
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

                window.jm.mng.conf.getChildren(path,true)
                    .then(function(nodes){
                    let ch = [];
                    let valNode = { path: path, name:path, val:null,children:[]};
                    let root = new TreeNode(path, path, [], null, valNode, valNode.name);
                        root.group = GROUP;
                        root.label = valNode.name;
                        root.expand = true;

                    for(let i = 0; i < nodes.length; i++) {
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
                    this.__getChildren(null,window.jm.mng.CONFIG_ROOT,(data)=>{
                        this.configs = data;
                    });
                }
            }
        },

        data () {
            return {
                configs: []
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
