<template>
    <div class="JI18nConfig">

        <div v-if="isLogin && list && list.length> 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
                    <tr>
                        <td>{{'clientId' | i18n }}</td>
                        <td>{{'mod' | i18n }}</td>
                        <td>{{'key' | i18n }}</td>
                        <td>{{'val' | i18n }}</td>
                        <td>{{'country' | i18n }}</td>
                        <td>{{'lan' | i18n }}</td>
                        <td>{{'desc' | i18n }}</td>
                        <!--<td>{{'createdTime' | i18n }}</td>
                        <td>{{'updatedTime' | i18n }}</td>-->
                        <td>{{'Operation' | i18n }}</td>
                    </tr>
                </thead>

                <tr v-for="c in list" :key="c.id">
                    <td>{{c.clientId}}</td>
                    <td>{{c.mod}}</td>
                    <td>{{c.key}}</td>
                    <td>{{c.val}}</td>
                    <td>{{c.country}}</td>
                    <td>{{c.lan}}</td>
                    <td>{{c.desc}}</td>
                  <!--  <td>{{c.createdTime | formatDate(1)}}</td>
                    <td>{{c.updatedTime | formatDate(1)}}</td>-->
                    <td>
                        <a v-if="isLogin" @click="update(c)">{{'Update' | i18n }}</a>&nbsp;&nbsp;&nbsp;&nbsp;
                        <a v-if="isLogin" @click="deleteItem(c.id)">{{'Delete' | i18n }}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && list && list.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="qry.pageSize" :current="qry.curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin" >{{msg}}</div>

        <div v-if="isLogin  && (!list || list.length == 0)" >{{msg}}</div>

        <!--  创建 或 更新 -->
        <Drawer  v-model="addDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50" :mask-closable="false">
            <div class="error">{{errMsg}}</div>
            <div class="addClientcls">
				<a @click="addDrawer.drawerStatus=false">{{'Close'|i18n}}</a>
                <a v-if="actInfo  && !importFileModel" @click="doAdd()">{{'Confirm'|i18n}}</a>
                <a v-if="actInfo && importFileModel" @click="doImport()">{{'DoImport'|i18n}}</a>
            </div>
            <div>

				<!--  文件导入 -->
				<JFileUpload v-if="importFileModel" ref="importFile" mcode="1168774889"
							 @finish="uploadFinish()"></JFileUpload>

				<Label v-if="!importFileModel"  for="key">{{'key'|i18n}}</Label>
				<Input  v-if="!importFileModel"   :disabled="updateModel"  id="key" v-model="ic.key"/>

				<Label  v-if="!importFileModel" for="val">{{'val'|i18n}}</Label>
				<Input  v-if="!importFileModel" id="val" v-model="ic.val"/>

				<Label for="desc">{{'desc'|i18n}}</Label>
				<Input  id="desc" v-model="ic.desc"/>

				<Label v-if="actInfo && actInfo.admin"  for="clientId">{{'clientId'|i18n}}</Label>
				<Input v-if="actInfo && actInfo.admin"   id="clientId" v-model="ic.clientId"/>

				<Label  for="mod">{{'mod'|i18n}}</Label>
				<Input  id="mod" v-model="ic.mod"/>

				<Label for="country">{{'country'|i18n}}</Label>
			   <!-- <Input id="country" v-model="ic.country"/>-->
				<Select :disabled="updateModel" id="country" :filterable="false" ref="country" v-model="ic.country">
					<Option v-for="c in conList" :key="c.l" :value="c.l" >{{c.b}}</Option>
				</Select>

				<Label for="lan">{{'lan'|i18n}}</Label>
				<!--<Input id="lan" v-model="ic.lan"/>-->
				<Select :disabled="updateModel"  id="lan" :filterable="false" ref="lan"  v-model="ic.lan">
					<Option v-for="c in langList" :key="c.l" :value="c.l" >{{c.b}}</Option>
				</Select>

				<Label  for="createdTime">{{'createdTime'|i18n}}</Label>
				<div id="createdTime">{{ic.createdTime| formatDate(1)}}</div>

				<Label  for="updatedTime">{{'updatedTime'|i18n}}</Label>
				<div id="updatedTime">{{ic.updatedTime| formatDate(1)}}</div>

				<Label  for="id">{{'id'|i18n}}</Label>
				<Input :disabled="true"  id="id" v-model="ic.id"/>
		
            </div>


        </Drawer>

        <div v-if="isLogin"  :style="qryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu"
             @mouseenter="openDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="qryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
            <table id="queryTable">
                <tr>
                    <td>{{'Country'|i18n}}</td><td>
                    <Select :filterable="false" ref="lan"  v-model="qryData.country">
                        <Option value="" >{{'None'|i18n}}</Option>
                        <Option v-for="c in conList" :key="c.l" :value="c.l" >{{c.b}}</Option>
                    </Select>
                </td>
                    <td>{{'Lan'|i18n}}</td><td>
                    <Select :filterable="false" ref="lan"  v-model="qryData.lan">
                        <Option value="" >{{'None'|i18n}}</Option>
                        <Option v-for="c in langList" :key="c.l" :value="c.l" >{{c.b}}</Option>
                    </Select>
                </td>
                </tr>
                <tr>
                    <td>{{'Key'|i18n}}</td>
                    <td>
                        <Input  v-model="qryData.key"/>
                    </td>
                    <td>{{'Val'|i18n}}</td>
                    <td>
                        <Input  v-model="qryData.val"/>
                    </td>
                </tr>
                <tr>
                    <td>{{'Mod'|i18n}}</td>
                    <td>
                        <Input v-model="qryData.mod"/>
                    </td>
                    <td>{{'clientId'|i18n}}(*)</td>
                    <td>
                        <Input  v-model="qryData.clientId"/>
                    </td>
                </tr>
                <tr>
                    <td> </td>
                    <td><i-button @click="doQuery()">{{'Query'|i18n}}</i-button></td>
                </tr>
            </table>
        </Drawer>
    </div>
