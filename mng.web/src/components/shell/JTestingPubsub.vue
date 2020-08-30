<template>
    <div class="JTestingPubsub" id="testing">
        <div >{{msg}}</div>
        <div class="publishCon">
            <Button @click="doSend()">Send</Button><br/>
            <label for="Topic">Topic</label>
            <Input id="Topic" v-model="topic" placeholder=""/>

            <label for="Content">Content</label>
            <Input id="Content" v-model="content" placeholder=""/>
        </div>

        <div class="subscribeCon">
            <Button v-if="subState" @click="doSubscribe()">Unsubscribe</Button>
            <Button  v-if="!subState"  @click="doSubscribe()">Subscribe</Button>
            <br/>
            <label for="Result">Result</label>
            <Input id="Result"  class='textarea' type="textarea" v-model="result"/>
        </div>
    </div>
</template>

<script>

    //const cid="testingPubsub";

    export default {
        name: 'JTestingPubsub',
        components:{

        },

        data () {
            return {
                topic:'/jmicro/test/topic01',
                content:'test content',
                result:'',
                msg:'',
                subState:false,
            }
        },
        methods: {
            doSend() {
                if(!this.content || this.content.length == 0) {
                    this.msg = '发送内容不能为空';
                    return;
                }
                if(!this.topic || this.topic.length == 0) {
                    this.msg = '主题不能为空';
                    return;
                }
                window.jm.ps.publishString({}, this.topic,this.content)
                    .then(rst=>{
                        console.log(rst);
                    }).catch(err=>{
                       console.log(err)
                });
            },

            msgCallback(msg) {
                if(!msg || msg.length == 0) {
                    this.$Message.info("Pubsub topic is disconnected by server")
                    this.doSubscribe();
                }else {
                    this.result += msg.data+"\n";
                }
            },

            doSubscribe(){
                if(!this.topic || this.topic.length == 0) {
                    this.msg = '主题不能为空';
                    return;
                }
                let self = this;
                if(this.subState) {
                    window.jm.ps.unsubscribe(this.topic,this.msgCallback)
                        .then((succ)=>{
                        if(succ==true) {
                            self.subState=false;
                        } else {
                            console.log(succ);
                        }
                    });
                }else {
                    window.jm.ps.subscribe(this.topic,{},this.msgCallback)
                        .then((rst)=>{
                        if(rst >= 0) {
                            self.subState=true;
                        }else {
                            console.log(rst);
                        }
                    });
                }
            }
        },

        mounted () {

        },

        beforeDestroy () {

        }
    }
</script>

<style>
    .JTestingPubsub{
        margin-bottom: 20px;
        height:90%;
    }

    .publishCon, .subscribeCon{
        height:100%;
        width:50%;
    }

    .publishCon{
        float:left;
    }
    .subscribeCon{
        float:right;
        height: 90%;
    }

    .textarea{
        height: 100%;
    }

    textarea.ivu-input{
        height:100%;
    }

</style>