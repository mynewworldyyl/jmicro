<template>
    <div class="JTopicListView" style="position:relative;height:auto">

        <div style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td>{{'topicTitle'|i18n}}</td><td>{{'TopicType'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td>
                    <td>{{'Creater'|i18n}}</td>

                    <td v-if="act && act.id==0">{{'ReadNum'|i18n}}</td>
                    <td v-if="act && act.id==0">{{'NoteNum'|i18n}}</td>

                    <td>{{'Operations'|i18n}}</td></tr></thead>
                <tr v-for="c in itemList" :key="c._id">
                    <td>{{c.title}}</td>
                    <td>{{c.topicType | i18n}}</td>
                    <td>{{c.createdTime | formatDate(2)}}</td>
                    <td>{{c.createrName}}</td>
                    <td v-if="act && act.id==0">{{c.readNum}}</td>
                    <td v-if="act && act.id==0">{{c.noteNum}}</td>
                    <td>
                        <a @click="viewTopic(c)">{{'View'|i18n}}</a>&nbsp;&nbsp;
                        <a v-if="act && c.createdBy==act.id" @click="editTopic(c)">{{'Edit'|i18n}}</a>&nbsp;&nbsp;
                        <a v-if="act && c.createdBy==act.id" @click="deleteTopic(c.id)">{{'Delete'|i18n}}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div :style="drawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()"></div>

        <Drawer  v-model="drawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                 :draggable="true" :scrollable="true" width="50">
            <table id="queryTable">
                <tr>
                    <td><i-button @click="doQuery()">{{'Query'|i18n}}</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>

        <Drawer id="topicDrawerViewId"  v-model="detail.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="70">
           <JTopicView :topic="curViewTopic"></JTopicView>
        </Drawer>

        <Modal v-if="isLogin" v-model="createTopicDialog" :closable="false" :loading="true" fullscreen
               class-name="createTopicDialog" @on-ok="doCreateTopic()" ref="createTopicDialog">
                <JCreateTopicView v-if="isLogin && createTopicDialog"  :topic="topic"
                                  @contentChange="contentChange"></JCreateTopicView>
        </Modal>

    </div>
</template>

<script>

    import rpc from "@/rpc/rpcbase"
    import JTopicView from './JTopicView.vue'
    //import JCreateTopicView from "./JCreateTopicView.vue";
    //import { quillEditor } from "vue-quill-editor";

    const cid = 'topicList';
    const sn = 'cn.jmicro.ext.bbs.api.IBbsService';
    const ns = 'bbs';
    const v = '0.0.1';

    export default {
        name: cid,
        components: {
            JCreateTopicView : () => import("./JCreateTopicView.vue"),
            JTopicView,
        },

        data() {
            return {
                createTopicDialog:false,
                topic:{'title':'','content':'',topicType:'other'},
                msg:'',
                updateMode:false,

                isLogin:false,
                act:null,

                itemList: [],
                queryParams:{noLog:"true"},
                totalNum:0,
                pageSize:10,
                curPage:1,

                curViewTopic:null,
                curTopics:{},

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },

                detail: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1000},
                },
            }
        },

        methods: {

            contentChange(topic) {
                this.topic = topic;
            },

            editTopic(topic) {
                if(topic.content && topic.content.length > 0) {
                    this.updateMode = true;
                    this.topic = topic;
                    this.createTopicDialog = true;
                } else {
                    let self = this;
                    rpc.callRpcWithParams(sn, ns, v, 'getTopic',[topic.id]).then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                        }else {
                            self.curTopics[topic.id] = resp.data;
                            topic.content = resp.data.topic.content;
                            self.updateMode = true;
                            self.topic = resp.data.topic;
                            self.createTopicDialog = true;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }

            },

            createTopic() {
                this.updateMode = false;
                this.createTopicDialog = true;
            },

            deleteTopic(topicId) {
                let self = this;
                rpc.callRpcWithParams(sn, ns, v, 'deleteTopic',[topicId]).then((resp)=>{
                    if(resp.code == 0) {
                        self.refresh();
                    }else {
                        self.$Message.info(resp.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            doCreateTopic() {
                let self = this;
                if(!this.topic.title || this.topic.title.length == 0) {
                    self.$Message.info( '主题标题不能为空');
                    return;
                }
                if(!this.topic.content || this.topic.content.length == 0) {
                    self.$Message.info( '主题内容不能为空');
                    return;
                }

                if(!this.topic.topicType || this.topic.topicType.length == 0) {
                    self.$Message.info( '主题类型不能为空');
                    return;
                }

                self.msg = '';

                if(this.updateMode) {
                    let o = {id: this.topic.id,content:this.topic.content,title:this.topic.title,
                        topicType:this.topic.topicType};
                    rpc.callRpcWithParams(sn, ns, v, 'updateTopic',[o]).then((resp)=>{
                        if(resp.code == 0) {
                            self.createTopicDialog = false;
                            self.refresh();
                        }else {
                            self.msg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }else {
                    rpc.callRpcWithParams(sn, ns, v, 'createTopic',[this.topic]).then((resp)=>{
                        if(resp.code == 0) {
                            self.createTopicDialog = false;
                            self.refresh();
                        }else {
                            self.msg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }
            },

            viewTopic(mi) {
                if(this.curTopics[mi.id]) {
                    this.curViewTopic = this.curTopics[mi.id];
                    this.detail.drawerStatus = true;
                    this.detail.drawerBtnStyle.zindex = 10000;
                    this.detail.drawerBtnStyle.right = '0px';
                }else {
                    let self = this;
                    rpc.callRpcWithParams(sn, ns, v, 'getTopic',[mi.id]).then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                        }else {
                            self.curViewTopic = resp.data;
                            self.curTopics[mi.id] = resp.data;
                            mi.content = resp.data.topic.content;
                            self.detail.drawerStatus = true;
                            self.detail.drawerBtnStyle.zindex = 10000;
                            self.detail.drawerBtnStyle.right = '0px';
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

            doQuery() {
                this.isLogin = rpc.isLogin();
                this.act = rpc.actInfo;
                let self = this;
                let params = this.getQueryConditions();
                rpc.callRpcWithParams(sn, ns, v, 'countTopic',[params]).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }else {
                        self.totalNum = resp.data;
                        self.curPage = 1;
                        self.refresh();
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            refresh() {
                let self = this;
                this.isLogin = rpc.isLogin();
                this.act = rpc.actInfo;

                let params = this.getQueryConditions();
                rpc.callRpcWithParams(sn, ns, v, 'topicList', [params,this.pageSize, this.curPage])
                 .then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    let ll = resp.data;
                    self.itemList = ll;

                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            getQueryConditions() {
                let ps = this.queryParams;
                let rst = {};
                for(let k in ps) {
                    rst[k] = ps[k];
                }

                if(rst.startTime) {
                    rst.startTime = new Date(rst.startTime).getTime()+"";
                }

                if(rst.endTime) {
                    rst.endTime = new Date(rst.endTime).getTime()+"";
                }
                if(rst.type) {
                    rst.type = rst.type + "";
                }

                if(rst.level) {
                    rst.level = rst.level + "";
                }

                return rst;
            },

            openDrawer() {
                this.drawer.drawerStatus = true;
                this.drawer.drawerBtnStyle.zindex = 10000;
                this.drawer.drawerBtnStyle.left = '0px';
            },

            onEditorFocus() {

            },

            onEditorBlur() {

            },

            onEditorReady() {
                this.$emit("contentChange",this.topic);
            },

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            let self = this;
            window.jm.vue.$on("editTopic",this.editTopic);
            rpc.addActListener(cid,self.doQuery);
            self.doQuery();

            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
                window.jm.vue.$off('editTopic',this.editTopic);
            }
            window.jm.vue.$on('editorClosed',ec);

            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh},
                        {name:"CreateTopic",label:"CreateTopic",icon:"ios-cog",call:self.createTopic}]
                });

        },

        beforeDestroy() {
            rpc.removeActListener(cid);
            //window.jm.vue.$off('editorClosed',ec);
        },

    }
</script>

<style>
    .JTopicListView{
        min-height: 500px;
    }

    .createTopicDialog{

    }

    #queryTable td {
        padding-left: 8px;
    }

    .drawerJinvokeBtnStatu{
        position: fixed;
        left: 0px;
        top: 30%;
        bottom: 30%;
        height: 39%;
        width: 1px;
        border-left: 1px solid lightgray;
        background-color: lightgray;
        border-radius: 3px;
        z-index: 1000000;
    }

    .configItemTalbe td {
        text-align: center;
    }


    #topicDrawerViewId .ivu-drawer-body{
        padding:0px;
    }

    textarea.ivu-input{
        height:100%;width:100%;
    }

</style>