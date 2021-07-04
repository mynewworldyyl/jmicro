<template>
    <div class="JTopicView">

        <div v-if="topic" class="topicTitle">{{topic.topic.title }}</div>

        <div v-if="topic"  class="contentHeader">
            <span style="font-weight:bold">{{topic.topic.createrName}}</span>&nbsp;&nbsp;&nbsp;
            <span>{{topic.topic.createdTime | formatDate(2)}}</span>&nbsp;&nbsp;&nbsp;
            <span>{{topic.topic.topicType | i18n }}</span>&nbsp;&nbsp;&nbsp;
            <a v-if="act && topic.topic.createdBy==act.id" @click="editTopic()">{{'Edit' | i18n }}</a>&nbsp;&nbsp;&nbsp;
        </div>

        <hr/>

        <div  v-if="topic" class="detailTopicContent">
            <div v-html="topic.topic.content"></div>
        </div>

        <hr/>

        <div  class="notesList">
            <div v-if="!topic || !topic.notes" >
                    {{"NoComments" | i18n}}
            </div>
            <div v-if="topic && topic.notes" >
                <div v-for="n in topic.notes" :key="n.id">
                    <div class="noteHeader">
                        <span style="font-weight:bold">{{n.createrName}}</span>&nbsp;&nbsp;&nbsp;
                        <span>{{n.createdTime | formatDate(2)}}</span>&nbsp;&nbsp;&nbsp;
                        <a v-if="act && n.createdBy==act.id" @click="editNote(n)">{{'Edit' | i18n }}</a>&nbsp;&nbsp;&nbsp;
                        <a v-if="act && n.createdBy==act.id" @click="deleteNote(n.id)">{{'Delete' | i18n }}</a>&nbsp;&nbsp;&nbsp;
                    </div>
                    <div class="noteContent">
                        {{n.content}}
                    </div>

                </div>
            </div>
        </div>

        <div class="noteTextBox">
            <div v-if="showInputBox" class="noteInputBox">
                <textarea id="topicTitle" v-model="note.content" style="width:100%;height:100%;"/>
            </div>

            <div style="float:right;width:19%;text-align:right">
                <a id="sendNote" @click="createNote()">
                    <span v-if="showInputBox"> {{ "Send" | i18n }}</span>
                    <span v-if="!showInputBox"> {{ "Comment" | i18n }}</span>
                </a>
                <a v-if="showInputBox" id="cancelNote" @click="cancelNote()">
                    <span> {{ "Cancel" | i18n }}</span>
                </a>
            </div>
        </div>
       <!-- <div v-if="isLogin" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>-->

    </div>
</template>

<script>

    const cid = 'JTopicView';
    const sn = 'cn.jmicro.ext.bbs.api.IBbsService';
    const ns = 'bbs';
    const v = '0.0.1';

    export default {
        name: cid,
        data() {
            return {

                createNoteDialog:false,
                msg:'',
                note : {content:''},
                updateMode:false,

                isLogin:false,
                totalNum:0,
                pageSize:10,
                curPage:1,

                showInputBox:false,
            }
        },

        components: {

        },

        props:{
            topic: {
                type: Object,
                default: null
            },
        },

        methods: {

            editNote(n) {
                this.note = n;
                this.updateMode = true;
                this.showInputBox = true;
                this.createNote();
            },

            deleteNote(noteId) {
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, 'deleteNote',[noteId]).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.info(resp.msg);
                    }else {
                        let idx = -1;
                        for(let i = 0; i < self.topic.notes.length; i++) {
                            let nn = self.topic.notes[i];
                            if(nn.id == noteId) {
                                idx = i;
                                break;
                            }
                        }
                        if(idx > -1) {
                            self.topic.notes.splice(idx,1);
                        }
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            editTopic() {
                window.jm.vue.$emit("editTopic",this.topic.topic);
            },

            cancelNote(){
                this.showInputBox = false;
                this.note.content = "";
            },

            createNote() {
                if(!rpc.isLogin()) {
                    this.$Message.info("未登录！");
                    return;
                }
                if(!this.showInputBox) {
                    this.showInputBox = true;
                    return;
                }

                if(!this.note.content || this.note.content.length == 0) {
                    this.$Message.info("发送内容不能为空");
                    return;
                }

                this.note.topicId = this.topic.topic.id;
                let self = this;

                if(this.updateMode) {
                    let o = {id:this.node.id, content: this.node.content,topicId: this.node.topicId};
                    rpc.callRpcWithParams(sn, ns, v, 'updateNote',[o]).then((resp)=>{
                        if(resp.code == 0) {
                            self.updateMode = false;
                            self.showInputBox = false;
                        }else {
                            self.$Message.info(resp.msg);
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                } else {
                    rpc.callRpcWithParams(sn, ns, v, 'createNote',[this.note]).then((resp)=>{
                        if(resp.code == 0) {
                            if(!self.topic.notes) {
                                self.topic.notes = [];
                            }
                            self.note.content = "";
                            self.topic.notes.unshift(resp.data);
                            self.showInputBox = false;
                        }else {
                            self.$Message.info(resp.msg);
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }

            },

            curPageChange(curPage){
                this.curPage = curPage;
                this.refresh();
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

        },

        mounted () {
            let self = this;
            this.act = rpc.actInfo;
            rpc.addActListener(cid,self.doQuery);
            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }
            window.jm.vue.$on('editorClosed',ec);

        },

        beforeDestroy() {
            rpc.removeActListener(cid);
        },

    }
</script>

<style>

    .JTopicView{
       height:auto;
       padding: 10px;
       position:relative;
    }

    .noteTextBox {
        position: fixed;
        top: 8px;
        right: 20px;
        left: 60%;
        border: none;
    }

    .noteInputBox{
        width:79%;height:50px;display:inline-block;float:left;
    }

    .detailTopicContent{
        padding:10px;
        font-size: large;
    }

    .contentHeader{
        height:15px;line-height: 15px;
        border-bottom: 1px solid lightgray;
        padding-bottom: 18px;
        font-size: smaller;
    }

    .noteHeader{
        height:15px;line-height: 15px;
        font-size: smaller;
    }

    .noteContent{
        min-height: 30px;
        border-bottom: 1px solid lightgray;
        padding: 5px 8px;
        font-size:small;
    }

    .topicTitle{
        text-align: center;
        font-size: x-large;
        font-weight: bold;
    }

    #sendNote,#cancelNote{
        height: 25px;
        width:70px;
        display: inline-block;
        /* float: right; */
        background-color: lightgray;
        text-align: center;
        line-height: 25px;
        cursor: hand;
        border-radius: 5px;
        border: 1px solid #eee;
        border-right-color: #717171;
        border-bottom-color: #717171;
    }

    #sendNote a{
        font-family:Arial;
        font-size:.8em;
        text-align:center;
        margin:3px;/*统一设置所有样式*/


    }
    #sendNote:link, #sendNote:visited{ /*超链接正常状态,被访问过的状态*/
        color:#A62020;
        background-color:#ecd8db;
        text-decoration:none;
        border-top:1px solid #eee; /*边框实现阴影*/
        border-left:1px solid #eee;
        border-bottom:1px solid #717171;
        border-right:1px solid #717171;
    }

    #sendNote:hover{ /*鼠标指针经过时的超链接*/
        color:green; /*改变文字颜色*/
        background-color:#e2c4c9; /*改变背景色*/
        border-top:1px solid #717171; /*边框变化实现按下去的效果*/
        border-left:1px solid #717171;
        border-bottom:1px solid #eee;
        border-right:1px solid #eee;
    }

</style>