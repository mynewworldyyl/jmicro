<template>
    <div class="JTestingPubsub" id="testing">

        <div class="publishCon">
            <div >{{msg}}</div>
            <Button :disabled="!isLogin" @click="doSend()">{{'Send'|i18n}}</Button>
            &nbsp;&nbsp;
            <Button :disabled="!isLogin" id="needResult" @click="changeResultSubStatus()">
                {{needSendResult?getMsg('NoNeedResult'):getMsg('NeedResult')}}
            </Button>
            &nbsp;&nbsp;
            <Button :disabled="!isLogin" @click="clearSendResult()">{{'ClearResult'|i18n}}</Button>

            <div>
                <label for="Topic">{{'SendTopic'|i18n}}</label>
                <Input id="Topic" v-model="sendTopic" placeholder=""/>
            </div>

            <div>
                <label for="Content">{{'SendContent'|i18n}}</label>
                <Input id="Content" v-model="content" placeholder=""/>
            </div>

            <br/>

            <div v-if="needSendResult">
                <label for="SendResultTopic">{{'SendResultTopic'|i18n}}</label>
                <Input id="SendResultTopic" v-model="sendResultTopic"/>
            </div>

            <div v-if="needSendResult">
                <label for="sendResultBox">{{'SendResult'|i18n}}</label>&nbsp;&nbsp;&nbsp;
                <Input id="sendResultBox"  class='textarea' type="textarea" v-model="sendResult"/>
            </div>

        </div>

        <div class="subscribeCon">
            <div>
                <Button :disabled="!isLogin" v-if="subState" @click="doSubscribe()">{{'Unsubscribe'|i18n}}</Button>
                <Button :disabled="!isLogin" v-if="!subState"  @click="doSubscribe()">{{'Subscribe'|i18n}}</Button>
                &nbsp;&nbsp;
                <Button :disabled="!isLogin"   @click="clear()">{{'Clear'|i18n}}</Button>
            </div>

            <div>
                <label for="SubTopic">{{'SubTopic'|i18n}}</label>
                <Input id="SubTopic" v-model="subTopic" placeholder=""/>
            </div>
            <div>
                <label for="Result">{{'RecieveMessage'|i18n}}</label>
                <Input id="Result"  class='textarea' type="textarea" v-model="result"/>
            </div>

        </div>
    </div>
</template>

<script>

    const cid="testingPubsub";

    export default {
        name: 'JTestingPubsub',
        components:{

        },

        data () {
            return {
                isLogin:false,
                sendTopic:'/jmicro/test/topic01',
                subTopic:'/jmicro/test/topic01',
                content:'test content',
                result:'',
                msg:'',
                subState:false,

                needSendResult:false,
                sendResult:'',
                sendResultTopic:'/jmicro/testresult/topic01',
            }
        },
        methods: {

            getMsg(key) {
                return  window.jm.mng.i18n.get(key);
            },

            sendResultCallback(msg) {
                if(!msg || msg.length == 0) {
                    this.$Message.info("Pubsub topic is disconnected by server")
                    this.changeResultSubStatus();
                }else {
                    let d = msg.data;
                    this.sendResult += 'Result: ' + d[0] + ', ID: '+ d[1] + ', Status Code: ' + d[2] + "\n";
                }
            },

            changeResultSubStatus() {
                if(!this.sendResultTopic || this.sendResultTopic.length == 0) {
                    this.$Message.info("Send result topic cannot be null!");
                    return;
                }

                let self = this;
                if(this.needSendResult) {
                    window.jm.ps.unsubscribe(this.sendResultTopic,this.sendResultCallback)
                        .then((succ)=>{
                            if(succ==true) {
                                self.needSendResult=false;
                            } else {
                                window.console.log(succ);
                            }
                        });
                } else {
                    window.jm.ps.subscribe(this.sendResultTopic,{},this.sendResultCallback)
                        .then((rst)=>{
                            if(rst >= 0) {
                                self.needSendResult=true;
                            }else {
                                console.log(rst);
                            }
                        });
                }
            },

            clear(){
                this.result="";
            },

            clearSendResult(){
                this.sendResult = '';
            },

            doSend() {
                if(!this.content || this.content.length == 0) {
                    this.msg = '发送内容不能为空';
                    return;
                }
                if(!this.sendTopic || this.sendTopic.length == 0) {
                    this.msg = '主题不能为空';
                    return;
                }

                let cb = null;
                if(this.needSendResult) {
                    if(!this.sendResultTopic || this.sendResultTopic.length == 0) {
                        this.msg = '发送结果主题不能为空';
                        return;
                    } else {
                        cb = this.sendResultTopic;
                    }
                }

                window.jm.ps.publishString(this.sendTopic,this.content,true,false,cb,{})
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
                    this.result += msg.id + ": "+ msg.data+"\n";
                }
            },

            doSubscribe(){
                if(!this.subTopic || this.subTopic.length == 0) {
                    this.msg = '主题不能为空';
                    return;
                }
                let self = this;
                if(this.subState) {
                    window.jm.ps.unsubscribe(this.subTopic,this.msgCallback)
                        .then((succ)=>{
                        if(succ==true) {
                            self.subState=false;
                        } else {
                            console.log(succ);
                        }
                    });
                }else {
                    window.jm.ps.subscribe(this.subTopic,{},this.msgCallback)
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
            let self = this;
            self.isLogin = window.jm.rpc.isLogin();
            window.jm.rpc.addActListener(cid,()=>{
                self.isLogin = window.jm.rpc.isLogin();
            });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);
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
        padding: 10px 0px 10px 10px;
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

    #sendResultBox{
        height: 50%;
    }

    textarea.ivu-input{
        height:100%;
    }

    .JTestingPubsub label {
        font-weight: bold;
    }

</style>