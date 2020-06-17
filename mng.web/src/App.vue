<template>

  <div>
      <div style="position: fixed;right: 0px;left: 0px; height: 60px;top: 0px;z-index:100">
          <Menu mode="horizontal" theme="light" active-key="service" @on-select="toRouter">
              <Menu-item name="service" >
                  <Icon type="ios-paper"></Icon>
                  SERVICE
              </Menu-item>

              <Menu-item name="statis">
                  <Icon type="ios-stats" />
                  STATIS
              </Menu-item>

              <Submenu name="mo">
                  <template slot="title">
                      <Icon type="ios-cog" />
                      MONITOR
                  </template>
                  <Menu-item name="monitors"><Icon type="ios-cog"></Icon>MONITORS</Menu-item>
                  <Menu-item name="typeConfig"><Icon type="ios-cog"></Icon>TYPE CONFIG</Menu-item>
                  <Menu-item name="monitorType"><Icon type="ios-cog"></Icon>MONITOR TYPES</Menu-item>
                  <Menu-item name="monitorTypeServiceMethod"><Icon type="ios-cog"></Icon>MONITOR SERVICE</Menu-item>
              </Submenu>

              <Menu-item name="config">
                  <Icon type="ios-construct"></Icon>
                  CONFIG
              </Menu-item>

              <Submenu name="d">
                  <template slot="title">
                      <Icon type="ios-analytics" />
                      DEPLOYMENT
                  </template>
                  <Menu-item name="choreography"><Icon type="ios-cog"></Icon>CHOREOGRAPHY</Menu-item>
                  <Menu-item name="agent"><Icon type="ios-cog"></Icon>AGENTS</Menu-item>
                  <Menu-item name="process"><Icon type="ios-cog"></Icon>PROCESS</Menu-item>
                  <Menu-item name="repository"><Icon type="ios-people"></Icon>REPOSITORY</Menu-item>
                  <Menu-item name="deploymentDesc"><Icon type="ios-alert" />DEPLOY DESC</Menu-item>
                  <Menu-item name="host"><Icon type="ios-cog"></Icon>HOST</Menu-item>
              </Submenu>

              <Submenu name="o">
                  <template slot="title">
                      <Icon type="ios-analytics" />
                      OTHERS
                  </template>
                  <Menu-item name="router"><Icon type="ios-people"></Icon>ROUTER</Menu-item>
                  <Menu-item name="shell"><Icon type="ios-cog"></Icon>SHELL</Menu-item>
                  <Menu-item name="warning"><Icon type="ios-alert" />WARNING</Menu-item>
                  <Menu-item name="log"><Icon type="ios-filing"></Icon>LOG</Menu-item>
                  <MenuItem name="help"> <Icon type="ios-cog"></Icon>HELP</MenuItem>
                  <MenuItem name="about"> <Icon type="ios-cog"></Icon>ABOUT</MenuItem>
                  <MenuItem name="contact"> <Icon type="ios-cog"></Icon>CONTACT ME</MenuItem>
              </Submenu>

          </Menu>
          <JAccount></JAccount>
      </div>

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
    import JMonitorTypeKeyList from './components/monitor/JMonitorTypeKeyList.vue'

    let cache = null;

export default {
  name: 'App',
  components: {
        JServiceList,
        JConfigList,
        JMonitorList,
        JRouterList,
        JAccount,
        JMonitorTypeKeyList,
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


      return {
          curSelect: cache[cache.curSelectKey],
          cache: cache,
      };
    },

  methods:{

      openDrawer() {
          this.curSelect.drawerStatus = true;
          this.curSelect.drawerBtnStyle.zindex = 10000;
          this.curSelect.drawerBtnStyle.left = '0px';
      },

      toRouter(key) {
          if(key == 'o' || key == 'd' || key =='mo') {
              return
          }else if(key == cache.curSelectKey  ) {
              this.openDrawer();
          } else if(cache[key]) {
              this.curSelect.drawerStatus = false;
              this.curSelect.drawerBtnStyle.zindex = -10000;
              cache.curSelectKey = key;
              this.curSelect = cache[key];
              this.openDrawer();
          }else {
              this.curSelect.drawerStatus = false;
              this.curSelect.drawerBtnStyle.zindex = -10000;
              this.curSelect.drawerBtnStyle.left = '-100px';
              window.jm.vue.$emit('openEditorSelect',key);
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
.JHeader{
    width:auto;
    height:39px;
    position: relative;
    top: 0px;
    left: 0px;
    border-radius: 3px;
    background-color:lightsteelblue;
    vertical-align: middle;
    line-height: 39px;
    text-align: left;
    padding-left:6px;
    font-weight:bold;
}

.mainMenuItem{
    display:inline-block;
    width:50px;
    height:auto;
    padding:5px 8px;
    margin: 0px 8px;
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
