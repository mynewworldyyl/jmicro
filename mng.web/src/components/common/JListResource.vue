<template>
    <div class="ListResource">
        <JResourceDetail v-for ="(it,idx) in resItem" :key="idx"  :resItem="it" :resType="resType">
        </JResourceDetail>
    </div>
</template>

<script>

    import JResourceDetail from './JResourceDetail.vue'

    export default {
        name: 'ListResource',

        components: {
            JResourceDetail
        },

        props:{
            resItem:{
                type:Array,
            },
            resType:{
                type:String,
            },
        },

        data() {
            return {

            };
        },

        mounted(){

        },

        methods: {

        },
    }

</script>

<style scoped>
    .ListResource{

    }

</style>
