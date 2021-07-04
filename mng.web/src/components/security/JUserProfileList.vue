<template>
  <div class="JUserProfileList">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">
                  <DropdownItem v-if="isLogin"  name="refresh">Refresh</DropdownItem>
              </DropdownMenu>
          </Dropdown>
      </div>
      <div>
          <Tree v-if="isLogin" :data="profiles" ref="routers"  @on-select-change="nodeSelect($event)"></Tree>
          <span v-if="!isLogin">Not login</span>
      </div>

  </div>
</template>

<script>

    import rpc from "@/rpc/rpcbase"
    import profile from "@/rpcservice/profile"
    import TreeNode from "../common/JTreeNode.js"
    import {Constants} from "@/rpc/message"

    const GROUP = 'userProfile';

    const cid= 'userProfileList';

    export default {
        name: 'JUserProfileList',
        data () {
            return {
                profiles :[],
                srcProfiles:[],
                adminPer:false,
                isLogin:false,
            }
        },

        mounted(){
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            rpc.addActListener(cid,this.accountListener);
            window.jm.vue.$on('tabItemRemove',this.editorRemove);
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
               window.jm.vue.$emit('userProfileSelect',evt);
            },

            accountListener(type) {
                if(Constants.LOGIN == type) {
                    this.isLogin = true;
                    this.refresh();
                }else {
                    this.isLogin = false;
                    this.profiles = [];
                    this.srcProfiles = [];
                }
            },

            refresh() {
                let self = this;
                if(this.isLogin) {
                    this.loadProfiles((routerList)=>{
                        self.routers = routerList;
                    });
                }else {
                    this.$Message.info("Not login")
                }
            },

            loadProfiles(cb) {
                let self = this;
                profile.getModuleList()
                    .then((resp) => {
                        if(resp.code == 0) {
                            self.srcProfiles = resp.data;
                            let rs = self.parseProfileNode(resp.data);
                            if(cb) {
                                cb(rs);
                            }
                        }else {
                            self.$Message.info(resp.msg);
                        }
                }).catch((err) => {
                    window.console.log(err);
                    if(cb) {
                        cb([]);
                    }
                });
            }

            ,parseProfileNode(nodes){
                if(!nodes || nodes.length == 0) {
                    return [];
                }

                let rs = [];

                for(let i = 0; i < nodes.length; i++) {
                    let n = nodes[i];
                    let tr = new TreeNode(n, n, null,null,null,n);
                    tr.group = GROUP;
                    tr.type = GROUP;
                    rs.push(tr);
                }
                this.profiles = rs;
                return rs;
            }

            ,menuSelect(name){
                if(name == 'refresh') {
                    this.refresh();
                }
            }

        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JUserProfileList{
      height:auto;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
      word-break: break-all;
  }

</style>
