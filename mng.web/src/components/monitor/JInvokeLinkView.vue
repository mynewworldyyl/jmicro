<template>
    <div class="JInvokeLinkView" style="position:relative;height:auto">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <treeTable ref="recTree"
                       :list.sync="logList"
                       @callMethod="callMethod"
                       @viewDetail="viewDetail"
                       @orderByCost="orderByCost">
            </treeTable>
        </div>

        <div v-if="!isLogin" > Not login</div>
        <div v-if="isLogin && (!logList || logList.length == 0)" >No data</div>

        <div v-if="isLogin && logList && logList.length > 0 "  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage" show-elevator show-sizer show-total
                  @on-change="curPageChange" @on-page-size-change="pageSizeChange"  :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="isLogin"  :style="drawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="drawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                 :draggable="true" :scrollable="true" width="50">
            <table id="queryTable">
                <tr>
                    <td>START TIME</td>
                    <td>
                        <DatePicker v-model="queryParams.startTime" placeholder="Start Time"
                                     format="yyyy-MM-dd hh:mm:ss"  type="datetime"
                                 ></DatePicker >
                     </td>
                    <td>END TIME</td>
                    <td>
                        <DatePicker v-model="queryParams.endTime" placeholder="End Time"
                                    format="yyyy-MM-dd hh:mm:ss"  type="datetime"
                        ></DatePicker >
                    </td>
                </tr>

                <tr>
                    <td>LINK ID</td><td> <Input v-model="queryParams.linkId"/></td>
                    <td>REQ ID</td><td> <Input v-model="queryParams.reqId"/></td>
                </tr>

                <tr>
                    <td>PARENT ID</td><td> <Input v-model="queryParams.reqParentId"/></td>
                    <td>ACT</td>
                   <td>
                       <Select :filterable="true"
                                :allowCreate="true" ref="actSelect" :label-in-value="true" v-model="queryParams.act">
                           <Option value="">none</Option>
                           <Option :value="v" v-for="v in selOptions.act" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>LOG LEVEL</td>
                    <td>
                        <Select :filterable="true" ref="levelSelect" :label-in-value="true" v-model="queryParams.level">
                            <Option value="" >none</Option>
                            <Option :value="v" v-for="(v,k) in selOptions.level" v-bind:key="k">{{k}}</Option>
                        </Select>
                    </td>
                    <td>TYPE</td>
                    <td>
                        <Select :filterable="true"
                                         :allowCreate="true" ref="typeSelect" :label-in-value="true" v-model="queryParams.type">
                        <Option value="" >none</Option>
                        <Option :value="v" v-for="(v,k) in selOptions.type" v-bind:key="k">{{k}}</Option>
                    </Select>
                    </td>
                </tr>

                <tr><td>REMOTE HOST</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="remoteHostSelect"  :label-in-value="true" v-model="queryParams.remoteHost">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.remoteHost" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>REMOTE PORT</td><td> <Input  v-model="queryParams.remotePort"/></td>
                </tr>

                <tr>
                    <td>LOCAL HOST</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="localHostSelect"  :label-in-value="true" v-model="queryParams.localHost">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.localHost" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>INSTANCE NAME</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="instanceNameSelect"  :label-in-value="true"
                                v-model="queryParams.instanceName">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.instanceName" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr><td>SERVICE NAME</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="serviceNameSelect"  :label-in-value="true"
                                v-model="queryParams.serviceName">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.serviceName" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>NAMESPACE</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="namespaceSelect"  :label-in-value="true"
                                v-model="queryParams.namespace">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.namespace" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>VERSION</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="versionSelect"  :label-in-value="true"
                                v-model="queryParams.version">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.version" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>METHOD</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="methodSelect"  :label-in-value="true"
                                v-model="queryParams.method">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.method" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>IMPL CLASS</td><td> <Input  v-model="queryParams.implCls"/></td>
                    <td>PROVIDER</td>
                    <td>
                        <Select v-model="queryParams.provider">
                            <Option value="">none</Option>
                            <Option value="true">true</Option>
                            <Option value="false">false</Option>
                        </Select>
                    </td>
                </tr>
                <tr>
                    <td>SUCCESS</td>
                    <td>
                        <Select v-model="queryParams.success">
                            <Option value="">none</Option>
                            <Option value="true">true</Option>
                            <Option value="false">false</Option>
                        </Select>
                    </td>
                    <td></td><td></td>
                </tr>

                <tr>
                    <td><i-button @click="doQuery()">QUERY</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>

      <!--  <div :style="detail.drawerBtnStyle" class="detailJinvokeBtnStatu" @mouseenter="openDetailDrawer()"></div>-->

        <Drawer v-if="isLogin"  v-model="detail.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="90">
            <JLinkDetailView :linkId="curDetailLinkId"></JLinkDetailView>
        </Drawer>

    </div>
