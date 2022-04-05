<template>

	<div class="container" :style="{height:$jcontainerHeight+'px'}">
		<div class="menu-bar">
			<Menu mode="horizontal" theme="light" active-key="service" @on-select="toRouter">
				<Submenu name="srv">
					<template slot="title">
						<Icon type="ios-cog" />{{"Monitor" | i18n }}
					</template>
					<Menu-group :title="'Monitor'|i18n">
						<Menu-item name="__service">
							<Icon type="ios-paper"></Icon>{{"Service"|i18n}}
						</Menu-item>
						<Menu-item name="__statis">
							<Icon type="ios-stats" />{{"Statis"|i18n}}
						</Menu-item>
						<Menu-item name="__monitors">
							<Icon type="ios-cog"></Icon>{{"Monitors"|i18n}}
						</Menu-item>
						<Menu-item name="__threadPool">
							<Icon type="ios-cog"></Icon>{{"Thread"|i18n}}
						</Menu-item>
						<Menu-item name="__resourceMonitorView">
							<Icon type="ios-cog"></Icon>{{"ResourceMonitor"|i18n("Resource Monitor")}}
						</Menu-item>
					</Menu-group>
					<Menu-group :title="'Log'|i18n">
						<Menu-item name="__invokeLinkView">
							<Icon type="ios-cog"></Icon>{{"InvokeLink"|i18n("Invoke Link")}}
						</Menu-item>
						<Menu-item name="__logItemView">
							<Icon type="ios-cog"></Icon>{{"MonitorLog"|i18n("Monitor Log")}}
						</Menu-item>
						<Menu-item name="__processLog">
							<Icon type="ios-cog"></Icon>{{"ProcessLog"|i18n("Process Log")}}
						</Menu-item>
					</Menu-group>
				</Submenu>

				<Submenu name="cfg">
					<template slot="title">
						<Icon type="ios-analytics" />{{"Config"|i18n}}
					</template>
					<Menu-group :title="'Config'|i18n">
						<Menu-item name="__config">
							<Icon type="ios-construct"></Icon>{{"SysConfig"|i18n('Sys Config')}}
						</Menu-item>
						<Menu-item name="__statisConfig">
							<Icon type="ios-alert" />{{"statisConfig"|i18n}}
						</Menu-item>
						<Menu-item name="__warningConfig">
							<Icon type="ios-alert" />{{"warningConfig"|i18n}}
						</Menu-item>
						<Menu-item name="__resourceConfig">
							<Icon type="ios-alert" />{{"resourceConfig"|i18n}}
						</Menu-item>
						<Menu-item name="__i18nConfig">
							<Icon type="ios-alert" />{{"i18nConfig"|i18n}}
						</Menu-item>
					</Menu-group>
					<Menu-group :title="'TypeConfig'|i18n">
						<Menu-item name="__typeConfig">
							<Icon type="ios-cog"></Icon>{{"BaseConfig"|i18n("Base Config")}}
						</Menu-item>
						<Menu-item name="__monitorType">
							<Icon type="ios-cog"></Icon>{{"MonitorTypes"|i18n("Monitor Types")}}
						</Menu-item>
						<Menu-item name="__monitorTypeServiceMethod">
							<Icon type="ios-cog"></Icon>{{"ServiceTypes"|i18n("Service Types")}}
						</Menu-item>
						<Menu-item name="__namedType">
							<Icon type="ios-cog"></Icon>{{"NamedTypes"|i18n("Named Types")}}
						</Menu-item>
						<Menu-item name="__jdataType">
							<Icon type="ios-cog"></Icon>{{"JDataType"|i18n("Data Type")}}
						</Menu-item>
					</Menu-group>

				</Submenu>

				<Submenu name="dep">
					<template slot="title">
						<Icon type="ios-analytics" />{{"Deployment"|i18n}}
					</template>
					<Menu-item name="__deploymentDesc">
						<Icon type="ios-alert" />{{"DeployDesc"|i18n("Deploy Desc")}}
					</Menu-item>
					<Menu-item name="__agent">
						<Icon type="ios-cog"></Icon>{{"Agents"|i18n}}
					</Menu-item>
					<Menu-item name="__process">
						<Icon type="ios-cog"></Icon>{{"Process"|i18n}}
					</Menu-item>
					<Menu-item name="__repository">
						<Icon type="ios-people"></Icon>{{"Repository"|i18n}}
					</Menu-item>
					<!--    <Menu-item name="__choreography"><Icon type="ios-cog"></Icon>{{"Choreography"|i18n}}</Menu-item>
	              <Menu-item name="__host"><Icon type="ios-cog"></Icon>{{"Host"|i18n}}</Menu-item>
	      -->
				</Submenu>

				<Submenu name="sec">
					<template slot="title">
						<Icon type="ios-analytics" />{{"Security"|i18n}}
					</template>
					<Menu-group :title="'Security'|i18n">
						<Menu-item v-if="isAdmin" name="__account">
							<Icon type="ios-alert" />{{"Account"|i18n}}
						</Menu-item>
						<Menu-item name="__role">
							<Icon type="ios-alert" />{{"Role"|i18n}}
						</Menu-item>
						<Menu-item name="__clientConfig">
							<Icon type="ios-alert" />{{"Client"|i18n}}
						</Menu-item>
						<Menu-item name="__userProfile">
							<Icon type="ios-alert" />{{"Profile"|i18n}}
						</Menu-item>
					</Menu-group>
					<Menu-group :title="'Interface'|i18n">
						<Menu-item name="__publicKeyList">
							<Icon type="ios-cog"></Icon>{{"publicKeyList"|i18n}}
						</Menu-item>
						<Menu-item name="__serviceMethodList">
							<Icon type="ios-cog"></Icon>{{"serviceMethodList"|i18n}}
						</Menu-item>
						<Menu-item name="__flow">
							<Icon type="ios-alert" />{{"flow"|i18n}}
						</Menu-item>
						<Menu-item name="__flowApproveInfo">
							<Icon type="ios-alert" />{{"FlowApproveInfo"|i18n}}
						</Menu-item>
					</Menu-group>

					<Menu-group :title="'DataService'|i18n">
						<Menu-item name="__interfaceDef">
							<Icon type="ios-alert" />{{"InterfaceDef"|i18n}}
						</Menu-item>
						<Menu-item name="__interfaceParamList">
							<Icon type="ios-alert" />{{"InterfaceParamList"|i18n}}
						</Menu-item>
						<Menu-item name="__interfaceUsedHistory">
							<Icon type="ios-alert" />{{"InterfaceUsedHistory"|i18n}}
						</Menu-item>
						<Menu-item name="__dayCostList">
							<Icon type="ios-alert" />{{"DayCostList"|i18n}}
						</Menu-item>
						<Menu-item name="__feeOrderList">
							<Icon type="ios-alert" />{{"FeeOrderList"|i18n}}
						</Menu-item>
						<Menu-item name="__actInterfaceList">
							<Icon type="ios-alert" />{{"AccountList"|i18n}}
						</Menu-item>
					</Menu-group>

				</Submenu>

				<Submenu name="oth">
					<template slot="title">
						<Icon type="ios-analytics" />{{'Others'|i18n}}
					</template>
					<Menu-group :title="'System'|i18n">
						<MenuItem name="__topicList">
						<Icon type="ios-cog"></Icon>{{"topicList"|i18n}}</MenuItem>
						<!--<Menu-item name="__router"><Icon type="ios-people"></Icon>{{"Router"|i18n}}</Menu-item>-->
						<Menu-item name="__routeRuleEditor">
							<Icon type="ios-people"></Icon>{{"Router"|i18n}}
						</Menu-item>
						<Menu-item name="__shell">
							<Icon type="ios-cog"></Icon>{{"Shell"|i18n}}
						</Menu-item>
					</Menu-group>

					<Menu-group :title="'Pubsub'|i18n">
						<Menu-item name="__pubsubItem">
							<Icon type="ios-alert" />{{"PubsubItems"|i18n("Pubsub Items")}}
						</Menu-item>
						<Menu-item name="__pubsubStatis">
							<Icon type="ios-alert" />{{"PubsubStatis"|i18n("Pubsub Statis")}}
						</Menu-item>
						<MenuItem name="__testingPubsub">
						<Icon type="ios-cog"></Icon>{{"PubsubTesting"|i18n("Pubsub Testing")}} </MenuItem>
					</Menu-group>

					<Menu-group :title="'Info'|i18n">
						<!-- <MenuItem name="__help"> <Icon type="ios-cog"></Icon>Help</MenuItem>-->
						<MenuItem name="__about">
						<Icon type="ios-cog"></Icon>{{"About"|i18n}}</MenuItem>
						<MenuItem name="__testing">
						<Icon type="ios-cog"></Icon>{{"Testing"|i18n}}</MenuItem>
					</Menu-group>
				</Submenu>

				<Submenu name="op">
					<template slot="title">
						<Icon type="ios-analytics" />{{"Operations"|i18n}}
					</template>
					<MenuItem v-for="mi in menus" :name="mi.name" :key="mi.name">
					<Icon :type="mi.icon"></Icon>{{mi.name|i18n}}
					</MenuItem>
				</Submenu>
			</Menu>
			<JAccount></JAccount>
		</div>
		
		<div class="content-bar" :style="{height:$jcontentHeight+'px'}"><router-view></router-view></div>
		
		<!-- <div class="foot-bar">
			<JBottomBar></JBottomBar>
		</div> -->
		
		<!-- 屏幕左边的打开抽屉按钮 -->
		<div :style="curSelect.drawerBtnStyle" class="drawerBtnStatu" @mouseenter="openDrawer()">
		</div>
		
			<!-- 服务监控列表 -->
			<Drawer v-model="cache.service.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JServiceList evt-name="serviceNodeSelect" group="service"></JServiceList>
			</Drawer>

			<Drawer v-model="cache.config.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JConfigList></JConfigList>
			</Drawer>

			<Drawer v-model="cache.statis.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JServiceList slId="statisSrvListId" evt-name="statisNodeSelect" group="statis"></JServiceList>
			</Drawer>

			<Drawer v-model="cache.monitors.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JMonitorList slId="monitorListId" evt-name="monitorNodeSelect"></JMonitorList>
			</Drawer>

			<!-- <Drawer  v-model="cache.router.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JRouterList></JRouterList>
          </Drawer>-->

			<Drawer v-model="cache.monitorType.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JMonitorTypeKeyList slId="monitorTypeKey" evt-name="monitorTypeKeySelect"></JMonitorTypeKeyList>
			</Drawer>

			<Drawer v-model="cache.monitorTypeServiceMethod.drawerStatus" :closable="false" placement="left"
				:transfer="true" :draggable="true" :scrollable="true" width="50">
				<JServiceList slId="monitorTypeServiceMethodId" evt-name="monitorTypeServiceMethodSelect" group="mtsm"
					menuStr="ins" groupBy="ins"></JServiceList>
			</Drawer>

			<Drawer v-model="cache.namedType.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JNamedTypeList slId="JNamedTypeId" evt-name="namedTypeSelect" group="namedType"></JNamedTypeList>
			</Drawer>

			<Drawer v-model="cache.threadPool.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JThreadPoolMonitorList slId="threadPoolId" evt-name="threadPoolSelect" group="threadPool">
				</JThreadPoolMonitorList>
			</Drawer>

			<Drawer v-model="cache.processLog.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JLogList slId="processLog"></JLogList>
			</Drawer>

			<Drawer v-model="cache.userProfile.drawerStatus" :closable="false" placement="left" :transfer="true"
				:draggable="true" :scrollable="true" width="50">
				<JUserProfileList slId="userProfile"></JUserProfileList>
			</Drawer>
			
	</div>