</template>

<script>

    import lc from "@/rpc/localStorage"
    const cid = 'i18nConfig';

    const lans = [{'l':'zh',b:'中文'},{'l':'zh-cn',b:'中文(简体)'},{'l':'zh-hk', b:'中文(香港)'},
        {'l':'zh-mo',b:'中文(澳门)'},{'l':'zh-sg',b:'中文(新加坡)'},{'l':'zh-tw',b:'中文(繁体)'},
        {'l':'en',b:'英语'},{'l':'en-us',b:'英语(美国)'}]

    const cons = [{l:'cn',b:'中国'},{l:'hk',b:'香港'},{l:'tw',b:'台湾'},{l:'us',b:'美国'},{l:'gb',b:'英国'}]

    const str2OpVal = {'eq': 1, 'regex': 2, 'in': 3, 'gt': 4, 'gte': 5, 'lt': 6, 'lte': 7}

    export default {
        name: cid,
        components: {
            JFileUpload : () => import('../common/JFileUpload.vue'),
        },

        data() {
            return {
                ops : str2OpVal,
                msg:'',
                actInfo : null,
                totalNum : 0,
                langList : lans,
                conList : cons,

                qryData:{
                    country:'',
                    lan:'',
                    key:'',
                    val:'',
                    mod:'',
                    qryPs : [{opType:1,fn:'country',v:null},{opType:1,fn:'lan',v:null},{opType:1,fn:'clientId',v:null},
                        {opType:2,fn:'key',v:null},{opType:2,fn:'val',v:null},{opType:2,fn:'mod',v:null},]
                },

               qry : {
                    size:30,
                    curPage:1,
                    sortName:'createdTime',
                    order:"-1",//1:增序  -1：降序
                    qryPs : []
               },

                ic:{},
                isLogin:false,
                list: [],
                errMsg:'',

                importFileModel:false,

                updateModel: false,
                tokenDialog: false,
                tokenContent: '',

                addDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : { left:'0px',zindex:1000 },
                },

                importDrawer:{
                    drawerStatus : false,
                    drawerBtnStyle : { left:'0px',zindex:1000 },
                },

                qryDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : { left:'0px',zindex:1000 },
                },

            }
        },

        methods: {

            openDrawer() {
                this.qryDrawer.drawerStatus = true;
                this.qryDrawer.drawerBtnStyle.zindex = 10000;
                this.qryDrawer.drawerBtnStyle.left = '0px';
            },

            uploadFinish(){
                this.refresh()
            },

            onOpenClose(){
                //this.tokenDialog = false;
            },

            import0(){
                this.importFileModel = true
                this.defaultIc()
                this.updateModel = false;
                this.errMsg = '';
                this.addDrawer.drawerStatus = true;
            },

            doImport() {
                this.checkVal();
                if(this.errMsg != '') {
                    return
                }
                let exp = this.ic.country +':' + this.ic.lan + ':' + this.ic.mod + ':' + this.ic.clientId
                let imf = this.$refs.importFile;
                imf.doUpdate(exp);
            },

            doQuery(){
                this.qry.curPage = 1
                this.qry.qryPs = []
                this.qryData.qryPs.forEach((e)=>{
                    let v = this.qryData[e.fn]
                    if(v && v != '') {
                        e.v = v
                        this.qry.qryPs.push(e)
                    }
                })
                this.refresh()
            },

            deleteItem (id) {
                this.callRemote(-1949154339,[id],()=>{
                    this.$Modal.info({
                        title: '成功',
                        content: "删除成功",
                    })
                    this.refresh()
                })
            },

            update(c) {
                this.importFileModel = false
                this.updateModel = true
                this.errMsg = ''
                this.ic = c
                this.addDrawer.drawerStatus = true
            },

            checkVal(){
                if(!this.ic.country) {
                    this.errMsg = '国家代码不能为空'
                    return;
                }

                if(!this.ic.lan) {
                    this.errMsg = '语言代码不能为空'
                    return;
                }

                if(!this.ic.clientId) {
                    this.errMsg = '租户ID不能为空'
                    return;
                }

                if(!(/\d+/.test(this.ic.clientId))) {
                    this.errMsg = '租户ID包含非法字符'
                    return;
                }

                if(!this.ic.mod) {
                    this.errMsg = '模块名称不能为空'
                    return;
                }
                this.errMsg = ''
            },

            doAdd() {
                let self = this

                this.checkVal();
                if(self.errMsg != '') {
                    return
                }

                if(!this.ic.key) {
                    this.errMsg = '资源名称不能为空'
                    return;
                }

                if(!this.ic.val) {
                    this.errMsg = '资源名称不能为空'
                    return;
                }

                if(self.updateModel) {
                    //update
                    this.callRemote(-85932037,[this.ic],()=>{
                        self.refresh()
                        this.addDrawer.drawerStatus = false
                    })
                }else {
                    //add
                    this.callRemote(-2017890951,[this.ic],()=>{
                        self.refresh();
                        this.addDrawer.drawerStatus = false
                    })
                }

                this.setLcv('country',this.ic.country)
                this.setLcv("lang",this.ic.lan)
                this.setLcv("clientId",this.ic.clientId)
                this.setLcv("mod",this.ic.mod)
                this.setLcv("desc",this.ic.desc)

            },

            defaultIc() {
                let cn = this.getLcv('country')
                let la =  this.getLcv("lang")
                let clientId =  this.getLcv("clientId")
                let mod =  this.getLcv("mod")
                let desc = this.getLcv("desc")
                this.ic = { country:cn, lan:la, mod:mod, clientId:clientId, desc:desc };
             },

            add() {
                this.importFileModel = false
                this.defaultIc()
                this.updateModel = false;
                this.errMsg = '';
                this.addDrawer.drawerStatus = true;
            },

            refresh() {
                let self = this;
                this.actInfo = this.$jr.auth.actInfo
                this.isLogin = this.$jr.auth.isLogin()
                if(this.isLogin) {
                    let qry = this.getQueryConditions();
                    let self = this;
                    //list
                    this.callRemote(1142173598,[qry],(resp)=>{
                        self.list = resp.data;
                        if(self.totalNum != resp.total) {
                            self.totalNum = resp.total;
                        }
                        //self.qry.curPage = 1;
                    })
                } else {
                    self.list = [];
                    this.$Notice.warning({
                        title: 'Error',
                        desc: '未登录',
                    });
                }
            },

            getQueryConditions() {
                return this.qry;
            },

            callRemote(mcode,args,sucCb,failCb) {
               // let self = this
                let req = this.$jr.rpc.cmreq(mcode,args)
                this.$jr.rpc.callRpc(req)
                    .then((resp) => {
                    if (resp.code == 0 ) {
                        if(sucCb) {
                            sucCb(resp);
                        }
                    } else {
                        if(failCb) {
                            failCb(resp,resp.msg);
                        } else {
                            this.$Notice.warning({
                                title: 'Error',
                                desc: resp.msg
                            });
                        }
                    }
                }).catch((err) => {
                    if(failCb) {
                        failCb(null,err);
                    } else {
                        this.$Notice.warning({
                            title: 'Error',
                            desc: JSON.stringify(err)
                        });
                    }
                });
            },
            pageSizeChange(pageSize) {
                this.qry.pageSize = pageSize;
                this.qry.curPage = 1;
                this.refresh();
            },

            curPageChange(curPage) {
                this.qry.curPage = curPage
                this.refresh()
            },
            getLcv(key) {
                return lc.get(cid+':'+key)
            },

            setLcv(key,val) {
                return lc.set(cid+':'+key,val)
            }
        },

        mounted () {

            //this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(()=>{
                this.refresh();
            });
            let self = this;

            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"Add",label:"Add",icon:"ios-cog",call:self.add},
                        {name:"Import",label:"Import",icon:"ios-cog",call:self.import0},
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.refresh();

            this.$bus.$on('editorClosed',ec);
        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },

    }
</script>

<style>
    .JI18nConfig{
    }

    .addClientcls a {
        display: inline-block;
        padding-right: 10px;
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
        z-index: 1000000;
    }
</style>