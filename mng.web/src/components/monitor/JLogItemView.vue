<template>
    <div class="JLogItemView" style="position:relative;height:auto">

        <div v-if="isLogin" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead><tr><td style="width:300px">TAG</td><td  style="width:165px">TIME</td><td style="width:135px">LEVEL</td>
                    <td  style="width:130px">TYPE</td><td  style="width:38px">TYPE</td><td  style="width:38px">TYPE</td>
                    <td>LOG</td></tr></thead>
                <tr v-for="c in logList" :key="c._id">
                    <td>{{c.tag}}</td><td>{{c.time | formatDate}}</td><td>{{c.levelLabel}}</td>
                    <td>{{c.typeLabel}}</td><td>{{c.val}}</td><td>{{c.num}}</td><td style="text-align:left;">{{c.desc}}</td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin" >Not login</div>

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
                    <td>TAG(*)</td>
                    <td>
                        <Input  v-model="queryParams.tag"/>
                    </td>
                    <td>DESC(*)</td>
                    <td>
                        <Input  v-model="queryParams.desc"/>
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
                    <td>EXCLUDE NOLOG</td>
                    <td>
                        <Select v-model="queryParams.noLog">
                            <Option value="false">false</Option>
                            <Option value="true">true</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td><i-button @click="doQuery()">QUERY</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>


    </div>
</template>

<script>

    const cid = 'JLogItemView';

    export default {
        name: cid,
        data() {
            return {
                isLogin:false,
                logList: [],
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
                let self = this;
                let params = this.getQueryConditions();
                window.jm.mng.logSrv.countLog(params).then((resp)=>{
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
                window.jm.mng.logSrv.queryLog(params,this.pageSize,this.curPage-1).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    let ll = resp.data;
                    if(ll && ll.length > 0) {
                        ll.map(e => {
                            e.typeLabel = self.ds['mtKey2Val'][e.type];
                            e.levelLabel = self.ds['logKey2Val'][e.level];
                        });
                    }

                    self.logList = ll;

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

            q() {
                let self = this;
                self.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                window.jm.mng.comm.getDicts(['logKey2Val','mtKey2Val']).then((dicts)=>{
                    if(dicts) {
                        for(let k in dicts) {
                            let k2v = dicts[k];
                            let v2k = {};
                            self.ds[k] = v2k;
                            for(let kk in k2v) {
                                v2k[k2v[kk]] = kk;
                            }
                        }
                    }
                }).catch((err)=>{
                    throw err;
                });

                window.jm.mng.logSrv.queryDict().then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    self.selOptions = resp.data;
                    window.jm.mng.act.addListener(cid,this.refresh);
                    self.doQuery();

                }).catch((err)=>{
                    window.console.log(err);
                });
            }

        },

        mounted () {
            let self = this;
            window.jm.rpc.addActListener(cid,self.q);
            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }
            window.jm.vue.$on('editorClosed',ec);



        },

        beforeDestroy() {
            window.jm.rpc.removeActListener(cid);
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
    .JLogItemView{
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