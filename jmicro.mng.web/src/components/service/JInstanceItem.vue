<template>
  <div>
    <p>{{node.id}}</p>
  </div>
</template>

<script>

    import TreeNode from  "./JServiceList.vue"

export default {
    name: 'JInstanceItem',
    components: {
    },
    props: {
    node:TreeNode
  }
}
</script>

<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

</style>
