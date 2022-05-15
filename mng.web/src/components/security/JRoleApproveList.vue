<template>
    <div class="JRoleApproveList">

        <table v-if="isLogin && authList && authList.length > 0" class="configItemTalbe" width="99%">
          <thead><tr><td>{{'RoleName'|i18n}}</td><td>{{'ActName'|i18n}}</td><td>{{'Status'|i18n}}</td>
              <td>{{'CreatedTime'|i18n}}</td> <td>{{'UpdatedTime'|i18n}}</td>
              <td>{{'CreatedBy'|i18n}}</td><td>{{'UpdatedBy'|i18n}}</td>
              <td>{{'Desc'|i18n}}</td>
              <td>{{'Operation'|i18n}}</td></tr>
          </thead>
          <tr v-for="a in authList" :key="a.id">
              <td>{{a.ma.roleName}}</td><td>{{a.ma.actName}}</td><td>{{authStatus[a.ma.status]}}</td>
              <td>{{a.ma.createdTime|formatDate(1)}}</td><td>{{a.ma.updatedTime|formatDate(1)}}</td>
              <td>{{a.ma.createdBy}}</td><td>{{a.ma.updatedBy}}</td><td v-html="a.ma.remark"></td>
              <td><a v-if="a.ma.status == st.STATUS_APPROVE"   @click="descAuthOk(a.ma.id,st.STATUS_REVOKE)"> {{'Revoke' | i18n}} </a>
                  <a v-if="a.ma.status == st.STATUS_APPLY || a.ma.status == st.STATUS_REJECT"   @click="descAuthOk(a.ma.id,st.STATUS_APPROVE)"> {{'Approve' | i18n}} </a>
                  <a v-if="a.ma.status == st.STATUS_APPLY"   @click="descAuthOk(a.ma.id,st.STATUS_REJECT)"> {{'Reject' | i18n}} </a>
          		  <a v-if="a.ma.status == st.STATUS_REVOKE"   @click="descAuthOk(a.ma.id,st.STATUS_APPROVE)"> {{'Reinvoke' | i18n}} </a>
              </td>
          </tr>
        </table>

        <div v-if="isLogin  && authList && authList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="qry.size" :current="qry.curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10,20,50,100,150,200]"></Page>
        </div>

        <div v-if="!isLogin">No permission</div>
        <div v-if="isLogin  && (!authList || authList.length == 0)">
            No data
        </div>

        <Drawer  v-if="isLogin"  v-model="detailDrawer.drawerStatus" :closable="false" placement="right"
                 :transfer="true" :draggable="true" :scrollable="true" width="50" @close="closeDetailDrawer()">
            <table class="detailTable" width="95%">
                <tr>
                    <td>{{"Online"|i18n}}</td><td>{{auth.online}}</td>
                    <td>{{"PerType"|i18n}}</td><td>{{auth.perType}}</td>
                </tr>
                <tr>
                    <td>{{"ServiceName"|i18n}}</td><td colspan="3">{{auth.serviceName}}</td>
                </tr>
                <tr>
                    <td>{{"Namespace"|i18n}}</td><td>{{auth.namespace}}</td>
                    <td>{{"Version"|i18n}}</td><td>{{auth.version}}</td>
                </tr>
                <tr>
                    <td>{{"Method"|i18n}}</td><td>{{auth.method}}</td>
                    <td>{{"Hash"|i18n}}</td><td>{{auth.haCode}}</td>
                </tr>
                <tr>
                    <td>{{"CreatedTime"|i18n}}</td><td>{{auth.createdTime}}</td>
                    <td>{{"UpdatedTime"|i18n}}</td><td>{{auth.updateTime}}</td>
                </tr>
                <tr>
                    <td>{{"ParamType"|i18n}}</td><td colspan="3">{{auth.paramsDescs}}</td>
                </tr>
                <tr>
                    <td>{{"ReturnType"|i18n}}</td><td>{{auth.returnTypeDesc}}</td>
                    <td>{{"ClientId"|i18n}}</td><td>{{auth.clientId}}</td>
                </tr>

            </table>
        </Drawer>

        <div v-if="isLogin"  :style="query.drawerBtnStyle" class="queryDrawerStatu" @mouseenter="openQueryDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="query.drawerStatus" :closable="false" placement="left"
                :transfer="true" :draggable="true" :scrollable="true" width="50">
            <div><i-button @click="doQueryResource()">{{'Confirm'|i18n}}</i-button></div>
            <table>
                
                <tr>
                    <td>{{"ActName"|i18n}}</td>
                    <td>
                        <Input  v-model="qry.ps.actName"/>
                    </td>
                    <td>{{"Status"|i18n}}</td>
                    <td>
                        <Select v-model="qry.ps.status">
                            <Option value="">{{"none" | i18n}}</Option>
                            <Option v-for=" (val,key) in authStatus" :key="'s_'+key" :value="key">{{val|i18n}}</Option>
                        </Select>
                    </td>
                </tr>

                <tr>
                    <td>{{"clientId"|i18n}}</td>
                    <td>
                        <Input  v-model="qry.ps.clientId"/>
                    </td>
                </tr>
            </table>
        </Drawer>

        <Modal
			v-model="showDescDialog"
			title="备注"
			@on-ok="descAuthOk"
			@on-cancel="descAuthCancel">
            <Input v-model="auth.desc"></Input>
        </Modal>

    </div>
