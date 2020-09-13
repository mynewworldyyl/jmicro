<template>
    <div class="JTopicListView" style="position:relative;height:auto">

        <div v-if="isLogin" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td>{{'topicTitle'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td><td>{{'Operations'|i18n}}</td></tr></thead>
                <tr v-for="c in itemList" :key="c._id">
                    <td>{{c.title}}</td>
                    <td>{{c.createdTime | formatDate}}</td>
                    <td><a @click="viewDetail(c)">{{'View'|i18n}}</a></td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin">{{'Notlogin'|i18n}}</div>

        <div v-if="isLogin" :style="drawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()"></div>

        <Drawer v-if="isLogin"  v-model="drawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                 :draggable="true" :scrollable="true" width="50">
            <table id="queryTable">
                <tr>
                    <td><i-button @click="doQuery()">{{'Query'|i18n}}</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>

        <Drawer v-if="isLogin"  v-model="detail.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="90">
            <div v-if="curViewTopic" class="detailTopicTitle">{{curViewTopic.title }}</div>
            <div  v-if="curViewTopic" class="detailTopicContent">
               <div>{{curViewTopic.content }}</div>
            </div>
        </Drawer>

        <Modal v-model="createTopicDialog" :loading="true" width="360" @on-ok="doCreateTopic()" ref="createTopicDialog">
           <div style="color:red;">{{msg}}</div>
            <div>
                <label for="topicTitle">{{'topicTitle'|i18n}}</label>
                <Input id="topicTitle" v-model="topic.title"/>
            </div>

            <div>
                <label for="topicContent">{{'topicContent'|i18n}}</label>&nbsp;&nbsp;&nbsp;
                <Input id="topicContent"  class='textarea' type="textarea" v-model="topic.content"/>
            </div>
        </Modal>

    </div>
</template>

<script>

    const cid = 'JTopicListView';
    const sn = 'cn.jmicro.ext.bbs.api.IBbsService';
    const ns = 'bbs';
    const v = '0.0.1';

    export default {
        name: cid,
        data() {
            return {

                createTopicDialog:false,
                topic:{'title':'','content':''},
                msg:'',

                isLogin:false,
                itemList: [],
                queryParams:{noLog:"true"},
                totalNum:0,
                pageSize:10,
                curPage:0,

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

                selOptions:{},

            }
        },

        components: {

        },

        methods: {

            createTopic() {
                this.createTopicDialog = true;
            },

            doCreateTopic() {
                let self = this;
                if(!this.topic.title || this.topic.title.length == 0) {
                    self.msg = '主题标题不能为空';
                }
                if(!this.topic.content || this.topic.content.length == 0) {
                    self.msg = '主题内容不能为空';
                }

                this.topic.topicType='question';
                self.msg = '';

                window.jm.rpc.callRpcWithParams(sn, ns, v, 'createTopic',[this.topic]).then((resp)=>{
                    if(resp.code == 0) {
                        self.createTopicDialog = false;
                        self.refresh();
                    }else {
                        self.msg = resp.msg;
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            viewDetail(mi) {
                if(this.curTopics[mi.id]) {
                    this.curViewTopic = this.curTopics[mi.id];
                    this.detail.drawerStatus = true;
                    this.detail.drawerBtnStyle.zindex = 10000;
                    this.detail.drawerBtnStyle.right = '0px';
                }else {
                    let self = this;
                    window.jm.rpc.callRpcWithParams(sn, ns, v, 'getTopic',[mi.id]).then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                        }else {
                            self.curViewTopic = resp.data;
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
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                let self = this;
                let params = this.getQueryConditions();
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'countTopic',[params]).then((resp)=>{
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
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                let params = this.getQueryConditions();
                window.jm.rpc.callRpcWithParams(sn, ns, v, 'topicList', [params,this.pageSize, this.curPage])
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

        },

        mounted () {
            let self = this;
            window.jm.rpc.addActListener(cid,self.doQuery);
            self.doQuery();
            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }
            window.jm.vue.$on('editorClosed',ec);

            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh},
                        {name:"CreateTopic",label:"CreateTopic",icon:"ios-cog",call:self.createTopic}]
                });

        },

        beforeDestroy() {
            window.jm.rpc.removeActListener(cid);
            //window.jm.vue.$off('editorClosed',ec);
        },

    }
</script>

<style>
    .JTopicListView{
        min-height: 500px;
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

    .detailTopicTitle{
        text-align: center;
        font-size: larger;
        font-weight: bold;
    }

</style>