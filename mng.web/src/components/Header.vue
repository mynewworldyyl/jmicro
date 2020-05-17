<template>
  <div class="JHeader">
  </div>
</template>

<script>
export default {
  name: 'j-header',
  props: {
    title: String
  }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JHeader{
      height:39px;
      position: relative;
      top: 0px;
      left: 0px;
      border-radius: 3px;
      background-color:white;
      vertical-align: middle;
      line-height: 39px;
      text-align: left;
  }

</style>
