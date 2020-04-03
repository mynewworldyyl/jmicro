<template>
  <div>
  </div>
</template>

<script>

    import TreeNode from "../service/JServiceList.vue"

export default {
    name: 'JStatisServerItemView',
    components: {
    },
    props: {
        meth : TreeNode
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

</style>