</template>

<script>
	import JBottomBar from './components/common/JBottomBar.vue'
	import JServiceList from './components/service/JServiceList.vue'
	import JConfigList from './components/config/JConfigList.vue'
	import JMonitorList from './components/monitor/JMonitorList.vue'
	/*import JRouterList from './components/route/JRouterList.vue'*/
	import JAccount from './components/common/JAccount.vue'
	/* import JToolBar from './components/common/JToolBar.vue'*/
	import JMonitorTypeKeyList from './components/monitor/JMonitorTypeKeyList.vue'
	import JNamedTypeList from './components/monitor/JNamedTypeList.vue'
	import JThreadPoolMonitorList from './components/monitor/JThreadPoolMonitorList.vue'
	import JLogList from './components/log/JLogList.vue'
	import JUserProfileList from "./components/security/JUserProfileList";
	import rpc from "@/rpc/rpcbase";
	import auth from "@/rpc/auth";
	import config from "@/rpc/config";
	let cache = null;
	const cid = 'jmicroApp';
	const mainMenus = {
		'op': true,
		'oth': true,
		'dep': true,
		'srv': true,
		'cfg': true
	};
	export default {
		name: 'App',
		mounted() {
			let self = this;
			auth.addActListener(cid, () => {
				self.isLogin = auth.isLogin();
				self.isAdmin = auth.isAdmin();
				if (self.activeEditorId) {
					self.selectMenu(self.activeEditorId);
				}
			});

			rpc.init({
					mod: 'jmng',
					useWs: true,
					clientId: 1
				},
				(isLogin) => {

				}
			);

			this.$bus.$on('editorOpen', function(opts) {
				if (!opts.editorId) {
					throw 'editorId is NULL';
				}
				if (!opts.menus) {
					opts.menus = [];
				}
				self.activeEditorId = opts.editorId;
				self.menusMap[opts.editorId] = opts.menus;
				self.menusMapCommon[opts.editorId] = self.filterMenus(opts.editorId);
				self.selectMenu(opts.editorId);
			});
			this.$bus.$on('menuChange', function(opts) {
				if (!opts.editorId) {
					throw 'editorId is NULL';
				}
				if (!opts.menus) {
					opts.menus = [];
				}
				self.menusMap[opts.editorId] = opts.menus;
				self.menusMapCommon[opts.editorId] = self.filterMenus(opts.editorId);
				if (self.activeEditorId == opts.editorId) {
					self.selectMenu(opts.editorId);
				}
			});
			this.$bus.$on('editorClosed', function(editorId) {
				delete self.menusMap[editorId];
				delete self.menusMapCommon[editorId];
				if (editorId == self.activeEditorId) {
					self.menus = [];
					self.activeEditorId = null;
				}
			});
			this.$bus.$on('editorActive', function(editorId) {
				self.activeEditorId = editorId;
				self.selectMenu(editorId);
			});
			this.$bus.$on('editorDeactive', function(editorId) {
				if (self.activeEditorId == editorId) {
					self.menus = [];
					self.activeEditorId = null;
				}
			});
		},
		components: {
			JUserProfileList,
			JServiceList,
			JConfigList,
			JMonitorList,
			/*JRouterList,*/
			JAccount,
			/*JToolBar,*/
			JMonitorTypeKeyList,
			JNamedTypeList,
			JThreadPoolMonitorList,
			JLogList,
			JBottomBar,
		},
		data() {
			if (!config.cache) {
				config.cache = {}
			}
			cache = config.cache;
			if (!cache.curSelectKey) {
				cache.curSelectKey = 'service';
			}
			cache['service'] = {
				key: 'service',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			cache['config'] = {
				key: 'config',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			cache['statis'] = {
				key: 'statis',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			cache['monitors'] = {
				key: 'monitors',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px'
				},
			};
			cache['monitorType'] = {
				key: 'monitorType',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px'
				},
			};
			cache['namedType'] = {
				key: 'namedType',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px'
				},
			};
			cache['monitorTypeServiceMethod'] = {
				key: 'monitorTypeServiceMethod',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px'
				},
			};
			cache['router'] = {
				key: 'router',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			cache['threadPool'] = {
				key: 'threadPool',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			cache['userProfile'] = {
				key: 'userProfile',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			cache['processLog'] = {
				key: 'processLog',
				drawerStatus: false,
				drawerBtnStyle: {
					left: '0px',
				},
			};
			/*cache['resourceMonitorView']={
			    key: 'resourceMonitorView',
			    drawerStatus:false,
			    drawerBtnStyle:{left:'0px',},
			};*/
			return {
				//ch:0,//视口高度
				//contentHeight:0,//内容区高度
				//containerHeight:0,//窗口区高度
				
				curSelect: cache[cache.curSelectKey],
				cache: cache,
				isLogin: false,
				isAdmin: false,
				menus: [
					/*{name:"test1",label:"label1",icon:"ios-cog"},
					              {name:"test2",label:"label2",icon:"ios-people"}*/
				],
				activeEditorId: null,
				menusMap: {},
				menusMapCommon: {},
			};
		},
		methods: {
			
			openDrawer() {
				this.curSelect.drawerStatus = true;
				this.curSelect.drawerBtnStyle.zindex = 10000;
				this.curSelect.drawerBtnStyle.left = '0px';
			},
			toRouter(key) {
				if (mainMenus[key]) {
					return;
				}
				if (key.startWith('__')) {
					key = key.substring(2);
					if (key == cache.curSelectKey) {
						this.openDrawer();
					} else if (cache[key]) {
						this.curSelect.drawerStatus = false;
						this.curSelect.drawerBtnStyle.zindex = -10000;
						cache.curSelectKey = key;
						this.curSelect = cache[key];
						this.openDrawer();
					} else {
						this.curSelect.drawerStatus = false;
						this.curSelect.drawerBtnStyle.zindex = -10000;
						this.curSelect.drawerBtnStyle.left = '-100px';
						this.$bus.$emit('openEditorSelect', key);
					}
				} else {
					let f = false;
					for (let i = 0; i < this.menus.length; i++) {
						let mi = this.menus[i];
						if (mi.name == key && typeof mi.call == 'function') {
							mi.call();
							f = true;
							break;
						}
					}
					if (!f) {
						this.$Message.error(key + " method not found!");
					}
				}
				/* this.$router.push('/'+key); */
			},
			doLoginOrLogout() {},
			selectMenu(editorId) {
				let self = this;
				if (self.isLogin) {
					self.menus = self.menusMap[editorId];
				} else {
					self.menus = self.menusMapCommon[editorId];
				}
			},
			filterMenus(editorId) {
				let ms = this.menusMap[editorId];
				if (!ms) {
					return [];
				}
				let cms = [];
				for (let i = 0; i < ms.length; i++) {
					let m = ms[i];
					if (!m.needAdmin) {
						cms.push(m);
					}
				}
				return cms;
			}
		}
	}
</script>

<style scoped="">
	
	.container {
		width:100%;
		overflow-y: auto;
	}
	
	.menu-bar {
		position:fixed;height:35px; top:0px; z-index:100;width:100%;
	}
	
	.menu-bar li {
		padding:0 5px;
	}
	
	.content-bar{
		margin-top:35px;
		width:100%;
	}
	
	.foot-bar {
		width:100%;
		height: 65px;
		background-color: blue;
		margin-top:8px;
	}
	
	#app {
		font-family: A venir, Helvetica, Arial, sans-serif;
		-webkit-font-smoothing: antialiased;
		-moz-osx-font-smoothing: grayscale;
		text-align: center;
		color: #2c3e50;
	}

	.drawerBtnStatu {
		position: fixed;
		left: 0px;
		top: 30%;
		bottom: 30%;
		height: 39%;
		width: 1px;
		border-left: 1px solid lightgray;
		background-color: lightgray;
		border-radius: 3px;
		z-index: 1000000;
	}
</style>
