<template>

  <div>
      <Menu mode="horizontal" theme="light" active-key="service" @on-select="toRouter">
          <Menu-item name="" >
              <Icon type="ios-paper"></Icon>
              SERVICE
          </Menu-item>
          <Menu-item name="config">
              <Icon type="ios-people"></Icon>
              CONFIG
          </Menu-item>

          <Menu-item name="router">
              <Icon type="ios-people"></Icon>
              ROUTER
          </Menu-item>

          <Menu-item name="statis">
              <Icon type="ios-people"></Icon>
              STATIS
          </Menu-item>

          <Menu-item name="log">
              <Icon type="ios-people"></Icon>
              LOG
          </Menu-item>
          
          <Menu-item name="shell">
              <Icon type="ios-people"></Icon>
              SHELL
          </Menu-item>

          <Menu-item name="others">
              <Icon type="ios-people"></Icon>
              OTHERS
          </Menu-item>
      </Menu>

      <!-- route outlet -->
      <router-view></router-view>
  </div>
</template>

<script>


export default {
  name: 'App',
  components: {
  },

  methods:{
      toRouter(key) {
        this.$router.push('/'+key);
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

</style>
