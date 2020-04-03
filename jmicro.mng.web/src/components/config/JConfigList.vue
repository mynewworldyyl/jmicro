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

    export default {
        name: 'JConfigList',

        mounted(){
            let self = this;
            this.__getChildren(null,'/',function(data){
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
                var parseNode = function(parentNode) {

                    parentNode.title = parentNode.name;
                    parentNode.id = parentNode.path;

                    if(!parentNode.children || parentNode.children.length == 0) {
                        return
                    }
                    parentNode.leaf = [];

                    for(let i = 0; i < parentNode.children.length; i++) {
                        let n = parentNode.children[i];
                        if(n.children != null && n.children.length > 0) {
                            parseNode(n);
                        } else {
                            parentNode.leaf.push(n);
                        }
                    }

                    if(parentNode.leaf.length > 0) {
                        for(let i = 0; i < parentNode.leaf.length; i++ ) {
                            let idx = parentNode.children.indexOf(parentNode.leaf[i]);
                            parentNode.children.splice(idx,1);
                        }
                    }
                }
                window.jm.mng.conf.getChildren(path,true).then(function(nodes){
                    let ch = [];
                    if(parent != null ) {
                        ch = parent.children
                    }
                    for(let i = 0; i < nodes.length; i++) {
                        parseNode(nodes[i]);
                        ch.push(nodes[i]);
                    }
                    cb(ch);
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
                configs: [

                ]
            }
        },


    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JLeftView{
      height:auto;
      min-height: 500px;
      min-width:200px;
      max-width:300px;
      float: left;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
  }

  .JRouterList{
      height:auto;
      min-height: 500px;
      min-width:25%;
      max-width: 50%;
      float: left;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
      word-break: break-all;
  }

</style>
