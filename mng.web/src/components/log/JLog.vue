<template>
  <div class="JLog">
   {{content}}
  </div>
</template>

<script>

    const cid = 'JLog';

    export default {
        name: cid,
        data() {
            return {
                content:''
            }
        },

        props: {
            item : {
                type: Object,
                default: null
            },
        },

        methods: {

            getLog() {
                let self = this;
                let le = self.item.val;
                window.jm.mng.agentLogSrv.subscribeLog(le.processId,self.item.title,le.agentId,10,self.callback)
                    .then(rst =>{
                        self.$Message.success(rst);
                    }).catch(err =>{
                        self.$Message.error(err || "subscribe error");
                });
            },

            callback(it) {
                this.content = this.content + it.data[0];
                //console.log(it.data[0]);
            },

            refresh() {

            },

        },

        mounted () {
            //let self = this;
            this.getLog();
        },

        beforeDestroy() {
            window.jm.mng.act.removeListener(cid);
        },

        filters: {
            formatDate: function(time) {
                // 后期自己格式化
                return new Date(time).format("yyyy/MM/dd hh:mm:ss S") //Utility.formatDate(date, 'yyyy/MM/dd')
            }
        },

    }


</script>

<style scoped>
  .JLog{
      height:auto;
      width:100%;
      position: relative;
  }

</style>
