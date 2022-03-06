<template>
    <div class="JPubsubStatisView" style="position:relative;height:auto">

        <div v-if="isLogin && itemList && itemList.length>0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td>{{'ClientId'|i18n}}</td><td>{{'Topic'|i18n}}</td><td>{{'Date'|i18n}}</td>
                    <td>{{'Send'|i18n}}</td><td>{{'Success'|i18n}}</td><td>{{'Failure'|i18n}}</td></tr></thead>
                <tr v-for="c in itemList" :key="c._id">
                    <td>{{c['clientId']}}</td><td>{{c['topic']}}</td><td>{{c['createdTime'] | formatDate('yyyy/MM/dd')}}</td>
                    <td>{{c['Ms_ReceiveItemCnt']}}</td><td>{{c['Ms_TaskSuccessItemCnt']}}</td><td>{{c['Ms_TopicInvalid']}}</td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && itemList && itemList.length>0" style="position:relative;text-align:center;">
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
                <tr>
                    <td><i-button @click="doQuery()">{{'Query'|i18n}}</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>


    </div>
</template>

<script>

    const cid = 'pubsubStatis';
    import psStatisSrv from "@/rpcservice/psStatisSrv"
    
    export default {
        name: cid,
        data() {
            return {
                isLogin:false,
                itemList: [],
                queryParams:{},
                totalNum:0,
                pageSize:10,
                curPage:1,

                curLogId:-1,
                ds:{},

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },

                selOptions:{
                },

            }
        },

        components: {

        },

        methods: {

            viewDetail(mi) {
                this.curLogId = mi._id;
                this.openDetailDrawer(mi);
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
                this.isLogin = this.$jr.auth.isLogin();
                if(!this.isLogin) {
                    return;
                }
                let self = this;
                let params = this.getQueryConditions();
                psStatisSrv.count(params).then((resp)=>{
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
                this.isLogin = this.$jr.auth.isLogin();
                if(!this.isLogin) {
                    return;
                }
                let params = this.getQueryConditions();
                psStatisSrv.query(params,this.pageSize,this.curPage-1).then((resp)=>{
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

            openDetailDrawer() {
                this.detail.drawerStatus = true;
                this.detail.drawerBtnStyle.zindex = 10000;
                this.detail.drawerBtnStyle.right = '0px';
            },

        },

        mounted () {
            let self = this;
            this.$jr.auth.addActListener(self.doQuery);
            self.doQuery();
            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }
            this.$bus.$on('editorClosed',ec);

            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },

    }
</script>

<style>
    .JPubsubStatisView{
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

</style>