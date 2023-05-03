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
			&nbsp;&nbsp;
			<Button :disabled="!isLogin" @click="changeChannel()">{{channel=='p2p'?"P2P":'PS'}}</Button>
			&nbsp;&nbsp;
			<Button :disabled="!isLogin" @click="byActOrTopic()">{{byType}}</Button>
            <div>
                <label for="Topic">{{'SendTopic'|i18n}}</label>
                <Input id="Topic" v-model="sendTopic" placeholder=""/>
            </div>
			
			<div>
				<label for="To">{{'ToTarget'|i18n}}</label>
				<Input id="To" v-model="to" placeholder=""/>
				<label for="Type">{{'Type'|i18n}}</label>
				<Input id="Type" v-model="type" placeholder=""/>
			</div>

            <div>
                <label for="Content">{{'SendContent'|i18n}}</label>
                <!-- <Input id="Content" v-model="content" placeholder=""/>-->
				<Input id="Content"  class='textarea' type="textarea" v-model="content"/>
            </div>
			
			<div>
			    <label for="cxt">{{'Context'|i18n}}</label>
			    <!-- <Input id="Content" v-model="content" placeholder=""/>-->
				<Input id="cxt"  class='textarea' type="textarea" v-model="cxt"/>
			</div>

            <br/>

            <div v-if="needSendResult">
                <label for="SendResultTopic">{{'SendResultTopic'|i18n}}</label>
                <Input id="SendResultTopic" v-model="sendResultTopic"/>
            </div>

            <div>
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
    import ps from "@/rpc/pubsub"
    import i18n from "@/rpc/i18n"
	import{Constants} from "@/rpc/message"
	import PSData from "@/rpc/psdata"

    const cid="testingPubsub";
	
	const channel_p2p="p2p";//端对端消息
	const channel_ps="ps";//通过消息代理发送

    export default {
        name: 'JTestingPubsub',
        components:{},

        data () {
            return {
                isLogin:false,
                //sendTopic:'/jmicro/test/topic01',
				sendTopic : "/__act/dev/25500/testdevice001",
                subTopic : '/__act/dev/25500/testDeivceMsg',
                content : this.$jr.lc.get('psContent'),
				cxt : this.$jr.lc.get('psCxt'),
                result : '',
                msg : '',
                subState : false,
				to: this.$jr.lc.get('psTo'),
				channel : 'p2p',
				byType:'topic',
				type:this.$jr.lc.get('type'),
                needSendResult:false,
                sendResult:'',
                sendResultTopic:'/jmicro/testresult/topic01',
            }
        },
        methods: {

            getMsg(key) {
                return  i18n.get(key);
            },
			
			changeChannel(){
				if(this.channel == channel_p2p) {
					this.channel = channel_ps;
				}else {
					this.channel = channel_p2p;
				}
			},
			
			byActOrTopic(){
				if(this.byType == 'actId') {
					this.byType = 'topic';
				}else {
					this.byType = 'actId';
				}
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
                    ps.unsubscribe(this.sendResultTopic,this.sendResultCallback)
                        .then((succ)=>{
                            if(succ==true) {
                                self.needSendResult=false;
                            } else {
                                window.console.log(succ);
                            }
                        });
                } else {
                    ps.subscribe(this.sendResultTopic,{},this.sendResultCallback)
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
				
				let c = {}
				if(this.cxt && this.cxt.length > 0) {
					c = JSON.parse(this.cxt)
					this.$jr.lc.set("psCxt",this.cxt)
				}
				this.$jr.lc.set("psContent",this.content)
				this.$jr.lc.set("psTo",this.to)
				
				this.$jr.lc.set("type",this.type)
				
				if(this.channel == channel_ps) {
					ps.publishString(this.sendTopic,this.content,this.to,c,true,false,cb)
					.then(rst=>{
						console.log(rst);
						this.sendResult += JSON.stringify(rst) + "\n";
					}).catch(err=>{
					   console.log(err)
					});
				} else {
					let c = ps.itemString(this.sendTopic, this.content, parseInt(this.to), parseInt(this.type))
					c.setDataType(PSData.Constants.FLAG_DATA_STRING);//item的data字段是一个字符串
					console.log(c)
					/*
					if(this.byType=='actId') {
						ps.sendDirectMessage(c, this.to, Constants.PROTOCOL_JSON, Constants.PROTOCOL_JSON)
					}else {
						ps.sendDirectMessageByTopic(c, this.sendTopic, Constants.PROTOCOL_JSON, Constants.PROTOCOL_JSON)
					}
					*/
				   
				   //测试二进制流编码item
				   c = c.encode();
				   if(this.byType=='actId') {
						ps.sendDirectMessage(c, this.to, Constants.PROTOCOL_BIN, Constants.PROTOCOL_BIN)
				   }else {
						ps.sendDirectMessageByTopic(c, this.sendTopic, Constants.PROTOCOL_BIN, Constants.PROTOCOL_BIN)
				   }
				}	
            },

            msgCallback(msg) {
                if(!msg || msg.length == 0) {
                    this.$Message.info("Pubsub topic is disconnected by server")
                    this.doSubscribe();
                } else {
                    //this.result += msg.id + ": "+ msg.data+"\n";
					if(PSData.Constants.FLAG_DATA_BIN == msg.getDataType()) {
						//依赖于接口实现数据编码，服务提供方和使用方需要协商好数据编码和解码方式
						//this.data = r;//由消息接收者读剩余数据
						if(msg.data) {
							//仅用于测试bin数据传输
							let v = msg.data[0]<<24 | msg.data[1]<<16 | msg.data[2]<<8 | msg.data[3]
							this.result += msg.id + ": "+ v +"\n";
						}
					}else if(PSData.Constants.FLAG_DATA_STRING == msg.getDataType()){
						this.result += msg.id + ": "+ msg.data+"\n";
					}else if(PSData.Constants.FLAG_DATA_JSON== msg.getDataType()){
						this.result += msg.id + ": "+ JSON.stringify(msg.data)+"\n";
					} else {
						//extra data
						this.result += msg.id + ": "+ JSON.stringify(msg.data)+"\n";
					}
					
                }
            },

            doSubscribe(){
                if(!this.subTopic || this.subTopic.length == 0) {
                    this.msg = '主题不能为空';
                    return;
                }
                let self = this;
                if(this.subState) {
                    ps.unsubscribe(this.subTopic,this.msgCallback)
                        .then((succ)=>{
                        if(succ==true) {
                            self.subState=false;
                        } else {
                            console.log(succ);
                        }
                    });
                } else {
                    ps.subscribe(this.subTopic,{},this.msgCallback)
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
            self.isLogin = this.$jr.auth.isLogin();
            this.$jr.auth.addActListener(()=>{
                self.isLogin = this.$jr.auth.isLogin();
            });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);
        },

        beforeDestroy () {

        }
    }
</script>

<style>
    .JTestingPubsub{
		display: flex;
        margin-bottom: 20px;
    }

    .publishCon, .subscribeCon{
        height:100%;
        width:50%;
        padding: 10px 0px 10px 10px;
    }

    .publishCon{
		
    }
    .subscribeCon{
		
    }

    .textarea{
        height: 200px;
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