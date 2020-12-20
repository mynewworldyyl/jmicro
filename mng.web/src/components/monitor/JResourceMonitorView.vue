<template>
    <div class="JResourceMonitorView" style="position:relative;height:auto">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
        </div>

        <div v-if="isLogin  && logList && logList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[60,100,150,200,500]"></Page>
        </div>

        <div v-if="!msg">{{msg}}</div>

        <div v-if="isLogin"  :style="drawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="drawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                :draggable="true" :scrollable="true" width="55">
            <table id="queryTable">
                <tr>
                    <td>{{'toType' | i18n}}</td>
                    <td>
                        <Select id="toType" v-model="queryParams.toType">
                            <Option v-for="(key,v) in toTypes" :value="v" :key="key">{{key | i18n}}</Option>
                        </Select>
                    </td>

                    <td>{{'resourceName'|i18n('Resource Name')}}</td>
                    <td>
                        <Select v-if="selOptions.resourceNames" :filterable="true"
                                :allowCreate="true" :label-in-value="true" v-model="queryParams.resName">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="v in selOptions.resourceNames" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>
                <tr>
                    <td>{{'instanceName'|i18n('Instance Name')}}</td>
                    <td>
                        <Select v-if="selOptions.allInstances" :filterable="true"
                                :allowCreate="true" ref="instanceNameSelect"  :label-in-value="true"
                                v-model="queryParams.insName">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.allInstances" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>

                    <td>{{'HostName'|i18n('Host Name')}}</td>
                    <td>
                        <Input v-model="queryParams.host"/>
                    </td>

                </tr>
                <tr>
                    <td>{{'startTime'|i18n('START TIME')}}</td>
                    <td>
                        <DatePicker v-model="queryParams.startTime" placeholder="Start Time"
                                    format="yyyy-MM-dd hh:mm:ss"  type="datetime"></DatePicker >
                    </td>
                    <td>{{'endTime'|i18n('END TIME')}}</td>
                    <td>
                        <DatePicker v-model="queryParams.endTime" placeholder="End Time"
                                    format="yyyy-MM-dd hh:mm:ss"  type="datetime"
                        ></DatePicker >
                    </td>
                </tr>
                <tr>
                    <td>{{'tag'|i18n('Tag')}}</td>
                    <td> <Input v-model="queryParams.tag"/></td>

                    <td>{{'configId'|i18n('Config ID')}}</td>
                    <td> <Input v-model="queryParams.configId"/></td>
                </tr>

                <tr>
                    <td>{{"groupBy"|i18n}}</td>
                    <td>
                        <RadioGroup v-model="queryParams.groupBy">
                            <Radio label="ins" true-value="ins" false-value="">{{'Instance'|i18n}}</Radio>
                            <Radio label="res"  true-value="res" false-value="">{{'Resource'|i18n}}</Radio>
                        </RadioGroup>
                    </td>

                    <td><i-button @click="refresh()">{{'Query'|i18n}}</i-button></td>
                    <td></td>
                </tr>
            </table>
        </Drawer>

    </div>
</template>

<script>

    const cid = 'resourceMonitorView';

    const sn = 'cn.jmicro.resource.IMngResourceService';
    const ns = 'mng';
    const v = '0.0.1';

    export default {
        name: cid,
        data() {
            return {
                msg:'',
                isLogin:false,
                logList: [],

                queryParams:{noLog:"true",groupBy:'ins',toType:8},
                totalNum:0,
                pageSize:60,
                curPage:1,

                curLogId:-1,

                toTypes:{8:'Direct',1:'Db', 4:'File',5:'Log',6:'Message'},

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

           /* viewDetail(mi) {
                this.curLogId = mi._id;
                this.openDetailDrawer(mi);
            },*/

            curPageChange(curPage){
                this.curPage = curPage;
                this.refresh();
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

            refresh() {
                let self = this;
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                self.msg = '';
                let params = this.getQueryConditions();
                window.jm.rpc.callRpcWithParams(sn,ns,v, 'getInstanceResourceData', [params])
                    .then((resp)=>{
                        window.console.log(resp);
                    }).catch((err)=>{
                    self.msg = err || '';
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

                return rst;
            },

            openDrawer() {
                this.drawer.drawerStatus = true;
                this.drawer.drawerBtnStyle.zindex = 10000;
                this.drawer.drawerBtnStyle.left = '0px';
            },

            getDicts() {
                let self = this;
                window.jm.mng.comm.getDicts([
                    window.jm.rpc.Constants.MONITOR_RESOURCE_NAMES,
                    window.jm.rpc.Constants.ALL_INSTANCES,],'')
                    .then((opts)=>{
                        if(opts) {
                            self.selOptions = opts;
                        }
                    }).catch((err)=>{
                    throw err;
                });
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            window.jm.rpc.addActListener(cid,this.q);
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.q}]
                });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

            self.getDicts();
            self.refresh();

        },

        beforeDestroy() {
            window.jm.rpc.removeActListener(cid);
        },

        filters: {

        },

    }
</script>

<style>
    .JResourceMonitorView{

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

</style>