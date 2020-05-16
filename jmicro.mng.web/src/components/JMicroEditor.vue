<template>
    <div class="JMainContentEditor">
        <div>
            <Tabs :value="!!selectNode ? selectNode.id:''" type="card" :closable="true" @on-tab-remove="handleTabRemove" :animated="false">
                <TabPane v-for="(item) in items"  :name="item.id" :label="item.label ? item.label : item.title"  v-bind:key="item.id">
                    <!-- RPC  config -->
                    <JServiceItem v-if="item.group == 'service' && item.type == 'sn'" :item="item"></JServiceItem>
                    <JInstanceItem v-else-if="item.group == 'service' && item.type == 'ins'" :item="item"></JInstanceItem>
                    <JMethodItem v-else-if="item.group == 'service' && item.type == 'method'" :meth="item"></JMethodItem>

                    <!--  RPC statis -->
                    <JStatisServiceItemView v-else-if="item.group == 'statis' && item.type == 'sn'" :serviceNode="item"></JStatisServiceItemView>
                    <JStatisServerItemView v-else-if="item.group == 'statis' && item.type == 'ins'" :serverNode="item"></JStatisServerItemView>
                    <JStatisSMethodItemView v-else-if="item.group == 'statis' && item.type == 'method'" :meth="item"></JStatisSMethodItemView>
                    <JStatisServiceItemView v-else-if="item.group == 'statis' && item.type == 'snv'" :serviceNode="item"></JStatisServiceItemView>

                    <!--  server monitor -->
                    <JMonitorEditor v-else-if="item.group == 'monitors'" :group="item"> </JMonitorEditor>

                    <!--  service router -->
                    <JRouterType v-else-if="item.group == 'router' && item.type == 't'" :selectItem="item"></JRouterType>
                    <JRouterGroup v-else-if="item.group == 'router' && item.type == 'g'" :selectItem="item"></JRouterGroup>

                    <!--  Config -->
                    <JConfigItem v-else-if="item.group == 'config'" :item="item"></JConfigItem>

                    <!-- Shell -->
                    <JShell v-else-if="item.group == 'shell'" :item="item"></JShell>

                    <JRepository v-else-if="item.group == 'repository'" :item="item"></JRepository>
                    <JHost v-else-if="item.group == 'host'" :item="item"></JHost>
                    <JDeploymentDesc v-else-if="item.group == 'deploymentDesc'" :item="item"></JDeploymentDesc>

                    <JAgent v-else-if="item.group == 'agent'" :item="item"></JAgent>
                    <JProcess v-else-if="item.group == 'process'" :item="item"></JProcess>


                </TabPane>
            </Tabs>
        </div>

    </div>
</template>

