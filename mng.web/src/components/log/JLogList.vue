<template>
  <div class="JLogList">
      <table id="queryTable">
          <tr>
              <td>PARENT ID</td><td> <Input v-model="queryParams.reqParentId"/></td>
              <td>ACT</td>
              <td>
                  <Select :filterable="true"
                          :allowCreate="true" ref="actSelect" :label-in-value="true" v-model="queryParams.act">
                      <Option value="">none</Option>
                  </Select>
              </td>
          </tr>

          <tr>
              <td>LOG LEVEL</td>
              <td>
                  <Select :filterable="true" ref="levelSelect" :label-in-value="true" v-model="queryParams.level">
                    <Option value="" >none</Option>
                  </Select>
              </td>
              <td>TYPE</td>
              <td>
                  <Select :filterable="true"
                          :allowCreate="true" ref="typeSelect" :label-in-value="true" v-model="queryParams.type">
                      <Option value="" >none</Option>
                  </Select>
              </td>
          </tr>

          <tr>
              <td><i-button @click="loadLogFiles()">QUERY</i-button></td><td></td>
          </tr>

      </table>
      <div>
          <Tree :data="groups" ref="logFileTree"  @on-select-change="nodeSelect($event)"></Tree>
      </div>

  </div>
</template>

<script>

    import TreeNode from  "../common/JTreeNode.js"
    import agentLogSrv from "@/rpcservice/agentLogSrv"
    const GROUP = 'fileLog';

    export default {
        name: 'JLogList',

        data () {
            return {
                queryParams:{},
                groups :[],
                srcNodes:[],
            }
        },

        props:{

            slId:{
                type:String,
                default:''
            }
        },

        mounted(){
            //this.loadLogFiles();
        },

        methods:{

            nodeSelect(nodes){
                if(!nodes || nodes.length == 0) {
                    return;
                }

                let n = nodes[0];
                if(n.type == 'file') {
                    this.$bus.$emit('logFileSelect',nodes);
                }

            },

            loadLogFiles() {
                agentLogSrv.getAllLogFileEntry().then((resp)=>{
                    if(!resp || resp.code != 0 || resp.data.length == 0 ) {
                        this.srcNodes=[];
                        this.groups = [];
                    }else {
                        this.srcNodes = resp.data;
                        this.notifyChange();
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    this.srcNodes=[];
                    this.groups = [];
                });
            }

            ,notifyChange() {

                let agents = [];
                let agents2 = {};

                for(let i = 0; i < this.srcNodes.length; i++) {
                    let node = this.srcNodes[i];
                    if(!node) {
                        continue;
                    }

                    let agTree = agents2[node.agentId];

                    if(!agTree) {
                        agTree = new TreeNode(node.agentId, node.agentId, [], null, node, node.agentId);
                        agTree.group = GROUP;
                        agTree.type = 'agent';
                        agTree.val = [];
                        agents.push(agTree);
                        agents2[node.agentId] = agTree;
                    }

                    let iname = !node.instanceName || node.instanceName.length == 0 ? node.processId : (node.processId + ":" +node.instanceName);
                    let piTree = new TreeNode(node.processId,iname,[],null,node,iname);
                    piTree.group = GROUP;
                    piTree.type = 'process';
                    agTree.addChild(piTree);

                    if(node.logFileList) {
                        for(let j = 0; j < node.logFileList.length; j++) {
                            let filePath = node.logFileList[j];
                            let id = node.processId +":"+filePath;
                            let fiTree = new TreeNode(id, filePath,[],null,node,id);
                            fiTree.group = GROUP;
                            fiTree.type = 'file';
                            piTree.addChild(fiTree);
                        }
                    }
                }
                this.groups = agents;
            }

            ,menuSelect(name){
                if('refresh' == name) {
                    this.loadLogFiles();
                } else {
                    this.groupBy = name;
                    this.notifyChange();
                }
            }
        },

    }

</script>

<style scoped>
  .JLogList {
      height:auto;
      border-right:1px solid lightgray;
      border-top:1px solid lightgray;
      text-align: left;
  }

</style>
