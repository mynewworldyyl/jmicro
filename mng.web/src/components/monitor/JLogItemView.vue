<template>
    <div class="JLogItemView" style="position:relative;height:auto">

        <div v-if="isLogin && logList && logList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="logConfigItemTalbe" border="0" width="99%">
               <!-- <thead><tr><td style="width:300px">TAG</td><td  style="width:165px">TIME</td><td style="width:135px">LEVEL</td>
                    <td  style="width:130px">TYPE</td><td  style="width:38px">TYPE</td><td  style="width:38px">TYPE</td>
                    <td>LOG</td></tr></thead>
                <tr v-for="c in logList" :key="c._id">
                    <td>{{c.tag}}</td><td>{{c.time | formatDate}}</td><td>{{c.levelLabel}}</td>
                    <td>{{c.typeLabel}}</td><td>{{c.val}}</td><td>{{c.num}}</td><td style="text-align:left;">{{c.desc}}</td>
                </tr>-->

                <tr v-for="c in logList" :key="c._id">
                    <td style="padding:5px 3px"><p v-html="toLog(c)"></p></td>
                </tr>

            </table>
        </div>

        <div v-if="isLogin  && logList && logList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[60,100,150,200,500]"></Page>
        </div>

        <div v-if="!isLogin" >Not login</div>

        <div v-if="isLogin  && (!logList || logList.length == 0)" >No data</div>

        <div v-if="isLogin"  :style="drawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="drawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                 :draggable="true" :scrollable="true" width="50">
            <table id="queryTable">
                <tr>
                    <td>START TIME</td>
                    <td>
                        <DatePicker v-model="queryParams.startTime" placeholder="Start Time"
                                     format="yyyy-MM-dd HH:mm:ss"  type="datetime"
                                 ></DatePicker >
                     </td>
                    <td>END TIME</td>
                    <td>
                        <DatePicker v-model="queryParams.endTime" placeholder="End Time"
                                    format="yyyy-MM-dd HH:mm:ss"  type="datetime"
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
                        <Select v-if="selOptions.level" :filterable="true" ref="levelSelect" :label-in-value="true" v-model="queryParams.level">
                            <Option value="" >none</Option>
                            <Option :value="v" v-for="(v,k) in selOptions.level" v-bind:key="k">{{k}}</Option>
                        </Select>
                    </td>
                    <td>Operator</td>
                    <td>
                        <Select v-model="queryParams.op">
                            <Option value="=">=</Option>
                            <Option value=">=">>=</Option>
                            <Option value=">" ></Option>
                        </Select>
                    </td>
                </tr>

                <tr><td>REMOTE HOST</td>
                    <td>
                        <Select v-if="selOptions.remoteHost" :filterable="true"
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
                        <Select v-if="selOptions.localHost" :filterable="true"
                                :allowCreate="true" ref="localHostSelect"  :label-in-value="true" v-model="queryParams.localHost">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.localHost" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>INSTANCE NAME</td>
                    <td>
                        <Select v-if="selOptions.instanceName" :filterable="true"
                                :allowCreate="true" ref="instanceNameSelect"  :label-in-value="true"
                                v-model="queryParams.instanceName">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.instanceName" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr><td>SERVICE NAME</td>
                    <td>
                        <Select  :filterable="true"
                                :allowCreate="true" ref="serviceNameSelect"  :label-in-value="true"
                                v-model="queryParams.serviceName">
                            <Option value="">none</Option>
                            <Option :value="v" v-for="(v) in selOptions.serviceName" v-bind:key="v">{{v}}</Option>
                        </Select>
                    </td>
                    <td>NAMESPACE</td>
                    <td>
                        <Select  :filterable="true"
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
                        <Select v-if="selOptions.version" :filterable="true"
                                :allowCreate="true" ref="versionSelect"  :label-in-value="true"
                                v-model="queryParams.version">
                            <Option value="">none</Option>

                            <Option :value="v" v-for="(v) in selOptions.version" v-bind:key="v">{{v}}</Option>

                        </Select>
                    </td>
                    <td>METHOD</td>
                    <td>
                        <Select v-if="selOptions.method" :filterable="true"
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
                    <td>Show Type</td>
                    <td>
                        <Select v-model="showType">
                            <Option value="1">{{"Flat"|i18n}}</Option>
                            <Option value="2">{{"Group"|i18n}}</Option>
                        </Select>
                        <!--<Select v-model="queryParams.noLog">
                            <Option value="false">false</Option>
                            <Option value="true">true</Option>
                        </Select>-->
                    </td>
                </tr>
                <tr>
                    <td>ConfigId</td>
                    <td>
                        <Input  v-model="queryParams.configId"/>
                    </td>
                    <td>Config Tag</td>
                    <td>
                        <Input  v-model="queryParams.configTag"/>
                    </td>
                </tr>
                <!--<tr>
                    <td> <Select v-if="selOptions.type" :filterable="true"
                                 :allowCreate="true" ref="typeSelect" :label-in-value="true" v-model="queryParams.type">
                        <Option value="" >none</Option>
                        <Option :value="v" v-for="(v,k) in selOptions.type" v-bind:key="k">{{k}}</Option>
                    </Select></td>
                    <td></td>
                </tr>-->

                <tr>
                    <td> </td>
                    <td><i-button @click="doQuery()">QUERY</i-button></td>
                </tr>
            </table>
        </Drawer>


    </div>
