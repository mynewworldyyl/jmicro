<template>
  <div id="logContentId" v-html="logContent"></div>

</template>

<script>

    import agentLogSrv from "@/rpcservice/agentLogSrv"
    const cid = 'JLog';

    export default {
        name: cid,
        data() {
            let self = this;
            return {
                logContent:'',
                autoScroll2Bottom : true,
                stop:true,
                autoSl: {name:"AutoScroll",label:"Stop Scroll",icon:"ios-cog",call:self.autoScroll},
                stopLg: {name:"StopLog",label:"Stop Log",icon:"ios-cog",call:self.changeLogStatus}
            }
        },

        watch: {
            logContent() {
                let self = this;
                if(self.autoScroll2Bottom) {
                    self.$nextTick(() => {
                        //self.$el.scrollTop = self.$el.scrollHeight;
                        //self.$refs.bottonTag.scrollIntoView();
                        //let c = document.querySelector(".editorBody");
                        //c.style.height = self.$el.scrollHeight;
                        //c.scrollTop = this.$el.scrollHeight;
                        this.$bus.$emit("scroptto",self.$el.scrollHeight);
                    })
                }
            }
        },

        updated(){
            if(this.autoScroll2Bottom) {
                //let c = document.querySelector(".editorBody");
                //c.style.height = this.$el.scrollHeight;
                //c.scrollTop = this.$el.scrollHeight;
                this.$bus.$emit("scroptto",this.$el.scrollHeight);
            }
        },

        props: {
            item : {
                type: Object,
                default: null
            },
        },

        methods: {
            callback(it) {
                this.logContent = this.logContent + it.data[0];
                //console.log(it.data[0]);
            },

            editorRemove(it) {
                if(this.item.id != it.id) {
                   return;
                }
                this.$off('tabItemRemove',this.editorRemove);
                this.changeLogStatus(true);
            },

            changeLogStatus(force) {
                let self = this;
                let le = self.item.val;
                if(!force && self.stop) {
                    let self = this;
                    let le = self.item.val;
                    agentLogSrv.subscribeLog(le.processId,self.item.title,le.agentId,100,self.callback)
                        .then(rst =>{
                            if(rst) {
                                self.stop = false;
                                self.stopLg.label="Stop Log";
                            }else {
                                self.$Message.success(rst);
                            }
                        }).catch(err =>{
                        self.$Message.error(err || "subscribe error");
                    });
                } else if(force || !force && !self.stop) {
                    agentLogSrv.unsubscribeLog(le.processId,self.item.title, le.agentId,self.callback)
                        .then(rst =>{
                            self.$Message.success(rst);
                            self.stop = true;
                            self.stopLg.label="Start Log";
                        }).catch(err =>{
                        self.$Message.error(err || "subscribe error");
                    });
                }
            },

            autoScroll() {
                this.autoScroll2Bottom =  !this.autoScroll2Bottom;
                if(this.autoScroll2Bottom) {
                    this.autoSl.label = "Stop Scroll";
                }else {
                    this.autoSl.label = "Auto Scroll";
                }
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            let self = this;
            this.$bus.$on('tabItemRemove',this.editorRemove);
            this.changeLogStatus();
            this.$bus.$emit("editorOpen", {"editorId":self.item.id, "menus":[self.autoSl,self.stopLg]});
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
      width:100%;
      position: relative;
      padding-left: 10px;
      overflow-y: auto;
  }

</style>
