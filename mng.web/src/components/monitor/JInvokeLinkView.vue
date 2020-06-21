<template>
    <div class="JInvokeLinkView">
        <treeTable ref="recTree"
                   :list.sync="logList"
                   @callMethod="callMethod"
                   @viewDetail="viewDetail"
        ></treeTable>

        <div :style="drawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()"></div>

        <Drawer  v-model="drawer.drawerStatus" :closable="false" placement="left" :transfer="true"
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
                            <Option :value="v" v-for="v in selOptions.act" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>LOG LEVEL</td>
                    <td>
                        <Select :filterable="true" ref="levelSelect" :label-in-value="true" v-model="queryParams.level">
                            <Option value="" >NONE</Option>
                            <Option :value="v" v-for="(v,k) in selOptions.level" v-bind:key="k">{{k}}</Option>
                        </Select>
                    </td>
                    <td>TYPE</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="typeSelect" :label-in-value="true" v-model="queryParams.type">
                            <Option value="" >NONE</Option>
                            <Option :value="v" v-for="(v,k) in selOptions.type" v-bind:key="k">{{k}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr><td>REMOTE HOST</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="remoteHostSelect"  :label-in-value="true" v-model="queryParams.remoteHost">
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
                            <Option :value="v" v-for="(v) in selOptions.localHost" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>INSTANCE NAME</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="instanceNameSelect"  :label-in-value="true"
                                v-model="queryParams.instanceName">
                            <Option :value="v" v-for="(v) in selOptions.instanceName" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr><td>SERVICE NAME</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="serviceNameSelect"  :label-in-value="true"
                                v-model="queryParams.serviceName">
                            <Option :value="v" v-for="(v) in selOptions.serviceName" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>NAMESPACE</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="namespaceSelect"  :label-in-value="true"
                                v-model="queryParams.namespace">
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
                            <Option :value="v" v-for="(v) in selOptions.version" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>METHOD</td>
                    <td>
                        <Select :filterable="true"
                                :allowCreate="true" ref="methodSelect"  :label-in-value="true"
                                v-model="queryParams.method">
                            <Option :value="v" v-for="(v) in selOptions.method" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>IMPL CLASS</td><td> <Input  v-model="queryParams.implCls"/></td>
                    <td>PROVIDER</td>
                    <td>
                        <Select v-model="queryParams.provider">
                            <Option value="true">true</Option>
                            <Option value="false">false</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td><i-button @click="refresh">QUERY</i-button></td><td></td>
                </tr>
            </table>
        </Drawer>

        <div :style="detail.drawerBtnStyle" class="detailJinvokeBtnStatu" @mouseenter="openDetailDrawer()"></div>

        <Drawer  v-model="detail.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="95">
            <JLinkDetailView :linkId="curDetailLinkId"></JLinkDetailView>
        </Drawer>

    </div>
</template>

<script>

    import treeTable from '../treetable/TreeTable.vue'
    import JLinkDetailView from './JLinkDetailView.vue'

    const cid = 'JInvokeLinkView';

    export default {
        name: cid,
        data() {
            return {
                adminPer:false,
                list: [], // 请求原始数据
                logList: [],
                queryParams:{},
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

            refresh() {
                let self = this;
                this.adminPer = window.jm.mng.comm.adminPer;
                let params = this.getQueryConditions();
                window.jm.mng.logSrv.query(params,100,0).then((resp)=>{
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
            }
        },

        mounted () {
            let self = this;
            window.jm.mng.logSrv.queryDict().then((resp)=>{
                if(resp.code != 0) {
                    self.$Message.success(resp.msg);
                    return;
                }
                self.selOptions = resp.data;
                window.jm.mng.act.addListener(cid,this.refresh);
                this.refresh();

            }).catch((err)=>{
                window.console.log(err);
            });

        },

        beforeDestroy() {
            window.jm.mng.act.removeListener(cid);
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