</template>

<script>

    const cid = 'logItemView';

    const LOG2LEVEL = window.jm.mng.LOG2LEVEL;

    const LEVEL2COLOR = {2:'debugTag', 5:'errorTag', 6:'finalTag', 3:'infoTag', 0:'noLogTag',
        1: 'tranceTag', 4:'warnTag'};

    export default {
        name: cid,
        data() {
            return {
                isLogin:false,
                logList: [],
                queryParams:{noLog:"true",op:"="},
                totalNum:0,
                pageSize:60,
                curPage:1,

                showType:"1",

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

            toGroupLogView(c){
                let v = '';
                if(c.cfgTag) {
                    v = v +  "TAG:" + c.cfgTag + ', '
                }

                if(c.instanceName) {
                    v = v +  "INS:" + c.instanceName + ', '
                }

                if(c.createTime > 0) {
                    let pv = new Date(c.createTime).format('yyyy/MM/dd hh:mm:ss S');
                    v = v + "CT:" + pv + ', '
                }

                if(c.smKey) {
                    v +='MK[' + c.smKey.usk.serviceName+"##"+c.smKey.usk.namespace + '##'+ c.smKey.usk.version;
                    v = v +'##'+c.smKey.usk.instanceName+'##'+c.smKey.usk.host+'##'+c.smKey.usk.port+'##' + c.smKey.method + '##' + c.smKey.paramsStr;
                    v+='] '
                }

                if(c.req){
                    v += 'REQ[' + c.req.serviceName+"##"+c.req.namespace + '##'+ c.req.version;
                    v = v +'##'+c.req.method ;
                    if(c.req.args && c.req.args.length > 0) {
                        v += '##' + JSON.stringify(c.req.args)+", ";
                    }
                    v += '] ';
                }

                if(c.resp){
                    v += 'RESP['
                    v += ', respId:'+ c.resp.id;
                    v += ', reqId:'+ c.resp.reqId
                    v += ', result:'+ JSON.stringify(c.resp.result)
                    v += ', success:'+ c.resp.success + '] '
                }

                for(let a in c) {
                    if(a != '_id' && a != 'items' && c[a] && c[a] != -1 && a != 'cfgTag' && a != 'createTime'
                        && a != 'inputTime' && a != 'instanceName' && a != 'smKey' && a != 'req' && a != 'resp') {
                        v = v + a + ":" + c[a] + ', '
                    }
                }

                v = '<span style="font-weight: bold">' + v + '</span>'

                if(c.items && c.items.length > 0 ) {
                    for(let i = 0; i < c.items.length; i++) {
                        let l = c.items[i];
                        let lv = '<span class="' + LEVEL2COLOR[l.level]+'">' + LOG2LEVEL[l.level] + '</span> : ';
                        lv += l.tag  + " : ";

                        if(l.lineNo) {
                            lv += l.lineNo  +" : "
                        }
                        lv += new Date(l.time).format('yyyy/MM/dd hh:mm:ss S')+" : ";
                        lv += l.desc;

                        if( l.ex ) {
                            lv += " <pre>"+l.ex+"</pre>" ;
                        }

                        v += '<br/>' + lv;
                    }
                }
                return v;
            },

            toFlatLogView(c){

                let v = '<span class="' + LEVEL2COLOR[c.items.level]+'">' + LOG2LEVEL[c.items.level] + '</span> : ';

                if(c.items.lineNo) {
                    v += c.items.lineNo  +" : "
                }

                if(c.instanceName) {
                    v = v +  c.instanceName + ':'
                }

                v += new Date(c.items.time).format('yyyy/MM/dd hh:mm:ss S')+" : ";

                v = v  + c.items.tag+":"

                if(c.tag) {
                    v = v + c.tag + ':'
                }

                if(c.smKey) {
                    v +='MK[' + c.smKey.usk.serviceName+"##"+c.smKey.usk.namespace + '##'+ c.smKey.usk.version;
                    v = v +'##'+c.smKey.usk.instanceName+'##'+c.smKey.usk.host+'##'+c.smKey.usk.port+'##' + c.smKey.method + '##' + c.smKey.paramsStr;
                    v+='] '
                }

                if(c.linkId) {
                    v = v + c.linkId + ':'
                }

                /*if(c.req){
                    v += 'REQ[' + c.req.serviceName+"##"+c.req.namespace + '##'+ c.req.version;
                    v = v +'##'+c.req.method ;
                    if(c.req.args && c.req.args.length > 0) {
                        v += '##' + JSON.stringify(c.req.args)+", ";
                    }
                    v += '] ';
                }*/

                /*if(c.resp){
                    v += 'RESP['
                    v += ', respId:'+ c.resp.id;
                    v += ', reqId:'+ c.resp.reqId
                    v += ', result:'+ JSON.stringify(c.resp.result)
                    v += ', success:'+ c.resp.success + '] '
                }*/

                v = '<span style="font-weight: bold">' + v + '</span>'

                let l = c.items;

                let lv = "";

                lv += l.desc;

                if( l.ex ) {
                    lv += " <pre>"+l.ex+"</pre>" ;
                }

                v += '<br/>' + lv;

                return v;
            },

            toLog(c){
                if(this.showType=='1') {
                    return this.toFlatLogView(c);
                } else {
                    return this.toGroupLogView(c);
                }
            },

            doQuery() {
                let self = this;
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    return;
                }
                self.logList = [];
                let params = this.getQueryConditions();
                window.jm.mng.logSrv.countLog(self.showType,params).then((resp)=>{
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

                self.logList = [];
                if(this.showType == '2') {
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
                } else {
                    window.jm.rpc.callRpcWithParams(window.jm.mng.logSrv.sn,
                        window.jm.mng.logSrv.ns, window.jm.mng.logSrv.v, 'queryFlatLog',
                        [params,this.pageSize,this.curPage-1])
                    .then((resp)=>{
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
                }
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
                window.jm.mng.comm.getDicts(['logKey2Val','mtKey2Val'],'').then((dicts)=>{
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
                    self.doQuery();
                }).catch((err)=>{
                    window.console.log(err);
                });
            }

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

            self.q();
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
        border:none;
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

    .logConfigItemTalbe td {
        text-align: left;
        border:none;
    }

    .debugTag{
        background-color: mediumspringgreen;
        color:crimson;
    }

    .errorTag{
        background-color: red;
        color: aliceblue;
    }

    .noLogTag {
        background-color: red;
        color:black;
    }

    .finalTag {
        background-color: darkred;
        color: aliceblue;
    }

    .tranceTag {
        background-color: palegreen;
        color:crimson;
    }

    .warnTag {
        background-color: yellow;
        color:crimson;
    }

    .infoTag {
        background-color: blue;
        color: aliceblue;
    }

    .errorTag,.warnTag,.tranceTag,.finalTag,.noLogTag,.debugTag, .infoTag{
        border-radius: 3px;
    }

</style>