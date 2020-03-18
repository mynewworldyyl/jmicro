<template>
  <div class="JLeftView">
      <Tree :data="configs" :load-data="loadChildren" @on-select-change="nodeSelect($event)"></Tree>
  </div>
</template>

<script>

    export default {
        name: 'j-left-view',

        created(){
            let self = this;
            this.__getChildren(null,'/',function(data){
                self.configs = data;
            });
        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit('leftViewNodeSelect',evt);
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
                window.jm.mng.getChildren(path,true).then(function(nodes){
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

</style>
