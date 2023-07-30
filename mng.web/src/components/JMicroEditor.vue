<template>
    <div class="JMainContentEditor">
            <Tabs :value="!!selectNode ? selectNode.id:''" type="card" :closable="true" :animated="false"
                  @on-tab-remove="handleTabRemove" @on-click="handleTabActive">
                <TabPane v-for="(item) in items"  :name="item.id" v-bind:key="item.id"
                         :label="(item.label ? item.label :item.title) | i18n ">
                    <!-- RPC  config -->
                    <div class="editorBody" :style="{height:($jcontentHeight-32)+'px'}">
                        <JServiceItem v-if="item.group == 'service' && item.type == 'sn'" :item="item"></JServiceItem>
                        <JInstanceItem v-else-if="item.group == 'service' && item.type == 'ins'" :item="item"></JInstanceItem>
                        <JMethodItem v-else-if="item.group == 'service' && item.type == 'method'" :meth="item"></JMethodItem>

                        <!--  RPC statis -->
                        <JStatisServiceItemView v-else-if="item.group == 'statis' && item.type == 'sn'" :serviceNode="item"></JStatisServiceItemView>
                        <JStatisServerItemView v-else-if="item.group == 'statis' && item.type == 'ins'" :serverNode="item"></JStatisServerItemView>
                        <JStatisSMethodItemView v-else-if="item.group == 'statis' && item.type == 'method'" :meth="item"></JStatisSMethodItemView>
                        <JStatisServiceItemView v-else-if="item.group == 'statis' && item.type == 'snv'" :serviceNode="item"></JStatisServiceItemView>
                        <JResourceMonitorView v-else-if="item.group == 'resourceMonitorView'" :item="item"></JResourceMonitorView>

                        <!--  server monitor -->
                        <JMonitorEditor v-else-if="item.group == 'monitors'" :group="item"> </JMonitorEditor>

						<JScheduleJobConfig v-else-if="item.group == 'scheduleJobConfig'" :group="item"> </JScheduleJobConfig>

                        <!--  service router -->
                        <!--<JRouterType v-else-if="item.group == 'router' && item.type == 't'" :selectItem="item"></JRouterType>
                        <JRouterGroup v-else-if="item.group == 'router' && item.type == 'g'" :selectItem="item"></JRouterGroup>
                        -->
                        <JRouteRuleEditor v-else-if="item.group == 'routeRuleEditor'" :selectItem="item"></JRouteRuleEditor>

                        <!--  Config -->
                        <JConfigItem v-else-if="item.group == 'config'" :item="item"></JConfigItem>
                        <JI18nConfig v-else-if="item.group == 'i18nConfig'" :item="item"></JI18nConfig>

                        <!-- Shell -->
                        <JShell v-else-if="item.group == 'shell'" :item="item"></JShell>
                        <JTesting v-else-if="item.group == 'testing'" :item="item"></JTesting>
                        <JAbout v-else-if="item.group == 'about'" :item="item"></JAbout>
                        <JLog v-else-if="item.group == 'fileLog'" :item="item"></JLog>
                        <JTopicListView v-else-if="item.group == 'topicList'" :item="item"></JTopicListView>

                        <JTestingPubsub v-else-if="item.group == 'testingPubsub'" :item="item"></JTestingPubsub>
                        <JPubsubItemView v-else-if="item.group == 'pubsubItem'" :item="item"></JPubsubItemView>
                        <JPubsubStatisView v-else-if="item.group == 'pubsubStatis'" :item="item"></JPubsubStatisView>

                        <JAccountEditor v-else-if="item.group == 'account'" :item="item"></JAccountEditor>
                        <JRoleEditor v-else-if="item.group == 'role'" :item="item"></JRoleEditor>
                        <JClientConfig v-else-if="item.group == 'clientConfig'" :item="item"></JClientConfig>
                        <JUserProfileEditor v-else-if="item.group == 'userProfile'" :item="item"></JUserProfileEditor>
						<floweditor v-else-if="item.group == 'flow'" :item="item"></floweditor>
						<JApproveInfoEditor v-else-if="item.group == 'flowApproveInfo'" :item="item"></JApproveInfoEditor>

                        <JRepository v-else-if="item.group == 'repository'" :item="item"></JRepository>
                        <JHost v-else-if="item.group == 'host'" :item="item"></JHost>
                        <JDeploymentDesc v-else-if="item.group == 'deploymentDesc'" :item="item"></JDeploymentDesc>

                        <JAgent v-else-if="item.group == 'agent'" :item="item"></JAgent>
                        <JProcess v-else-if="item.group == 'process'" :item="item"></JProcess>

                        <JTypeConfig v-else-if="item.group == 'typeConfig'" :item="item"></JTypeConfig>
						<JDataType v-else-if="item.group == 'jdataType'" :item="item"></JDataType>
                        <JMonitorTypeKeyEditor v-else-if="item.group == 'monitorTye'" :item="item"></JMonitorTypeKeyEditor>
                        <JMonitorTypeServiceMethodEditor v-else-if="item.group == 'mtsm'" :item="item"></JMonitorTypeServiceMethodEditor>
                        <JInvokeLinkView v-else-if="item.group == 'invokeLinkView'" :item="item"></JInvokeLinkView>
                        <JLogItemView v-else-if="item.group == 'logItemView'" :item="item"></JLogItemView>
                        <JPublicKeyList v-else-if="item.group == 'publicKeyList'" :item="item"></JPublicKeyList>
                        <JNamedTypeEditor v-else-if="item.group == 'namedType'" :item="item"></JNamedTypeEditor>
                        <JThreadPoolMonitorEditor v-else-if="item.group == 'threadPool'" :item="item"></JThreadPoolMonitorEditor>
                        <JLogWarningConfigView v-else-if="item.group == 'warningConfig'" :item="item"></JLogWarningConfigView>
                        <JStatisConfigView v-else-if="item.group == 'statisConfig'" :item="item"></JStatisConfigView>
                        <JResourceConfigView v-else-if="item.group == 'resourceConfig'" :item="item"></JResourceConfigView>
                        <JPublicKeyList v-else-if="item.group == 'publicKeyList'" :item="item"></JPublicKeyList>
                        <JServiceMethodList v-else-if="item.group == 'serviceMethodList'" :item="item"></JServiceMethodList>
						<JPermissionApproveList v-else-if="item.group == 'permissionApproveList'" :item="item"></JPermissionApproveList>
						<JRoleApproveList v-else-if="item.group == 'roleApproveList'" :item="item"></JRoleApproveList>
						
						<JInterfaceDefEditor v-else-if="item.group == 'interfaceDef'" :item="item"></JInterfaceDefEditor>
						<InterfaceParamList v-else-if="item.group == 'interfaceParamList'" :item="item"></InterfaceParamList>
                        <InterfaceUsedHistory v-else-if="item.group == 'interfaceUsedHistory'" :item="item"></InterfaceUsedHistory>
						<DayCostList v-else-if="item.group == 'dayCostList'" :item="item"></DayCostList>
						<FeeOrderList v-else-if="item.group == 'feeOrderList'" :item="item"></FeeOrderList>
						<ActInterfaceList v-else-if="item.group == 'actInterfaceList'" :item="item"></ActInterfaceList>
						
						<JDeviceFunctionEditor v-else-if="item.group == 'deviceFunctionDef'" :item="item"></JDeviceFunctionEditor>
						<JDeviceProductList v-else-if="item.group == 'DeviceProductList'" :item="item"></JDeviceProductList>
						<JDeviceList v-else-if="item.group == 'JDeviceList'" :item="item"></JDeviceList>
                    </div>
                </TabPane>
            </Tabs>

    </div>
