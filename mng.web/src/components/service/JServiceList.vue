<template>
  <div class="JServiceList">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">

                  <DropdownItem v-for="m in menus" :key="m" :selected="groupBy==m" :name="m">{{m}}</DropdownItem>
                 <!-- <DropdownItem :selected="groupBy=='ins'" name="ins">Instance</DropdownItem>
                  <DropdownItem :selected="groupBy=='snv'" name="snv">SNV</DropdownItem>-->
                  <DropdownItem  name="all" :divided="false">All</DropdownItem>
                  <DropdownItem  name="refresh" :divided="true">Refresh</DropdownItem>
              </DropdownMenu>
          </Dropdown>
      </div>
      <div>
          <Tree :data="services" ref="servicesTree"  @on-select-change="nodeSelect($event)"></Tree>
      </div>

  </div>
</template>

<script>

    import srv from "@/rpcservice/srv"
    import TreeNode from "../common/JTreeNode.js"
    import rpc from "@/rpc/rpcbase"
    //服务，服务实例，服务方法分组
    const GROUP_SN = 'sn';
    //服务实例，服务，服务方法分组
    const GROUP_INS = 'ins';
    //服务实例方法结点
    const GROUP_METHOD='method';
    //服务名称，名称空间，版本分组服务方法
    const GROUP_SNV = 'snv';

    //const cid = 'JServiceList';

    export default {
        name: 'JServiceList',
        data () {
            let menus = this.menuStr.split(",");
            return {
                services : [],
                srcNodes : [],
                menus : menus,
                showAll:false,
            }
        },

        props:{
            evtName:{
                type:String,
                default:'serviceNodeSelect'
            },

            slId:{
                type:String,
                default:''
            },

            group:{
                type:String,
                default:'service'
            },

            menuStr:{
                type : String,
                default:GROUP_SN  +  ',' + GROUP_INS + ',' + GROUP_SNV
            },

            groupBy:{
                type : String,
                default:GROUP_SN
            }

        },

        mounted(){
            let self = this;
            rpc.addActListener(this.slId,()=>{
                self.isLogin = rpc.isLogin();
                if( self.isLogin) {
                    self.loadServices();
                }else {
                    self.srcNodes =[];
                    self.services = [];
                }
            });

            let ec = function() {
                rpc.removeActListener(self.slId);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit(this.evtName,evt);
            },

            loadServices(cb) {
                srv.getServices(this.showAll)
                    .then((res)=>{
                    let nodes = res.data
                    if(!nodes || nodes.length == 0 ) {
                        if(cb) {
                            cb([]);
                        }
                    }
                    this.srcNodes = nodes;
                    this.notifyChange(cb);
                }).catch((err)=>{
                    window.console.log(err);
                    if(cb) {
                        cb([]);
                    }
                });
            }

            ,notifyChange(cb) {

                let layer02Layer1map = {}; //第0层到第1层影射
                let layer12Layer2map = {};//第1层到第2层影射

                let roots = [];
                this.srcNodes.forEach((e)=>{
                    let n = this.parseSrvNode(e);
                    if(n!= null) {
                        if(this.groupBy == GROUP_SN) {
                            let r = layer02Layer1map[n.title];  //title = serviceName
                            if(!r) {
                                r = new TreeNode(this.group + ':'+ n.title,n.title,[],null,n.val, this.group + ':'+ n.title);
                                layer02Layer1map[n.title] = r;
                                r.type = GROUP_SN;
                                r.group = this.group;
                                roots.push(r);
                            }

                            let t = n.val.key.instanceName+'##'+n.val.key.host+'##'+n.val.key.port;
                            let layer1Key = n.title+'##'+t;
                            let l2 = layer12Layer2map[layer1Key];  //title = serviceName
                            if(!l2) {
                                l2 = new TreeNode(this.group + ':'+layer1Key,t,[],r,n.val.key, this.group+':' + n.val.key.instanceName);
                                layer12Layer2map[layer1Key]= l2;
                                l2.type = GROUP_INS;
                                l2.group = this.group;
                                r.addChild(l2);
                            }

                            if(n.children != null && n.children.length > 0) {
                                n.children.forEach((e)=>{
                                    e.parent = l2;
                                    e.type = GROUP_METHOD;
                                    e.group = this.group;
                                    l2.addChild(e);
                                });
                            }
                        } else if(this.groupBy == GROUP_INS){
                            let insKey = n.val.key.instanceName;
                            let r = layer02Layer1map[insKey];  //title = serviceName
                            if(!r) {
                                let title = insKey + '##'+n.val.key.host+'##'+n.val.key.port;
                                r = new TreeNode(this.group + ':'+insKey,title,[],null,n.val, this.group + ':' + n.val.key.instanceName);
                                layer02Layer1map[insKey]=r;
                                r.type = GROUP_INS;
                                r.group = this.group;
                                roots.push(r);
                            }

                            let layer1Key = n.title + '##' + r.title;
                            let l2 = layer12Layer2map[layer1Key];  //title = serviceName
                            if(!l2) {
                                l2 = new TreeNode(this.group + ':'+layer1Key,n.title,[],r,n.val, this.group + ':' + n.val.key.serviceName);
                                layer12Layer2map[layer1Key] = l2;
                                l2.type = GROUP_SN;
                                l2.group = this.group;
                                r.addChild(l2);
                            }

                            if(n.children != null && n.children.length > 0) {
                                n.children.forEach((e)=>{
                                    e.parent = l2;
                                    e.type = GROUP_METHOD;
                                    e.group = this.group;
                                    l2.addChild(e);
                                });
                            }
                        } else if(this.groupBy == GROUP_SNV) {
                            let insKey = n.title;
                            if(!layer02Layer1map[insKey]) {
                                n.type = GROUP_SNV;
                                n.id = n.title;
                                if(n.children != null && n.children.length > 0) {
                                    n.children.forEach((e)=>{
                                        e.parent = n;
                                        e.type = GROUP_METHOD;
                                        e.group = this.group;
                                        //n.addChild(e);
                                    });
                                }
                                roots.push(n);
                            }
                        }
                    }
                });

                if(cb) {
                    cb(roots);
                }else {
                    this.services = roots;
                }
            }

            ,parseSrvNode(node){
                if(!node) {
                    return null;
                }

                let title = node.key.serviceName + '##' + node.key.namespace + '##' + node.key.version;
                let mFullKey = title + '##' + node.key.instanceName + '##' + node.key.host + '##' + node.key.port;
                let id = this.group + ':' + mFullKey;

                let mKeyPrefix = title + '######';

                let tr = new TreeNode(id,title,[],null,node);
                tr.group = this.group;
                if(!!node.methods && node.methods.length > 0){
                    node.methods.forEach((e)=>{
                        if(e!=null && e.key.method != 'wayd') {
                            let mid = id + '##' + e.key.method + '##' + e.key.paramsStr;
                            let tm = new TreeNode(mid, e.key.method, null, tr, e, this.group + ':' + e.key.method);
                            tm.type = GROUP_METHOD;
                            tm.group = this.group;
                            tm.mkey = mKeyPrefix + '##' + e.key.method + '##' + e.key.paramsStr;
                            tr.addChild(tm);
                        }
                    })
                }

                return tr;
            }

            ,menuSelect(name){
                if('refresh' == name) {
                    this.loadServices((srvTrees)=>{
                        this.services = srvTrees;
                    });
                }else if('all' == name) {
                    this.showAll =! this.showAll;
                    this.loadServices((srvTrees)=>{
                        this.services = srvTrees;
                    });
                }else {
                    this.groupBy = name;
                    this.notifyChange();
                }
            }
        },
    }

</script>


<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JServiceList{

  }

</style>
