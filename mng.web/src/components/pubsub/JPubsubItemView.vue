<template>
    <div class="JPubsubItemView" style="position:relative;height:auto">

        <div v-if="isLogin" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td>{{'ID'|i18n}}</td><td>{{'ClientId'|i18n}}</td><td>{{'Topic'|i18n}}</td>
                    <td>{{'Callback'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td><td>{{'UpdatedTime'|i18n}}</td>
                    <td>{{'Result'|i18n}}</td><td>{{'Data'|i18n}}</td><td>{{'Operations'|i18n}}</td></tr></thead>
                <tr v-for="c in itemList" :key="c._id">
                    <td>{{c.psData.id}}</td><td>{{c.psData.srcClientId}}</td><td>{{c.psData.topic}}</td><td>{{c.psData.callback}}</td>
                    <td>{{c.createdTime | formatDate}}</td><td>{{c.updatedTime | formatDate}}</td>
                    <td>{{c.result}}</td><td>{{c.psData.data}}</td>
                    <td><a @click="resend(c.psData)">{{'Resend'|i18n}}</a></td>
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
                <tr>
                    <td><i-button @click="doQuery()">{{'Query'|i18n}}</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>


    </div>
</template>

<script>

    import ps from "@/rpc/pubsub"
    import rpc from "@/rpc/rpcbase"
    import psDataSrv from "@/rpcservice/psDataSrv"

    const cid = 'JPubsubItemView';

    export default {
        name: cid,
        data() {
            return {
                isLogin:false,
                itemList: [],
                queryParams:{noLog:"true"},
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

            resend(item) {
                let id = item.id;
                item.id = 0;
                ps.publishOneItem(item)
                    .then(rst=>{
                        item.id = id;
                        console.log(rst);
                    }).catch(err=>{
                    console.log(err);
                    item.id = id;
                });
            },

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
                this.isLogin = rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                let self = this;
                let params = this.getQueryConditions();
                psDataSrv.count(params).then((resp)=>{
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
                if(!this.isLogin) {
                    return;
                }
                let params = this.getQueryConditions();
                psDataSrv.query(params,this.pageSize,this.curPage-1).then((resp)=>{
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
            rpc.addActListener(cid,self.doQuery);
            self.doQuery();
            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }
            window.jm.vue.$on('editorClosed',ec);

            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

        },

        beforeDestroy() {
            rpc.removeActListener(cid);
            //window.jm.vue.$off('editorClosed',ec);
        },

        filters: {
            formatDate: function(time) {
                // 后期自己格式化
                return new Date(time).format("yyyy/MM/dd hh:mm:ss S") //Utility.formatDate(date, 'yyyy/MM/dd')
            }
        },

    }
</script>

<style>
    .JPubsubItemView{
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