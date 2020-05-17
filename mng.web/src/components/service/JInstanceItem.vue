<template>
  <div>
    <p>{{item.id}}</p>
  </div>
</template>

<script>

    import TreeNode from  "./JServiceList.vue"

export default {
    name: 'JInstanceItem',
    components: {
    },
    props: {
    item:TreeNode
  }
}
</script>

<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

</style>