</template>

<script>

    import cons from "@/rpc/constants"
	import st from "./c"

    const cid = 'roleApproveList';
    const sn = 'cn.jmicro.security.api.IRoleServiceJMSrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    export default {
        name: 'JRoleApproveList',
        data () {
            return {
				st,
                authStatus:{"1":'Apply',"2":"Reject","3":"Approve","4":"Revoke"},
                authList:[],
                isLogin : false,
                act:null,
				
				showDescDialog:false,
                auth:{desc:''},
                dicts:{},

                queryParams:{},

                errMsg:'',

                detailDrawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },
				
                query: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },

				totalNum:0,
				qry:{
					size:20,
					curPage:1,
					sortName:'createdTime',
					order:2,
					ps:{status:'3'},
				}
            }
        },
        methods: {
			
            refresh(){
                let self = this;
				this.isLogin = this.$jr.auth.isLogin();
				this.act = this.$jr.auth.actInfo;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'queryRoleAuthList', [this.qry])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
						self.totalNum = resp.total;
                        self.$nextTick(() => {self.authList = resp.data})
                        self.errMsg = "";
                    }).catch((err) => {
                    self.errMsg = err;
                });
            },

            descAuthOk(authId,status) {
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateRoleActStatus',
                    [authId, status, this.desc])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
                        self.descAuthCancel();
                        self.refresh();
                    }).catch((err) => {
            			self.errMsg = err;
                });
            },
            
            descAuthCancel() {
                this.status = -1;
                this.auth = {};
                this.showDescDialog = false;
            },

            closeAuthDrawer() {
                this.resetAuthData();
                this.authClientDrawer.drawerStatus = false;
                this.authClientDrawer.drawerBtnStyle.zindex = 100;
            },

            openAuthDrawer(cm) {
                this.cm = cm;
                this.resetAuthData();
                this.refresh();
                this.errMsg = "";
                this.authClientDrawer.drawerStatus = true;
                this.authClientDrawer.drawerBtnStyle.zindex = 1000;
            },

            resetAuthData() {
                this.errMsg = '';
                this.showDescDialog=false;
                this.desc = '';
                this.auth = {};
            },

            doQueryResource(){
                //let qry = this.queryParams;
                this.qry.curPage = 1;
                this.refresh();
            },

            curPageChange(curPage){
                this.qry.curPage = curPage;
                this.refresh();
            },

            pageSizeChange(pageSize){
                this.qry.size = pageSize;
                this.qry.curPage = 1;
                this.refresh();
            },

            openQueryDrawer(){
                this.query.drawerStatus = true;
                this.query.drawerBtnStyle.zindex = 10000;
                this.query.drawerBtnStyle.left = '0px';
            },

            openDetailDrawer(cm) {
                this.cm = cm;
                this.errMsg = "";
                this.detailDrawer.drawerStatus = true;
            },

            closeDetailDrawer() {
                this.detailDrawer.drawerStatus = false;
                this.detailDrawer.drawerBtnStyle.zindex = 100;
            },

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            //has admin permission, only control the show of the button
            this.$jr.auth.addActListener(this.refresh);
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[
                        {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });
            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);

            self.refresh();
        },
    }
</script>

<style>
	
    .JRoleApproveList{
        height:auto;
    }
	
    .queryDrawerStatu{
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

    .detailTable {

    }

    .detailTable tr td{
        padding-right:10px;
    }

</style>