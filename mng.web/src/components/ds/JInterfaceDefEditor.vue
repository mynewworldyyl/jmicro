<template>
    <div class="JInterfaceDefEditor">

        <div v-if="isLogin && roleList && roleList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
					<tr><td>{{'apiId'|i18n}}</td><td>{{'Name'|i18n}}</td>
                    <td>{{'Method'|i18n}}</td><td>{{'ReqEnc'|i18n}}</td>
					<td>{{'SuccessCode'|i18n}}</td><td>{{'SuccessKey'|i18n}}</td><td>{{'FailMessageKey'|i18n}}</td>
					<td>{{'TokenApiCode'|i18n}}</td>
					<td>{{'priceUnit'|i18n}}</td><td>{{'price'|i18n}}</td><td>{{'freeCnt'|i18n}}</td>
					<td>{{'CreatedBy'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td>
                    <td>{{"Operation"|i18n}}</td></tr>
                </thead>
                <tr v-for="c in roleList" :key="'ide' + c.id">
                    <td>{{c.apiId}}</td><td class="descCol">{{c.name}}</td>
					 <td>{{c.method}}</td> <td>{{c.reqEnc}}</td>
					  <td>{{c.successCode}}</td> <td>{{c.successKey}}</td> <td>{{c.failMessageKey}}</td>
					  <td>{{c.tokenApiCode}}</td>
					  <td>{{c.priceUnit}}</td> <td>{{c.price}}</td> <td>{{c.freeCnt}}</td>
                    <td>{{c.updatedBy}}</td><td>{{c.createdTime | formatDate(1)}}</td>
                    <td>
                        <a  @click="viewDef(c)">{{"View"|i18n}}</a>&nbsp;
                        <a  @click="updateDef(c)">{{"Update"|i18n}}</a>&nbsp;
						<a  @click="deleteDef(c.id)">{{"Delete"|i18n}}</a>
						<a  @click="testDef(c)">{{"Test"|i18n}}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && roleList && roleList.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="queryParams.size" :current="queryParams.curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

		<div v-if="!isLogin">
            No permission!
        </div>
		
        <div v-if="!roleList || roleList.length == 0">
            No data!
        </div>

        <!--  查看 或 审批 -->
        <Drawer ref="defInfo"  v-model="defInfoDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="80">
			    <InterfaceDef :updateModel="updateModel" @success="saveSuccess()" :form="form"></InterfaceDef>
				<ParamList :viewDetail="!updateModel" type="header" :defId="form.id" :plist="form.headers"></ParamList>
				
				<ParamList :viewDetail="!updateModel" type="req"  :defId="form.id"  :plist="form.reqs"></ParamList>
				<!-- <ParamList :viewDetail="!updateModel" type="reqParam"  :defId="form.id"  :plist="form.defReqParams"></ParamList> -->
				<ParamList :viewDetail="!updateModel" type="config"  :defId="form.id"  :plist="form.ortherConfigs"></ParamList>
				
              <div>{{errorMsg}}</div>
        </Drawer>
		
		<!--  测试接口 -->
		<Drawer ref="testDef"  v-model="testDefDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
		    :draggable="true" :scrollable="true" width="80">
		
		<div class="testBtnContainer">
			<el-button size="mini" @click="doTest()">{{'Send'|i18n}}</el-button>
		</div>
		
		<div v-if="form && form.id && form.reqs && form.reqs.length" class="paramContainer">
			<el-row v-for="p in form.reqs">
				<el-col class="title" :span="3"><span>{{p.name|i18n}}</span>
				<span v-if="p.isRequired">*</span>
				</el-col>
				<el-col :span="21">
					<el-input v-model="testParam[p.key]" v-if="p.type=='string'" type="textarea" :autosize="{minRows: 1, maxRows: 10}"></el-input>
					<el-input v-model="testParam[p.key]" v-else-if="p.type=='boolean'" type="checkbox"></el-input>
					<el-input v-model="testParam[p.key]" v-else></el-input>
				</el-col>
			</el-row>
		</div>
		
		<RespView v-if="testResp.resp" :resp="testResp.resp"></RespView>

		</Drawer>

	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<Drawer v-if="isLogin"   v-model="queryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
	         :draggable="true" :scrollable="true" width="50">
	    <table id="queryTable">
	        <tr>
	        	<td>startTime</td>
	            <td>
	        		 <el-date-picker v-model="queryParams.ps.startTime" type="date" placeholder="选择日期"
	        		       value-format="yyyy-MM-dd">
	        		    </el-date-picker>
	             </td>
	        	 <td>endTime</td>
	            <td>
	        		<el-date-picker v-model="queryParams.ps.endTime" type="date" placeholder="选择日期"
	        		     value-format="yyyy-MM-dd">
	        		   </el-date-picker>
	            </td>
	        </tr>
	        	
	        <tr>
	            <td>ActId</td><td> <Input  v-model="queryParams.ps.actId"/></td>
	            <td>ClientId</td><td> <Input  v-model="queryParams.ps.clientId"/></td>
	        </tr>
	        <tr>
	            <td>Code</td><td> <Input  v-model="queryParams.ps.code"/></td>
	        	<td>Name</td><td> <Input  v-model="queryParams.ps.name"/></td>
	        </tr>
			
			<tr>
			    <td>Desc</td><td> <Input  v-model="queryParams.ps.desc"/></td>
				<td>ApiId</td><td> <Input  v-model="queryParams.ps.apiId"/></td>
			</tr>
	        <tr>
	            <td><i-button @click="doQuery()">{{"Query"|i18n}}</i-button></td><td></td>
	        </tr>
	    </table>
	</Drawer>
	

    </div>
</template>

<script>

 import cons from "@/rpc/constants"
 import defCons from "./cons.js"
 import ParamList from "./ParamList.vue"
 import InterfaceDef from "./InterfaceDef.vue"
  import RespView from "./RespView.vue"
  
 const cid = 'interfaceDef'
 
 const sn = defCons.sn;
 const ns = defCons.ns;
 const v = defCons.v;
 
    export default {
        name: cid,
        components: {
			ParamList,InterfaceDef,RespView
        },
        data() {
            return {
                errorMsg:'',
                isLogin:false,
                roleList: [],

				queryParams:{size:10,curPage:1,ps:{}},
				totalNum:0,

                updateModel:false,
                form : {},
				
				statusMap: defCons.statusMap,
				
                defInfoDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },
				
				testDefDrawer: {
					drawerStatus : false,
					drawerBtnStyle : {left:'0px',zindex:1000},
				},
				
				queryDrawer: {
				    drawerStatus:false,
				    drawerBtnStyle:{left:'0px',zindex:1000},
				},
				
				testParam:{},
				testResp:{}
            }
        },

        methods: {
			
			doQuery() {
				this.queryParams.curPage = 1
			    this.refresh();
			},
			
			openQueryDrawer() {
			    this.queryDrawer.drawerStatus = true;
			    this.queryDrawer.drawerBtnStyle.zindex = 10000;
			    this.queryDrawer.drawerBtnStyle.left = '0px';
			},
			
			doTest(){
				//console.log(this.testParam)
				let qry = {jsonParam: JSON.stringify(this.testParam),apiId:this.form.apiId}
				let st = new Date().getTime();
				this.$jr.rpc.callRpcWithParams("cn.jmicro.api.ds.IDataApiJMSrv", ns, v, 'getData', [qry])
				    .then((resp)=>{
				        this.testResp = resp
						this.testResp.cost = new Date().getTime() - st
				    }).catch((err)=>{
						this.$notify.error({
							title: '错误',
							message: err
						});
				});
			},
			
			saveSuccess() {
				this.defInfoDrawer.drawerStatus = false;
				this.form = {}
				this.errorMsg = '';
				this.refresh()
			},
			
			viewDef(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getDefDetail', [c.id])
				    .then((resp)=>{
				        if(resp.code == 0){
				            this.updateModel = false;
				            this.form = resp.data;
				            this.errorMsg = '';
				            this.defInfoDrawer.drawerStatus = true;
				        } else {
				           this.$notify.error({
				           		title: '错误',
				           		message: resp.msg
				           	});
				        }
				    }).catch((err)=>{
						this.$notify.error({
								title: '错误',
								message: resp.msg
							});
				});
			},
			
			async testDef(c){
				let resp = await this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getDefDetail', [c.id])
				if(resp && resp.code == 0){
					this.testParam = {}
				    this.form = resp.data
				    this.testDefDrawer.drawerStatus = true
				} else if(!resp) {
				   this.$notify.error({
				   		title: '错误',
				   		message: resp.msg
				   	});
				}else {
					this.$notify.error({
							title: '错误',
							message: '获取明细失败'
						});
				}
			},
			
			updateDef(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getDefDetail', [c.id])
				    .then((resp)=>{
				        if(resp.code == 0){
				            this.updateModel = true;
				            this.form = resp.data
				            this.errorMsg = '';
				            this.defInfoDrawer.drawerStatus = true;
				        } else {
				           this.$notify.error({
				           		title: '错误',
				           		message: resp.msg
				           	});
				        }
				    }).catch((err)=>{
						this.$notify.error({
								title: '错误',
								message: resp.msg
							});
				});
			},
			
			deleteDef(defId){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'delDef', [defId])
					.then((resp)=>{
						if(resp.code == 0){
						   this.refresh()
						} else {
						   this.$notify.error({
								title: '错误',
								message: resp.msg
							});
						}
					}).catch((err)=>{
						this.$notify.error({
							title: '错误',
							message: resp.msg
						});
				});
			},
			
			addDef() {
				this.updateModel = true;
				this.form = {method:'POST',reqEnc:'utf-8',respEnc:'utf-8',returnType:'sync',respType:'json'}
				this.errorMsg = '';
				this.defInfoDrawer.drawerStatus = true;
			},

            curPageChange(curPage){
				this.queryParams.curPage = curPage
                this.refresh();
            },

            pageSizeChange(pageSize){
				this.queryParams.size = pageSize
				this.queryParams.curPage = 1
                this.refresh();
            },

            refresh() {
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
                if(this.isLogin) {
                    let params = this.getQueryConditions();
					
                    let self = this;
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listDefs', [params])
                        .then((resp)=>{
                            if(resp.code == 0){
                                self.roleList = resp.data;
                                self.totalNum = resp.total;
                                //self.queryParams.curPage = 1;
                            } else {
								this.$notify.error({
									title: '错误',
									message: resp.msg
								});
                            }
                        }).catch((err)=>{
						   this.$notify.error({
								title: '错误',
								message: JSON.stringify(err)
							});
                    });
                }else {
                    self.roleList = [];
                }
            },

            getQueryConditions() {
                return this.queryParams;
            },
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.refresh);
            this.refresh();
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid, "menus":[
                    {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh},
					{name:"Add",label:"Add",icon:"ios-cog",call:self.addDef},
					]
                });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);
        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },

    }
</script>

<style>
    .JInterfaceDefEditor{
        min-height: 500px;
    }

	.descCol{
		overflow: hidden;
		text-overflow: ellipsis;
		flex-wrap: nowrap;
	}
	
	.title {
		font-weight: bold;
	}
	
	
</style>