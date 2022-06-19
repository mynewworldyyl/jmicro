<template>
  <div class="JUserProfileList">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">
                  <DropdownItem v-if="isLogin"  name="refresh">Refresh</DropdownItem>
				  <DropdownItem v-if="isLogin"  name="add">{{'Add' | i18n}}</DropdownItem>
              </DropdownMenu>
          </Dropdown>
      </div>
      <div>
          <Tree v-if="isLogin" :data="profiles" ref="routers"  @on-select-change="nodeSelect($event)"></Tree>
          <span v-if="!isLogin">Not login</span>
      </div>

		<Modal v-model="showAddModule" :loading="true" width="360" @on-cancel="showAddModule=false" 
			@on-ok="doAddModule()" ref="addMobule">
            <table>
				<tr v-if="isAdmin"><td>{{'ClientId'|i18n}}</td><td><input  type="input"  v-model="module.clientId"/></td></tr>
				<tr><td>{{'Module'|i18n}}</td><td><input  type="input"  v-model="module.module"/></td></tr>
            </table>
        </Modal>
		
  </div>
</template>

<script>

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
				isAdmin:this.$jr.auth.isAdmin(),
				module:{clientId: 0, module:''},
				showAddModule:false,
            }
        },

        mounted(){
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.accountListener);
            this.$bus.$on('tabItemRemove',this.editorRemove);
        },

        beforeDestroy() {
            this.$off('tabItemRemove',this.editorRemove);
            this.$jr.auth.removeActListener(GROUP);
        },

        methods:{
			
			async doAddModule(){
				
				if(!this.module.module) {
					this.$Message.error("失败模块名称不能为空")
				}
				
				if(!this.$jr.auth.isAdmin() || !this.module.clientId) {
					this.module.clientId = this.$jr.auth.actInfo.clientId
				}
				
				let rst = await profile.addModule(this.module.clientId, this.module.module)
				if(rst.code != 0) {
					 this.$Message.error(rst.msg || "失败")
				}else{
					 this.refresh()
					 this.showAddModule=false
					 this.$Message.info("成功")
				}
			},
			
            editorRemove(it) {
                if(GROUP != it.id) {
                    return;
                }
                this.$off('tabItemRemove',this.editorRemove);
                this.$jr.auth.removeActListener(cid);
            },

            nodeSelect(evt){
               this.$bus.$emit('userProfileSelect',evt);
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
                }else if(name=='add') {
					this.showAddModule = true
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
