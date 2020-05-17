<template>
  <div class="JMonitorList">
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

    const GROUP = 'monitors';

    export default {
        name: 'JMonitorList',
        data () {
            return {
                groups :[],
                srcNodes:[],
            }
        },

        props:{
            evtName:{
                type:String,
                default:'monitorNodeSelect'
            },
            slId:{
                type:String,
                default:''
            }
        },

        mounted(){
            this.loadMonitors();
        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit(this.evtName,evt);
            },

            loadMonitors() {
                window.jm.mng.monitor.serverList().then((nodes)=>{
                    if(!nodes || nodes.length == 0 ) {
                        this.srcNodes=[];
                        this.groups = [];
                    }else {
                        this.srcNodes = nodes;
                        this.notifyChange();
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    this.srcNodes=[];
                    this.groups = [];
                });
            }

            ,notifyChange() {

                let roots = [];
                let key2nodes = {};

                for(let i = 0; i < this.srcNodes.length; i++) {
                    let node = this.srcNodes[i];
                    if(!node) {
                        continue;
                    }
                    let grp = node.group;

                    let r = key2nodes[grp];
                    if(!r) {
                        r = new TreeNode(grp,grp,[],null,null);
                        r.group = GROUP;
                        r.val = [];
                        roots.push(r);
                        key2nodes[grp] = r;
                    }
                    r.val.push(node);
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
  .JMonitorList{

  }

</style>
