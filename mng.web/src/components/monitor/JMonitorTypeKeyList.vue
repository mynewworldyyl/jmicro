<template>
  <div class="JMonitorTypeKeyList">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">
                  <DropdownItem  name="refresh" :divided="true">Refresh</DropdownItem>
              </DropdownMenu>
          </Dropdown>
      </div>
      <div>
          <Tree :data="groups" ref="monitorTree"  @on-select-change="nodeSelect($event)"></Tree>
      </div>

  </div>
</template>

<script>

    import TreeNode from '../common/JTreeNode.js'
    import moType from "@/rpcservice/moType"
    
    const GROUP = 'monitorTye';

    const cid = 'JMonitorTypeKeyList';

    export default {
        name: 'JMonitorTypeKeyList',
        data () {
            return {
                groups :[],
                srcNodes:[],
            }
        },

        props:{
            evtName:{
                type:String,
                default:'monitorTypeKeySelect'
            },
            slId:{
                type:String,
                default:''
            }
        },

        mounted(){
            let self = this;
            this.$jr.auth.addActListener(()=>{
                self.isLogin = this.$jr.auth.isLogin();
                if( self.isLogin) {
                    self.loadMonitors();
                }
            });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);

        },

        methods:{

            nodeSelect(evt){
               this.$bus.$emit(this.evtName,evt);
            },

            loadMonitors() {
                let self = this;
                moType.getMonitorKeyList().then((resp)=>{
                    if(resp.code != 0 ) {
                        self.$Message.error(resp.msg);
                        return;
                    }
                    this.srcNodes = resp.data;
                    this.notifyChange();
                }).catch((err)=>{
                    window.console.log(err);
                    this.srcNodes=[];
                    this.groups = [];
                });
            }

            ,notifyChange() {

                let roots = [];

                for(let key in this.srcNodes) {
                    let val = this.srcNodes[key];
                    if(!key) {
                        continue;
                    }
                    let arr = key.split("##");
                    let r = new TreeNode(key,key,[],null,val,arr[1]);
                    r.group = GROUP;
                    roots.push(r);
                }

                this.groups = roots;
            }

            ,menuSelect(name){
                if('refresh' == name) {
                    this.loadMonitors();
                } else {
                    this.groupBy = name;
                    this.notifyChange();
                }
            }
        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JMonitorTypeKeyList{

  }

</style>
