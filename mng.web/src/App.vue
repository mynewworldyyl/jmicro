<template>

  <div>
      <div style="position:fixed; right:0px; left:0px; height:60px; top:0px; z-index:100">
          <Menu mode="horizontal" theme="light" active-key="service" @on-select="toRouter">
              <Submenu name="mo">
                  <template slot="title"><Icon type="ios-cog" />MONITOR</template>
                  <Menu-group title="MONITOR">
                      <Menu-item name="__service" ><Icon type="ios-paper"></Icon>SERVICE</Menu-item>
                      <Menu-item name="__statis"><Icon type="ios-stats" />STATIS</Menu-item>
                      <Menu-item name="__monitors"><Icon type="ios-cog"></Icon>MONITORS</Menu-item>
                      <Menu-item name="__threadPool"><Icon type="ios-cog"></Icon>THREAD</Menu-item>
                  </Menu-group>
                  <Menu-group title="LOG">
                      <Menu-item name="__invokeLinkView"><Icon type="ios-cog"></Icon>INVOKE LINK</Menu-item>
                      <Menu-item name="__logItemView"><Icon type="ios-cog"></Icon>Monitor LOG</Menu-item>
                      <Menu-item name="__processLog"><Icon type="ios-cog"></Icon>Process Log</Menu-item>
                  </Menu-group>
                  <Menu-group title="CFG">
                      <Menu-item name="__warning"><Icon type="ios-alert" />WARNING</Menu-item>
                      <Menu-item name="__typeConfig"><Icon type="ios-cog"></Icon>TYPE CONFIG</Menu-item>
                      <Menu-item name="__monitorType"><Icon type="ios-cog"></Icon>MONITOR TYPES</Menu-item>
                      <Menu-item name="__monitorTypeServiceMethod"><Icon type="ios-cog"></Icon>SERVICE TYPES</Menu-item>
                      <Menu-item name="__namedType"><Icon type="ios-cog"></Icon>NAMED TYPES</Menu-item>
                  </Menu-group>
               </Submenu>

              <Submenu name="d">
                  <template slot="title">
                      <Icon type="ios-analytics" />
                      DEPLOYMENT
                  </template>
                  <Menu-item name="__deploymentDesc"><Icon type="ios-alert" />DEPLOY DESC</Menu-item>
                  <Menu-item name="__agent"><Icon type="ios-cog"></Icon>AGENTS</Menu-item>
                  <Menu-item name="__process"><Icon type="ios-cog"></Icon>PROCESS</Menu-item>
                  <Menu-item name="__repository"><Icon type="ios-people"></Icon>REPOSITORY</Menu-item>
                  <Menu-item name="__choreography"><Icon type="ios-cog"></Icon>CHOREOGRAPHY</Menu-item>
                  <Menu-item name="__host"><Icon type="ios-cog"></Icon>HOST</Menu-item>
              </Submenu>

              <Submenu name="o">
                  <template slot="title">
                      <Icon type="ios-analytics" />
                      OTHERS
                  </template>
                  <Menu-item name="__config"><Icon type="ios-construct"></Icon>CONFIG</Menu-item>
                  <Menu-item name="__router"><Icon type="ios-people"></Icon>ROUTER</Menu-item>
                  <Menu-item name="__shell"><Icon type="ios-cog"></Icon>SHELL</Menu-item>
                 <!-- <Menu-item name="log"><Icon type="ios-filing"></Icon>LOG</Menu-item>-->
                  <MenuItem name="__help"> <Icon type="ios-cog"></Icon>HELP</MenuItem>
                  <MenuItem name="__about"> <Icon type="ios-cog"></Icon>ABOUT</MenuItem>
                 <!-- <MenuItem name="contact"> <Icon type="ios-cog"></Icon>CONTACT ME</MenuItem>-->
                  <MenuItem name="__testing"> <Icon type="ios-cog"></Icon>TESTING</MenuItem>
              </Submenu>

              <Submenu name="me">
                  <template slot="title">
                      <Icon type="ios-analytics" />
                      MENUS
                  </template>

                  <MenuItem  v-for="mi in menus" :name="mi.name" :key="mi.name">
                      <Icon :type="mi.icon"></Icon>{{mi.label}}</MenuItem>

              </Submenu>

          </Menu>
          <JToolBar></JToolBar>
          <JAccount></JAccount>
      </div>

      <!-- 屏幕左边的打开抽屉按钮 -->
      <div :style="curSelect.drawerBtnStyle" class="drawerBtnStatu" @mouseenter="openDrawer()"></div>

      <div style="margin-top:60px;">
          <!-- 服务监控列表 -->
          <Drawer  v-model="cache.service.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JServiceList evt-name="serviceNodeSelect" group="service"></JServiceList>
          </Drawer>

          <Drawer v-model="cache.config.drawerStatus" :closable="false" placement="left" :transfer="true"
                  :draggable="true" :scrollable="true" width="50">
              <JConfigList></JConfigList>
          </Drawer>

          <Drawer  v-model="cache.statis.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JServiceList slId="statisSrvListId" evt-name="statisNodeSelect"  group="statis"></JServiceList>
          </Drawer>

          <Drawer  v-model="cache.monitors.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JMonitorList slId="monitorListId" evt-name="monitorNodeSelect"></JMonitorList>
          </Drawer>

          <Drawer  v-model="cache.router.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JRouterList></JRouterList>
          </Drawer>

          <Drawer  v-model="cache.monitorType.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JMonitorTypeKeyList slId="monitorTypeKey" evt-name="monitorTypeKeySelect"></JMonitorTypeKeyList>
          </Drawer>

          <Drawer  v-model="cache.monitorTypeServiceMethod.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JServiceList slId="monitorTypeServiceMethodId" evt-name="monitorTypeServiceMethodSelect"
                            group="mtsm" menuStr="ins" groupBy="ins"></JServiceList>
          </Drawer>

          <Drawer  v-model="cache.namedType.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JNamedTypeList slId="JNamedTypeId" evt-name="namedTypeSelect"
                            group="namedType"></JNamedTypeList>
          </Drawer>

          <Drawer  v-model="cache.threadPool.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JThreadPoolMonitorList slId="threadPoolId" evt-name="threadPoolSelect"
                              group="threadPool"></JThreadPoolMonitorList>
          </Drawer>

          <Drawer  v-model="cache.processLog.drawerStatus" :closable="false" placement="left" :transfer="true"
                   :draggable="true" :scrollable="true" width="50">
              <JLogList slId="processLog"></JLogList>
          </Drawer>

          <!-- route outlet -->
          <router-view></router-view>
      </div>

  </div>