</template>

<script>

    import treeTable from '../treetable/LinkLogTreeTable.vue'
    import JLinkDetailView from './JLinkDetailView.vue'

    import logSrv from "@/rpcservice/logSrv"
    
    const cid = 'invokeLinkView';

    export default {
        name: cid,
        data() {
            return {
                isLogin:false,
                list: [], // 请求原始数据
                logList: [],
                queryParams:{},
                totalNum:0,
                pageSize:10,
                curPage:1,

                linkIdSort:1,
                createTimeSort:0,
                costSort:0,

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },
                detail: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1000},
                },
                selOptions:{
                },

                curDetailLinkId:-1,
            }
        },

        components: {
            treeTable,
            JLinkDetailView,
        },

        methods: {
            callMethod(mi) {
              console.log(mi);
            },

            viewDetail(mi) {
                this.curDetailLinkId = mi.item.linkId;
                this.openDetailDrawer(mi);
            },

            orderByCost(newVal) {
                this.params['costSort'] = newVal;
                this.doQuery();
            },

            curPageChange(curPage){
                this.curPage = curPage;
                this.doQuery();
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.doQuery();
            },

            refresh() {
                let self = this;
                let params = this.getQueryConditions();
                this.$bus.$emit("refreshLinkItemList");
                this.logList = [];

                logSrv.count(params).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    } else {
                        self.totalNum = resp.data;
                        self.curPage = 1;
                        self.doQuery();
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            doQuery() {
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                if(!this.isLogin) {
                    return;
                }
                let params = this.getQueryConditions();
                logSrv.query(params,this.pageSize,this.curPage-1).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    self.logList = resp.data;
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

            createGroup(val,key) {
                let groups = this.selOptions[key];
                if(!groups) {
                    groups = this.selOptions[key] = [];
                }
                if(val && val.trim() != '') {
                    val = val.trim();
                    for(let i =0; i < groups.length; i++) {
                        if(val == this.groups[i]) {
                            return;
                        }
                    }
                    this.groups.push(val);
                }
            },

            refreshDict() {
                let self = this;
                self.isLogin = this.$jr.auth.isLogin();
                if(!this.isLogin) {
                    self.selOptions = [];
                    return;
                }
                logSrv.queryDict().then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    self.selOptions = resp.data;
                    self.refresh();
                }).catch((err)=>{
                    window.console.log(err);
                });
            },
        },

        mounted () {
            let self = this;
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(cid,this.refreshDict);

            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refreshDict}
                    ]
                });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            self.refresh();
            this.$bus.$on('editorClosed',ec);

        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },

    }
</script>

<style>
    .JInvokeLinkView{
        min-height: 500px;
    }

    #queryTable{

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

    .detailJinvokeBtnStatu{
        position: fixed;
        right: 0px;
        top: 30%;
        bottom: 30%;
        height: 39%;
        width: 1px;
        border-right: 1px solid lightgray;
        background-color: lightgray;
        border-radius: 3px;
        z-index: 1000000;
    }

</style>