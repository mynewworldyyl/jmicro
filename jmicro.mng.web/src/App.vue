<template>
  <div id="app">
      <j-left-view></j-left-view>
      <main-content-editor :allow-many=false></main-content-editor>
  </div>
</template>

<script>
    import JLeftView from './components/LeftView.vue'
    import MainContentEditor from './components/MainContentEditor.vue'

export default {
  name: 'App',
  components: {
      MainContentEditor,
      JLeftView,
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
</style>
