<template>
  <div>
    <JLineChart :charContainerId="node.id + '1'" :node="node"></JLineChart>
    <!--<JLineChart :charContainerId="node.id + '2'"  :node="node"></JLineChart>-->
    <!--<JLineChart></JLineChart>
    <JLineChart></JLineChart>-->
  </div>
</template>

<script>

    //import TreeNode from "../service/JServiceList.vue"
    import JLineChart from "../chart/JLineChart.vue"

export default {
    name: 'JStatisSMethodItemView',
    components: {
        JLineChart
    },
    props: {
        meth : {type:Object,default:null}
    },

    methods: {

    },

    data(){
        return {
            node : this.meth,
        }
    }
}
</script>

<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

</style>
