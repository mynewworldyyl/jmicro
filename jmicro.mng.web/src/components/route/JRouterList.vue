<template>
  <div class="JRouterList">
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
      <div>
          <Tree :data="routers" ref="routers"  @on-select-change="nodeSelect($event)"></Tree>
      </div>

  </div>
</template>

<script>

    export default {
        name: 'JRouterList',
        data () {
            return {
                routers :[],
                srcRouters:[],
            }
        },

        props:{

        },

        created(){
            this.loadRouters((routerList)=>{
                this.routers = routerList;
            });


            window.jm.vue.$on('routerAdded',(r) => {
                if(!r) {
                    return;
                }
                this.srcRouters.push(r);
                let val = JSON.parse(r.val);
                this.parseRouterNode(this.srcRouters,val.type+'##'+val.group);
            });

        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit('routerNodeSelect',evt);
            },

            loadRouters(cb) {
                window.jm.mng.conf.getChildren(window.jm.mng.ROUTER_ROOT,true)
                    .then((nodes) => {
                        this.srcRouters = nodes;
                        let rs = this.parseRouterNode(nodes);
                        if(cb) {
                            cb(rs);
                        }
                }).catch((err) => {
                    window.console.log(err);
                    if(cb) {
                        cb([]);
                    }
                });
            }

            ,parseRouterNode(nodes,selectId){
                if(!nodes || nodes.length == 0) {
                    return [];
                }

                let type2TypeNode = {};

                let type2groups = {};

                let rs = [];

                for(let i = 0; i < nodes.length; i++) {
                    let n = nodes[i];
                    if(typeof n.val == 'string') {
                        n.val = JSON.parse(n.val);
                    }
                    let tn = type2TypeNode[n.val.type];
                    if(!tn) {
                        tn = new TreeNode(n.val.type,n.val.type,[],null,n);
                        if(n.val.type == selectId) {
                            tn.selected=true;
                        }
                        type2TypeNode[n.val.type] = tn;
                        tn.type='t';
                        rs.push(tn);
                    }

                    let grpId = n.val.type+'##'+n.val.group;
                    let grp = type2groups[grpId];
                    if(!grp) {
                        grp = new TreeNode(grpId, n.val.group,null,tn,[]);
                        type2groups[grpId] = grp;
                        grp.type='g';
                        tn.addChild(grp);
                        if(grpId == selectId) {
                            grp.selected = true;
                        }
                    }
                    grp.val.push(n);
                }
                this.routers = rs;
                return rs;
            }

            ,menuSelect(name){
                if(name == 'refresh') {
                    this.loadRouters();
                }
            }

        },
    }

    export class TreeNode{

        constructor(id='',title='',children=[],parent=null,val=null){
            this.id= id;
            this.title= title;
            this.children= children;
            this.val = val;
            this.parent = parent;
            this.type='';
        }

        addChild(node) {
            this.children.push(node);
        }

        removeChild(node) {
            let idx = this.indexOfChild(node);
            if(idx >=0) {
                this.children.splice(idx,1);
            }
        }

        indexOfChild(node) {
            if(!this.children || this.children.length == 0) {
                return -1;
            }
            for(let i = 0; i < this.children.length; i++) {
                if(this.children[i]==node) {
                    return i;
                }
            }
            return -1;
        }

    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JRouterList{
      height:auto;
      min-height: 500px;
      min-width:17%;
      max-width: 30%;
      float: left;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
      word-break: break-all;
  }



</style>
