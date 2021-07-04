<template>
  <div class="JNamedTypeList">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">
                  <DropdownItem  name="refresh" :divided="true">REFRESH</DropdownItem>
                  <DropdownItem  name="add" :divided="true">ADD</DropdownItem>
              </DropdownMenu>
          </Dropdown>
      </div>
      <div>
          <Tree :data="groups" ref="namedTypeTree"  @on-select-change="nodeSelect($event)"></Tree>
      </div>

      <Modal v-model="addNameDialog" :loading="true" ref="addNameDialog" width="360" @on-ok="onAddOk()">
          <div>
              <div style="color:red">{{errMsg}}</div>
              <Label for="name">NAME</Label>
              <Input id="name" v-model="newName"/>
          </div>
      </Modal>

  </div>
</template>

<script>

    import moType from "@/rpcservice/moType"
    import rpc from "@/rpc/rpcbase"
    
    import TreeNode from '../common/JTreeNode.js'

    const GROUP = 'namedType';

    const cid = 'JNameTypeList';

    export default {
        name: 'JNameTypeList',
        data () {
            return {
                groups :[],
                srcNodes:[],
                newName:null,
                addNameDialog:false,
                errMsg:'',
            }
        },

        props:{
            evtName:{
                type:String,
                default:'namedTypeSelect'
            },
            slId:{
                type:String,
                default:''
            }
        },

        mounted(){

            let self = this;
            rpc.addActListener(cid,()=>{
                self.isLogin = rpc.isLogin();
                if( self.isLogin) {
                    self.loadNamedTypeList();
                }
            });

            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);
        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit(this.evtName,evt);
            },

            onAddOk(){
                let self = this;
                if(!this.newName) {
                    this.errMsg = 'Name cannot be null';
                    return;
                }
                this.errMsg = "";
                moType.addNamedTypes(this.newName).then((resp)=>{
                    if(resp.code != 0 ) {
                        self.$Message.error(resp.msg);
                        return;
                    }
                    self.addNameDialog = false;
                    self.loadNamedTypeList();
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            loadNamedTypeList() {
                let self = this;
                moType.getNamedList().then((resp)=>{
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
            },

            notifyChange() {

                let roots = [];

                for(let i = 0; i < this.srcNodes.length; i++) {
                    let n = this.srcNodes[i];
                    let r = new TreeNode(n,n,[],null,n);
                    r.group = GROUP;
                    roots.push(r);
                }
                this.groups = roots;
            }

            ,menuSelect(name){
                if('refresh' == name) {
                    this.loadNamedTypeList();
                } else if('add' == name) {
                    this.addNameDialog = true;
                } else {
                    this.notifyChange();
                }
            }
        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JNamedTypeList{

  }

</style>
