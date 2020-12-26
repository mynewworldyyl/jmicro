<template>
    <div class="ResourceDetail">
        <span v-for="(val,key) in resItem" :key="key">
           {{key|i18n}}:{{val}},
        </span>
    </div>
</template>

<script>

    export default {
        name: 'ResourceDetail',

        props:{
            resItem:{
                type:Object,
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
    .ResourceDetail{

    }

</style>
