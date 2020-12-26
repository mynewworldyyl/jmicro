<template>
    <div class="ResourceItem">
            <h5 class="itemTitleBar">
                <span>{{'ResourceType'|i18n}}:</span><span>{{item.resName || '-'}}</span>
                <span>{{'Tag'|i18n}}:</span><span>{{item.tag || '-'}},</span>
                <span>{{'Time'|i18n}}:</span><span>{{item.time | formatDate}}</span>
            </h5>
            <JListResource v-if="item.resName=='disk'" :resItem="metaData" :resType="item.resName"></JListResource>
            <JResourceDetail v-else :resItem="metaData" :resType="item.resName"></JResourceDetail>
    </div>
</template>

<script>

    import JResourceDetail from './JResourceDetail.vue'
    import JListResource from './JListResource.vue'

export default {
    name: 'ResourceItem',
    components: {
        JResourceDetail,
        JListResource
    },

    props:{
        item:{
            type:Object,
        },
    },

    computed:{
        metaData() {
            let data = this.item.metaData;
            let isArr = false;
            let cnt = 0;
            let k = null;
            for(let key in data) {
                cnt++;
                if(data[key] instanceof  Array) {
                    isArr = true;
                    k = key;
                }
            }

            if(cnt == 1 && isArr) {
                return data[k];
            }else {
               return data;
            }

        }
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
    .ResourceItem{

    }

     span {
        padding: 5px 2px;
    }
</style>
