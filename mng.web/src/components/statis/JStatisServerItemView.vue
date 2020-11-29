<template>
  <div>
    <p stype="word-break: break-all;padding: 0px 10px;font-size: medium;">{{node.id}}</p>
    <JLineChart titleName="QPS" type="qps" indexTypes="240,209,246" :charContainerId="node.id + '1'" :node="node"></JLineChart>
    <JLineChart titleName="Percent" type="percent"   indexTypes="240,209,246" :charContainerId="node.id + '2'" :node="node"></JLineChart>
    <JLineChart titleName="Total" type="total"   indexTypes="240,209,246" :charContainerId="node.id + '3'" :node="node"></JLineChart>
    <JIndexPercent titleName="QPS" type="qps"   :charContainerId="node.id + '4'" :node="node"></JIndexPercent>
    <JIndexPercent titleName="Percent" type="percent"   :charContainerId="node.id + '5'" :node="node"></JIndexPercent>
    <JIndexPercent titleName="Total" type="total"   :charContainerId="node.id + '6'" :node="node"></JIndexPercent>
  </div>
</template>

<script>

    //import TreeNode from "../service/JServiceList.vue"
    import JLineChart from "../chart/JLineChart.vue"
    import JIndexPercent from "../chart/JIndexPercent.vue"

    export default {
        name: 'JStatisServiceItemView',
        components: {
            JLineChart,
            JIndexPercent
        },
        props: {
            serverNode : {type:Object,default:null}
        },

        methods: {

        },

        data(){
            return {
                node : this.serverNode,
            }
        }
    }
</script>

<style scoped>

  .StatisCard{
    width:350px;
    display:inline-block
  }



</style>