<script>
    //import jm from '../../public/js/jm.js'
    import JServiceItem from './service/JServiceItem.vue'
    import JMethodItem from './service/JSMethodItem.vue'
    import JInstanceItem from './service/JInstanceItem.vue'

    import JStatisServiceItemView from './statis/JStatisServiceItemView.vue'
    import JStatisSMethodItemView from './statis/JStatisSMethodItemView.vue'
    import JStatisServerItemView from './statis/JStatisServerItemView.vue'

    import JMonitorEditor from "./monitor/JMonitorEditor.vue"

    import JRouterType from './route/JRouterType.vue'
    import JRouterGroup from './route/JRouterGroup.vue'

    import JConfigItem from './config/JConfigItem.vue'

    import JShell from './shell/JShell.vue'

    import JRepository from './deployment/JRepository.vue'
    import JHost from './deployment/JHost.vue'
    import JDeploymentDesc from './deployment/JDeploymentDesc.vue'
    import JAgent from './deployment/JAgent.vue'
    import JProcess from './deployment/JProcess.vue'

    import TreeNode from "./common/JTreeNode.js"

    export default {
        name: 'JMicroEditor',
        components: {
            JServiceItem,
            JMethodItem,
            JInstanceItem,

            JStatisServiceItemView,
            JStatisSMethodItemView,
            JStatisServerItemView,

            JMonitorEditor,

            JRouterGroup,
            JRouterType,

            JConfigItem,
            JHost,
            JRepository,
            JDeploymentDesc,
            JShell,
            JAgent,
            JProcess,

        },

        data () {
            let dataId = 'JMicroEditorDataId';
            let d = window.jm.mng.cache[dataId];
            if( !d) {
                d = window.jm.mng.cache[dataId] = {
                    items:[],
                    selectNode:null,
                    allowMany:true,
                };
            }
            return d;
        },

        mounted : function() {
            //let self = this;
            this.mountServiceSelect();
            this.mountStatisSelect();
            this.mountConfigSelect();
            this.mountMonitorsSelect();
            this.mountRouterSelect();

            this.mountShellSelect();

        },

        methods: {

            mountMonitorsSelect(){
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                window.jm.vue.$on('monitorNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    let is = self.items;
                    let it = null;

                    if(self.allowMany ) {
                        for(let i = 0; i < self.items.length; i++) {
                            if(self.items[i].id == node.id) {
                                it = self.items[i];
                                break;
                            }
                        }

                        if(!it) {
                            is.push(node);
                            self.items = is;
                            self.selectNode = node;
                        } else {
                            self.selectNode = it;
                        }
                    } else {
                        is[0] = node;
                        self.items = is;
                        self.selectNode = node;
                    }
                });
            },

            mountRouterSelect(){
                let self = this;
                window.jm.vue.$on('routerNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    let idx = null;

                    for(let i = 0; i < self.items.length; i++) {
                        if(node.group == 'router' && self.items[i].group == node.group) {
                            idx = i;
                            break;
                        }
                    }

                    if(idx == null) {
                        self.items.push(node);
                    } else {
                        self.items[idx] = node
                    }
                    self.selectNode = node;
                });
            },

            mountStatisSelect(){
                let self = this;
                window.jm.vue.$on('statisNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id ) {
                        return;
                    }

                    let is = self.items;
                    let it = null;

                    if(self.allowMany ) {
                        for(let i = 0; i < self.items.length; i++) {
                            if(self.items[i].id == node.id ) {
                                it = self.items[i];
                                break;
                            }
                        }

                        if(!it) {
                            is.push(node);
                            self.items = is;
                            self.selectNode = node;
                        } else {
                            self.selectNode = it;
                        }
                    } else {
                        is[0] = node;
                        self.items = is;
                        self.selectNode = node;
                    }
                });
            },

            mountConfigSelect(){
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                window.jm.vue.$on('configNodeSelect',function(nodes) {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];
                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    let is = self.items;
                    let idx = null;

                    for(let i = 0; i < self.items.length; i++) {
                        if(node.group == 'config' && self.items[i].group == node.group) {
                            idx = i;
                            break;
                        }
                    }

                    if(idx == null) {
                        is.push(node);
                        self.items = is;
                        self.selectNode = node;
                    } else {
                        self.items[idx] = node
                        self.selectNode = node;
                    }
                });
            },

            mountServiceSelect() {
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                window.jm.vue.$on('serviceNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    let is = self.items;
                    let it = null;

                    for(let i = 0; i < self.items.length; i++) {
                        if(self.items[i].id == node.id ) {
                            it = self.items[i];
                            break;
                        }
                    }

                    if(!it) {
                        is.push(node);
                        self.items = is;
                        self.selectNode = node;
                    } else {
                        self.selectNode = it;
                    }

                });
            },

            handleTabRemove (id) {
                let i = -1;
                for(let idx = 0;  idx < this.items.length; idx++ ) {
                    if(id == this.items[idx].id) {
                        i = idx;
                        break;
                    }
                }
                if(i > -1) {
                    //let it = this.items[i];
                    this.items.splice(i,1);

                    if(this.items.length > 0) {
                        if(i == 0) {
                            this.selectNode = this.items[0];
                        }else {
                            this.selectNode = this.items[i-1];
                        }
                    }else {
                        this.selectNode = null;
                    }
                }
            },

            mountShellSelect(){
                let self = this;
                window.jm.vue.$on('openEditorSelect',function(editorId) {

                    if(!!self.selectNode && self.selectNode.group == editorId) {
                        return;
                    }

                    let title = editorId;
                    let it = null;

                    for(let i = 0; i < self.items.length; i++) {
                        if(self.items[i].group == title ) {
                            it = self.items[i];
                            break;
                        }
                    }

                    if(it) {
                        self.selectNode = it;
                    } else {
                        let r = new TreeNode(title,title,null,null,null, title);
                        r.type = title;
                        r.group = title;
                        self.items.push(r);
                        self.selectNode = r;
                    }
                });
            },

        }
    }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
    .JMainContentEditor{
        height:auto;
        /* min-height: 500px;*/
        overflow:hidden;
    }

</style>
