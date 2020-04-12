<template>
  <div class="JServiceList">
      <div class="toolBar">
          <Dropdown @on-click="menuSelect">
              <a href="javascript:void(0)">
                  <Icon type="ios-arrow-down"></Icon>
              </a>
              <DropdownMenu slot="list">
                  <DropdownItem :selected="groupBy=='sn'" name="sn">Service</DropdownItem>
                  <DropdownItem :selected="groupBy=='ins'" name="ins">Instance</DropdownItem>
                  <DropdownItem :selected="groupBy=='snv'" name="snv">SNV</DropdownItem>
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

    //服务，服务实例，服务方法分组
    const GROUP_SN = 'sn';
    //服务实例，服务，服务方法分组
    const GROUP_INS = 'ins';
    //服务实例方法结点
    const GROUP_METHOD='method';
    //服务名称，名称空间，版本分组服务方法
    const GROUP_SNV = 'snv';

    export default {
        name: 'JServiceList',
        data () {
            return {
                services :[],
                groupBy : GROUP_SN,
                srcNodes:[],
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
            }
        },

        created(){
            this.loadServices((srvTrees)=>{
                this.services = srvTrees;
            });
        },

        methods:{

            nodeSelect(evt){
               window.jm.vue.$emit(this.evtName,evt);
            },

            loadServices(cb) {

                window.jm.mng.srv.getServices().then((nodes)=>{
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
                                r = new TreeNode(n.title,n.title,[],null,n.val);
                                layer02Layer1map[n.title] = r;
                                r.type = GROUP_SN;
                                roots.push(r);
                            }

                            let t = n.val.key.instanceName+'##'+n.val.key.host+'##'+n.val.key.port;
                            let layer1Key = n.title+'##'+t;
                            let l2 = layer12Layer2map[layer1Key];  //title = serviceName
                            if(!l2) {
                                l2 = new TreeNode(layer1Key,t,[],r,n.val.key);
                                layer12Layer2map[layer1Key]= l2;
                                l2.type = GROUP_INS;
                                r.addChild(l2);
                            }

                            if(n.children != null && n.children.length > 0) {
                                n.children.forEach((e)=>{
                                    e.parent = l2;
                                    e.type = GROUP_METHOD;
                                    l2.addChild(e);
                                });
                            }
                        } else if(this.groupBy == GROUP_INS){
                            let insKey = n.val.key.instanceName;
                            let r = layer02Layer1map[insKey];  //title = serviceName
                            if(!r) {
                                let title = insKey + '##'+n.val.key.host+'##'+n.val.key.port;
                                r = new TreeNode(insKey,title,[],null,n.val);
                                layer02Layer1map[insKey]=r;
                                r.type = GROUP_INS;
                                roots.push(r);
                            }

                            let layer1Key = n.title + '##' + r.title;
                            let l2 = layer12Layer2map[layer1Key];  //title = serviceName
                            if(!l2) {
                                l2 = new TreeNode(layer1Key,n.title,[],r,n.val);
                                layer12Layer2map[layer1Key] = l2;
                                l2.type = GROUP_SN;
                                r.addChild(l2);
                            }

                            if(n.children != null && n.children.length > 0) {
                                n.children.forEach((e)=>{
                                    e.parent = l2;
                                    e.type = GROUP_METHOD;
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

                let title = node.key.serviceName+'##'+node.key.namespace+'##'+node.key.version;
                let id = title+'##'+node.key.instanceName+'##'+node.key.host+'##'+node.key.port;

                let tr = new TreeNode(id,title,[],null,node);

                if(!!node.methods && node.methods.length > 0){
                    node.methods.forEach((e)=>{
                        if(e!=null && e.key.method != 'wayd') {
                            let mid = id+'##'+e.key.method+'##' + e.key.paramsStr;
                            let tm = new TreeNode(mid, e.key.method,null,tr,e);
                            tm.type = GROUP_METHOD;
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
                }else {
                    this.groupBy = name;
                    this.notifyChange();
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
  .JServiceList{

  }

</style>