</template>

<script>

    import JServiceList from './components/service/JServiceList.vue'
    import JConfigList from './components/config/JConfigList.vue'
    import JMonitorList from './components/monitor/JMonitorList.vue'
    import JRouterList from './components/route/JRouterList.vue'
    import JAccount from './components/common/JAccount.vue'
    import JToolBar from './components/common/JToolBar.vue'
    import JMonitorTypeKeyList from './components/monitor/JMonitorTypeKeyList.vue'
    import JNamedTypeList from './components/monitor/JNamedTypeList.vue'
    import JThreadPoolMonitorList from './components/monitor/JThreadPoolMonitorList.vue'
    import JLogList  from './components/log/JLogList.vue'

    let cache = null;

export default {
    name: 'App',
    mounted() {
        //window.jm.rpc.config.ip='';
        window.jm.rpc.init();
        //jm.mng.init();
        let self = this;

        window.jm.vue.$on('editorOpen',function(opts) {
            if(!opts.editorId) {
                throw 'editorId is NULL';
            }

            if(!opts.menus) {
                opts.menus = [];
            }

            self.activeEditorId = opts.editorId;
            self.menusMap[opts.editorId] = opts.menus;
            self.menus = self.menusMap[opts.editorId];
        });

        window.jm.vue.$on('editorClosed',function(editorId) {
            delete self.menusMap[editorId];
            if(editorId == self.activeEditorId) {
                self.menus = [];
            }
        });

        window.jm.vue.$on('editorActive',function(editorId) {
            self.activeEditorId = editorId;
            self.menus = self.menusMap[editorId];
        });

    },

  components: {
        JServiceList,
        JConfigList,
        JMonitorList,
        JRouterList,
        JAccount,
        JToolBar,
        JMonitorTypeKeyList,
        JNamedTypeList,
        JThreadPoolMonitorList,
        JLogList,

    },

    data() {

        if(!window.jm.mng.cache) {
             window.jm.mng.cache = {}
        }

        cache = window.jm.mng.cache;

        if(!cache.curSelectKey) {
            cache.curSelectKey = 'service';
        }

         cache['service']={
            key: 'service',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px',},
        };

        cache['config']={
            key: 'config',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px',},
        };

        cache['statis']={
            key: 'statis',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px',},
        };

        cache['monitors']={
            key: 'monitors',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px'},
        };

        cache['monitorType']={
            key: 'monitorType',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px'},
        };

        cache['namedType']={
            key: 'namedType',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px'},
        };

        cache['monitorTypeServiceMethod']={
            key: 'monitorTypeServiceMethod',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px'},
        };

        cache['router']={
            key: 'router',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px',},
        };

        cache['threadPool']={
            key: 'threadPool',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px',},
        };

        cache['processLog']={
            key: 'processLog',
            drawerStatus:false,
            drawerBtnStyle:{left:'0px',},
        };

      return {
          curSelect: cache[cache.curSelectKey],
          cache: cache,
          menus:[/*{name:"test1",label:"label1",icon:"ios-cog"},
              {name:"test2",label:"label2",icon:"ios-people"}*/],
          activeEditorId : null,
          menusMap:{

          }
      };
    },

  methods:{

      openDrawer() {
          this.curSelect.drawerStatus = true;
          this.curSelect.drawerBtnStyle.zindex = 10000;
          this.curSelect.drawerBtnStyle.left = '0px';
      },

      toRouter(key) {
          if(key == 'o' || key == 'd' || key =='mo' || key == 'me') {
              return
          }else if( key.startWith('__')) {
              key  = key.substring(2);
              if(key == cache.curSelectKey  ) {
                  this.openDrawer();
              } else if(cache[key]) {
                  this.curSelect.drawerStatus = false;
                  this.curSelect.drawerBtnStyle.zindex = -10000;
                  cache.curSelectKey = key;
                  this.curSelect = cache[key];
                  this.openDrawer();
              } else {
                  this.curSelect.drawerStatus = false;
                  this.curSelect.drawerBtnStyle.zindex = -10000;
                  this.curSelect.drawerBtnStyle.left = '-100px';
                  window.jm.vue.$emit('openEditorSelect',key);
              }
          } else {
              let f = false;
              for(let i = 0; i < this.menus.length; i++) {
                  let mi = this.menus[i];
                  if(mi.name == key) {
                      mi.call();
                      f = true;
                      break;
                  }
              }
              if(!f) {
                  this.$Message.error(key + " method not found!");
              }
          }

          /* this.$router.push('/'+key); */
      },

      doLoginOrLogout(){

      }

  }
}
</script>

<style>

#app {
  font-family: A
  venir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

.drawerBtnStatu{
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
