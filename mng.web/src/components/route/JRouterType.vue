<template>
  <div class="JRouterType">
      {{selectNode.id}}
  </div>
</template>

<script>

   // import TreeNode from "./JRouterList";

    export default {
        name: 'JRouterType',
        components: {
        },

        data () {
            return {
                selectNode:this.item,
            }
        },

        props:{
            item: {
                type: Object,
                default: null
            },
        },

        methods: {
        }
    }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .JRouterType{
      height:auto;
     /* min-height: 500px;*/
      overflow:hidden;
  }

</style>
