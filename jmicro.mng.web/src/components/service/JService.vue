<template>
  <div>
    <JServiceList></JServiceList>
    <JServiceEditor :allow-many=true ></JServiceEditor>
  </div>
</template>

<script>

    import JServiceList from './JServiceList.vue'
    import JServiceEditor from './JServiceEditor.vue'

export default {
    name: 'JService',
    components: {
        JServiceEditor,
        JServiceList,
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
