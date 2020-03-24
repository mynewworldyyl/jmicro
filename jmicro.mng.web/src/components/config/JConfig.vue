<template>
  <div>
    <JConfigList></JConfigList>
    <JConfigEditor :allow-many=false></JConfigEditor>
  </div>
</template>

<script>

    import JConfigList from './JConfigList.vue'
    import JConfigEditor from './JConfigEditor.vue'

export default {
    name: 'JConfig',
    components: {
        JConfigEditor,
        JConfigList,
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
