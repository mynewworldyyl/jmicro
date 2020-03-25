<template>
  <div>
    <JRouterList></JRouterList>
    <JRouterEditor :allow-many='false' ></JRouterEditor>
  </div>
</template>

<script>

    import JRouterList from './JRouterList.vue'
    import JRouterEditor from './JRouterEditor.vue'

export default {
    name: 'JRouter',
    components: {
        JRouterEditor,
        JRouterList,
    },
    props: {
    title: String
  }
}
</script>

<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

</style>
