<template>
  <div class="JThreadPoolMonitorList">
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
          <Tree :data="serverList" ref="serverListRef"  @on-select-change="nodeSelect($event)"></Tree>
      </div>

  </div>
</template>

<script>

    import TreeNode from "../common/JTreeNode.js"
    import rpc from "@/rpc/rpcbase"
    import threadPoolSrv from "@/rpcservice/threadPoolSrv"
    const GROUP = 'threadPool';

    const cid = 'JThreadPoolMonitorList';

    export default {
        name: 'JThreadPoolMonitorList',
        data () {
            return {
                serverList :[],
                srcServerList:[],
            }
        },

        props:{

        },

        created() {

        },

        mounted(){
            let self = this;
            rpc.addActListener(cid,()=>{
                self.isLogin = rpc.isLogin();
                if( self.isLogin) {
                    self.loadServerList();
                }
            });

            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });


            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit('threadPoolSelect',evt);
            },

            loadServerList() {
                let self = this;
                threadPoolSrv.serverList('all')
                    .then((resp) => {
                    if(resp.code == 0) {
                        self.srcServerList = resp.data;
                        self.parseServerListNode();
                    }
                }).catch((err) => {
                    window.console.log(err);
                });
            }

            ,parseServerListNode(){
                if(!this.srcServerList || this.srcServerList.length == 0) {
                    return [];
                }

                let in2ServerList = {};

                let rs = [];

                let nodes = this.srcServerList;

                for(let i = 0; i < nodes.length; i++) {
                    let n = nodes[i];
                    let tn = in2ServerList[n.instanceName]
                    if(!tn) {
                        tn = in2ServerList[n.instanceName] = new TreeNode(n.instanceName, n.instanceName, [], null, n.instanceName);
                        tn.group = GROUP;
                        tn.type = 'ins';
                        rs.push(tn);
                    }
                    let t = n.ec.threadNamePrefix;
                    let s = new TreeNode(n.key, t,null,tn,n,t);
                    s.group = GROUP;
                    s.type = 'mo';
                    tn.addChild(s);

                }
                this.serverList = rs;
                return rs;
            }

            ,menuSelect(name){
                if(name == 'refresh') {
                    this.loadServerList();
                }
            }

        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JThreadPoolMonitorList{
      height:auto;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
      word-break: break-all;
  }



</style>
