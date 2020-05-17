<template>
  <div>
  <JStatisView dataId="JStatisView" :allow-many=true ></JStatisView>

  <div :style="drawerBtnStyle" class="drawerBtnStatu" @mouseenter="openDrawer()"></div>
  <Drawer :closable="false" placement="left" v-model="drawerStatus" :transfer="true"
          :draggable="true" :scrollable="true" width="50">
      <JServiceList slId="statisSrvListId" evt-name="statisNodeSelect"></JServiceList>
  </Drawer>

  </div>
</template>

<script>

    import JServiceList from '../service/JServiceList.vue'
    import JStatisView from './JStatisView.vue'

export default {
    name: 'JStatis',
    components: {
        JStatisView,
        JServiceList,
    },
    data() {
        return {
            drawerStatus: false,
            pStyle: {
                fontSize: '16px',
                color: 'rgba(0,0,0,0.85)',
                lineHeight: '24px',
                display: 'block',
                marginBottom: '16px'
            },
            drawerBtnStyle:{

            }
        }
    },
    props: {
    title: String
  },
    methods:{
        openDrawer() {
            this.drawerStatus=true;
            this.drawerBtnStyle.zindex = -10000;
        }
    }
}
</script>

<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

  .demo-drawer-profile{
      font-size: 14px;
  }

  .demo-drawer-profile .ivu-col{
      margin-bottom: 12px;
  }

  .serverListContainer{
    height:auto;
    min-height: 500px;
    min-width:25%;
    max-width: 50%;
    float: left;
    border-right:1px solid lightgray;
    border-top:1px solid lightgray;
    text-align: left;
    word-break: break-all;
  }

  .drawerBtnStatu{
      position: absolute;
      left: 0px;
      top: 30%;
      bottom: 30%;
      height: 39%;
      width: 1px;
      border-left: 1px solid lightgray;
      background-color: lightgray;
      border-radius: 3px;
  }

</style>
