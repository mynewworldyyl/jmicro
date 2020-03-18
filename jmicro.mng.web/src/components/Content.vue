<template>
  <div class="JContent">
    <j-left-view></j-left-view>
    <main-content-editor allowMany="false"></main-content-editor>
  </div>
</template>

<script>
    import JLeftView from './LeftView.vue'
    import MainContentEditor from './MainContentEditor.vue'

export default {
  name: 'j-content',
    components: {
        MainContentEditor,
        JLeftView,
    },
    props: {
    title: String
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

</style>