</template>

<script>

    import TreeNode from "./common/JTreeNode.js"
   
    import config from "@/rpc/config"

    //import JServiceItem from './service/JServiceItem.vue'
    /*import JMethodItem from './service/JSMethodItem.vue'
    import JInstanceItem from './service/JInstanceItem.vue'

    import JStatisServiceItemView from './statis/JStatisServiceItemView.vue'
    import JStatisSMethodItemView from './statis/JStatisSMethodItemView.vue'
    import JStatisServerItemView from './statis/JStatisServerItemView.vue'

    import JMonitorEditor from "./monitor/JMonitorEditor.vue"

    import JRouterType from './route/JRouterType.vue'
    import JRouterGroup from './route/JRouterGroup.vue'

    import JConfigItem from './config/JConfigItem.vue'

    import JShell from './shell/JShell.vue'
    import JTesting from './shell/JTesting.vue'

    import JRepository from './deployment/JRepository.vue'
    import JHost from './deployment/JHost.vue'
    import JDeploymentDesc from './deployment/JDeploymentDesc.vue'
    import JAgent from './deployment/JAgent.vue'
    import JProcess from './deployment/JProcess.vue'


    import JTypeConfig from "./monitor/JTypeConfig.vue"
    import JInvokeLinkView from "./monitor/JInvokeLinkView.vue"
    import JMonitorTypeKeyEditor from "./monitor/JMonitorTypeKeyEditor.vue"
    import JMonitorTypeServiceMethodEditor from "./monitor/JMonitorTypeServiceMethodEditor.vue"
    import JLogItemView from "./monitor/JLogItemView.vue"
    import JNamedTypeEditor from "./monitor/JNamedTypeEditor.vue"
    import JThreadPoolMonitorEditor from "./monitor/JThreadPoolMonitorEditor.vue"*/

    export default {
        name: 'JMicroEditor',
        components: {
            JServiceItem : () => import('./service/JServiceItem.vue'),
            JMethodItem: () => import('./service/JSMethodItem.vue'),
            JInstanceItem : () => import('./service/JInstanceItem.vue'),

            JStatisServiceItemView : () => import('./statis/JStatisServiceItemView.vue'),
            JStatisSMethodItemView : () => import('./statis/JStatisSMethodItemView.vue'),
            JStatisServerItemView : () => import('./statis/JStatisServerItemView.vue'),

            JMonitorEditor : () => import('./monitor/JMonitorEditor.vue'),
			JScheduleJobConfig : ()=>import('./monitor/JScheduleJobConfig.vue'),
			
           /* JRouterGroup : () => import('./route/JRouterType.vue'),
            JRouterType : () => import('./route/JRouterGroup.vue'),*/

            JConfigItem : () => import('./config/JConfigItem.vue'),
            JHost : () => import('./deployment/JHost.vue'),
            JRepository : () => import('./deployment/JRepository.vue'),
            JDeploymentDesc : () => import('./deployment/JDeploymentDesc.vue'),
            JShell : () => import('./shell/JShell.vue'),
            JAbout : () => import('./shell/JAbout.vue'),
            JTesting : () => import('./shell/JTesting.vue'),
            JTopicListView:() => import('./shell/JTopicListView.vue'),
            JAgent : () => import('./deployment/JAgent.vue'),
            JProcess : () => import('./deployment/JProcess.vue'),
            JLog : () => import('./log/JLog.vue'),
            JLogWarningConfigView: () => import('./monitor/JLogWarningConfigView.vue'),
            JStatisConfigView: () => import('./monitor/JStatisConfigView.vue'),
            JResourceConfigView: () => import('./monitor/JResourceConfigView.vue'),
            JResourceMonitorView: () => import('./monitor/JResourceMonitorView.vue'),

            JAccountEditor:()=> import('./security/JAccountEditor.vue'),
            JRoleEditor:()=> import('./security/JRoleEditor.vue'),
            JClientConfig:()=> import('./security/JClientConfig.vue'),
            JUserProfileEditor:()=> import('./security/JUserProfileEditor.vue'),
            JPublicKeyList:()=> import('./security/JPublicKeyList.vue'),
            JServiceMethodList:()=> import('./security/JServiceMethodList.vue'),
			JPermissionApproveList:()=> import('./security/JPermissionApproveList.vue'),
			JRoleApproveList:()=>import('./security/JRoleApproveList.vue'),
			
            JTypeConfig : () => import('./monitor/JTypeConfig.vue'),
            JI18nConfig : () => import('./i18n/JI18nConfig.vue'),

            JTestingPubsub : () => import('./pubsub/JTestingPubsub.vue'),
            JPubsubItemView : () => import('./pubsub/JPubsubItemView.vue'),
            JPubsubStatisView: ()=> import('./pubsub/JPubsubStatisView.vue'),

            JMonitorTypeKeyEditor : () => import('./monitor/JMonitorTypeKeyEditor.vue'),
            JMonitorTypeServiceMethodEditor : () => import('./monitor/JMonitorTypeServiceMethodEditor.vue'),
            JInvokeLinkView : () => import('./monitor/JInvokeLinkView.vue'),
            JLogItemView : () => import('./monitor/JLogItemView.vue'),
            JNamedTypeEditor : () => import('./monitor/JNamedTypeEditor.vue'),
            JThreadPoolMonitorEditor : () => import('./monitor/JThreadPoolMonitorEditor.vue'),
            JRouteRuleEditor : () => import('./route/JRouteRuleEditor.vue'),
			floweditor : () => import('./flow/floweditor.vue'),
			JApproveInfoEditor : () => import('./flow/JApproveInfoEditor.vue'),
			JInterfaceDefEditor : () => import('./ds/JInterfaceDefEditor.vue'),
			InterfaceParamList : () => import('./ds/InterfaceParamList.vue'),
			InterfaceUsedHistory : () => import('./ds/InterfaceUsedHistory.vue'),
			DayCostList : () => import('./ds/DayCostList.vue'),
			FeeOrderList : () => import('./ds/FeeOrderList.vue'),
			ActInterfaceList : () => import('./ds/ActInterfaceList.vue'),
			JDataType : () => import('./ds/JDataType.vue'),
			
			JDeviceFunctionEditor:()=>import('./iot/JDeviceFunctionDefEditor.vue'),
			JDeviceProductList:()=>import('./iot/DeviceProductList.vue'),
			JDeviceList:()=>import('./iot/JDeviceList.vue'),
        },

        data () {
            let dataId = 'JMicroEditorDataId';
            let d = config.cache[dataId];
            if( !d) {
                d = config.cache[dataId] = {
                    items:[],
                    selectNode:null,
                    allowMany:true,
                    //tabTitle:'',
                };
            }
            return d;
        },

        mounted : function() {
            let self = this;
            this.mountServiceSelect('serviceNodeSelect');
            this.mountStatisSelect();

            this.mountConfigSelect('configNodeSelect','config');
            this.mountConfigSelect('userProfileSelect','userProfile');

            this.mountMonitorsSelect();
            this.mountRouterSelect();

            this.mountShellSelect();

            this.mountMonitorTypeKeySelect('monitorTypeKeySelect');

            this.mountServiceSelect('monitorTypeServiceMethodSelect');

            this.mountServiceSelect('namedTypeSelect');

            this.mountServiceSelect('threadPoolSelect');

            this.mountServiceSelect('logFileSelect');

            //this.$bus.$emit('openEditorSelect','topicList');

            this.$bus.$on("scroptto",(to) => {
                this.$nextTick(() => {
                    let c = self.$el.querySelector(".editorBody");
                    //c.scrollTop = to
                    c.scrollTo(0,to);
                });
            });

            window.addEventListener("scroll",(/*e*/)=>{
                //console.log(e);
                /*let c = document.querySelector(".editorBody");
                console.log('editorBody: ' + c.scrollTop);*/
               /* self.$nextTick(() => {
                    let c = self.$el.querySelector(".editorBody");
                    c.scrollTop = c.scrollHeight
                });*/
            },true);

        },

        updated() {
           /* this.$nextTick(() => {
                let c = this.$el.querySelector(".editorBody");
                c.scrollTop = c.scrollHeight
            });*/
        },

        methods: {

            mountMonitorTypeKeySelect(evt){
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                this.$bus.$on(evt,(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    let is = self.items;
                    let it = null;

                   /* if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/
                    self.handleTabOpen(node.id);
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

            mountMonitorsSelect(){
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                this.$bus.$on('monitorNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                   /* if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/
                    self.handleTabOpen(node.id);
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
                this.$bus.$on('routerNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                   /* if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/
                    self.handleTabOpen(node.id);
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
                this.$bus.$on('statisNodeSelect',(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id ) {
                        return;
                    }

                    let is = self.items;
                    let it = null;

                  /*  if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/
                    self.handleTabOpen(node.id);
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

            mountConfigSelect(evtName,groupName){
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                this.$bus.$on(evtName,function(nodes) {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];
                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    /*if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/

                    self.handleTabOpen(node.id);

                    let is = self.items;
                    let idx = null;

                    for(let i = 0; i < self.items.length; i++) {
                        if(node.group == groupName && self.items[i].group == node.group) {
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

            mountServiceSelect(evt) {
                let self = this;
                //console.log(window.jm.utils.isBrowser('ie'));
                this.$bus.$on(evt,(nodes) => {
                    if(!nodes || nodes.length ==0) {
                        return;
                    }

                    let node = nodes[0];

                    if(!!self.selectNode && self.selectNode.id == node.id) {
                        return;
                    }

                    let is = self.items;
                    let it = null;

                    /*if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/
                    self.handleTabOpen(node.id);
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

            handleTabActive(id) {
                this.$bus.$emit('editorActive',id);
            },

            handleTabDeactive(id) {
                this.$bus.$emit('editorDeactive',id);
            },

            handleTabOpen(id) {
                this.$bus.$emit("editorOpen",
                    {
                        "editorId":id,
                        "menus":[]
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
                    let it = this.items[i];

                    if(i>0) {
                        this.handleTabActive(this.items[i-1].id);
                    }else if(this.items.length > 1) {
                        this.handleTabActive(this.items[i+1].id);
                    }

                    this.$jr.auth.removeActListener(it.id);
                    this.$bus.$emit('tabItemRemove',it.id);
                    this.$bus.$emit('editorClosed',it.id);

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
                this.$bus.$on('openEditorSelect',function(editorId) {

                    if(!!self.selectNode && self.selectNode.group == editorId) {
                        return;
                    }

                    let title = editorId;
                    let it = null;

                    /*if(self.selectNode) {
                        self.handleTabDeactive(self.selectNode.id);
                    }*/

                    self.handleTabOpen(title);
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
        overflow:hidden;
    }

    .editorBody {
      overflow-y: auto;
	  overflow-x: hidden;
    }

    .editorToolBar{
        height:30px;
        width:100%;
        position:absolute;
        left: 0px;
        top:91px;
        right:0px;
        border-bottom: 1px solid lightgray;
    }

</style>
