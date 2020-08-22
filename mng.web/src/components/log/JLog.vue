<template>
  <div class="JLog" v-html="content">
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

            editorRemove(it) {
                if(this.item.id != it.id) {
                   return;
                }
                let self = this;
                let le = self.item.val;
                window.jm.vue.$off('tabItemRemove',self.editorRemove);
                window.jm.mng.agentLogSrv.unsubscribeLog(le.processId,self.item.title, le.agentId,self.callback)
                    .then(rst =>{
                        self.$Message.success(rst);
                    }).catch(err =>{
                    self.$Message.error(err || "subscribe error");
                });
            },

            autoScroll() {

            },

            stopScroll() {

            },
        },

        mounted () {
            let self = this;
            window.jm.vue.$on('tabItemRemove',this.editorRemove);
            this.getLog();
            window.jm.vue.$emit("editorOpen",
                {
                    "editorId":self.item.id,
                    "menus":[{name:"AutoScroll",label:"Auto Scroll",icon:"ios-cog",call:self.autoScroll},
                        {name:"StopScroll",label:"Stop Scroll",icon:"ios-cog",call:self.stopScroll}]
                });
        },

        beforeDestroy() {
            this.editorRemove(this.item);
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
      padding-left: 10px;
  }

</style>
