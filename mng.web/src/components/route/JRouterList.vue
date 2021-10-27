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

          <Tree v-if="isLogin" :data="routers" ref="routers"  @on-select-change="nodeSelect($event)"></Tree>
          <span v-if="!isLogin">No permission</span>
      </div>

  </div>
</template>

<script>

    import rpc from "@/rpc/rpcbase"
    import conf from "@/rpcservice/conf"
    import cons from "@/rpcservice/jm"

    import TreeNode from "../common/JTreeNode.js"

    const GROUP = 'router';

    const cid = 'JRouterList';

    export default {
        name: 'JRouterList',
        data () {
            return {
                routers :[],
                srcRouters:[],
                isLogin:false,
            }
        },

        props:{

        },

        created() {
            window.jm.vue.$on('routerAdded',(r) => {
                if(!r) {
                    return;
                }
                this.srcRouters.push(r);
                let val = JSON.parse(r.val);
                this.parseRouterNode(this.srcRouters,val.type+'##'+val.group);
            });
        },

        mounted(){

            let self = this;
            self.isLogin = rpc.isLogin();
            rpc.addActListener(cid,()=>{
                self.isLogin = rpc.isLogin();
                if( self.isLogin) {
                    self.refresh();
                }
            });

            window.jm.vue.$on('editorClosed',self.editorRemove);

        },

        beforeDestroy() {
            window.jm.vue.$off('tabItemRemove',this.editorRemove);
            rpc.removeActListener(GROUP);
        },

        methods:{

            editorRemove(it) {
                if(GROUP != it.id) {
                    return;
                }
                window.jm.vue.$off('tabItemRemove',this.editorRemove);
                rpc.removeActListener(cid);
            },


            nodeSelect(evt){
               window.jm.vue.$emit('routerNodeSelect',evt);
            },

            refresh() {
                this.isLogin = rpc.isLogin();
                if(this.isLogin) {
                    let self = this;
                    this.loadRouters((routerList)=>{
                        self.routers = routerList;
                    });
                }
            },

            loadRouters(cb) {
                conf.getChildren(cons.ROUTER_ROOT,true)
                    .then((res) => {
                        let nodes = res.data
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
                        tn.group = GROUP;
                        if(n.val.type == selectId) {
                            tn.selected = true;
                        }
                        type2TypeNode[n.val.type] = tn;
                        tn.type='t';
                        rs.push(tn);
                    }

                    let grpId = n.val.type+'##'+n.val.group;
                    let grp = type2groups[grpId];
                    if(!grp) {
                        grp = new TreeNode(grpId, n.val.group,null,tn,[]);
                        grp.group = GROUP;
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

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JRouterList{
      height:auto;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
      word-break: break-all;
  }



</style>
