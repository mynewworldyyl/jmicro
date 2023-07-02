<template>
    <div class="JDeviceFunctionDefEditor">

        <div v-if="isLogin && roleList && roleList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
					<tr><td>{{'id'|i18n}}</td><td>{{'funName'|i18n}}</td>
                    <td>{{'grp'|i18n}}</td><td>{{'labelName'|i18n}}</td>
					<td>{{'actId'|i18n}}</td><td>{{'clientId'|i18n}}</td>
					<td>{{'ver'|i18n}}</td><td>{{'funType'|i18n}}</td>
					<td>{{'showFront'|i18n}}</td><td>{{'selfDefArg'|i18n}}</td>
					<!-- <td>{{'CreatedBy'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td> -->
                    <td>{{"Operation"|i18n}}</td></tr>
                </thead>
                <tr v-for="c in roleList" :key="'ide' + c.id">
                    <td>{{c.id}}</td><td class="descCol">{{c.funName}}</td>
					 <td>{{c.grp}}</td> <td>{{c.labelName}}</td>
					  <td>{{c.actId}}</td> <td>{{c.clientId}}</td> <td>{{c.ver}}</td>
					  <td>{{c.funType}}</td><td>{{c.showFront+''|i18n}}</td><td>{{c.selfDefArg+''|i18n}}</td>
					  <!-- <td>{{c.updatedBy}}</td><td>{{c.updatedTime | formatDate(1)}}</td> -->
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
			    <DeviceFunctionDef :updateModel="updateModel" @success="saveSuccess()" :form="form"></DeviceFunctionDef>		
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

		</Drawer><!--  测试接口结束 -->

	<!-- 弹出查询页面 -->
	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<!-- 数据查询开始-->
	<Drawer v-if="isLogin"   v-model="queryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
	         :draggable="true" :scrollable="true" width="50">
	    <table id="queryTable">	        	
	        <tr>
	            <td>ActId</td><td> <Input  v-model="queryParams.ps.actId"/></td>
	            <td>ClientId</td><td> <Input  v-model="queryParams.ps.clientId"/></td>
	        </tr>
	        <tr>
	            <td>funName</td><td> <Input  v-model="queryParams.ps.funName"/></td>
	        	<td>funDesc</td><td> <Input  v-model="queryParams.ps.funDesc"/></td>
	        </tr>
			
			<tr>
			    <td>labelName</td><td> <Input  v-model="queryParams.ps.labelName"/></td>
				<td>grp</td><td> <Input  v-model="queryParams.ps.grp"/></td>
			</tr>
			
			<tr>
			    <td>funType</td><td> <Input  v-model="queryParams.ps.funType"/></td>
				<td>ver</td><td> <Input  v-model="queryParams.ps.ver"/></td>
			</tr>
			<tr>
			    <td>showFront</td><td> <Input  v-model="queryParams.ps.showFront"/>
				
				</td>
				<td></td><td> <!-- <Input  v-model="queryParams.ps.ver"/> --></td>
			</tr>
			
	        <tr>
	            <td><i-button @click="doQuery()">{{"Query"|i18n}}</i-button></td><td></td>
	        </tr>
	    </table>
	</Drawer><!-- 数据查询结束-->
	

    </div>
</template>

<script>

 import cons from "@/rpc/constants"
 import DeviceFunctionDef from "./DeviceFunctionDef.vue"
 
 const cid = 'deviceFunctionDefEditor'
 
    export default {
        name: cid,
        components: {
			DeviceFunctionDef
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
				
				//statusMap: defCons.statusMap,
				
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
				
				let ps = JSON.stringify(this.testParam)
				let key = "__inttps_" + this.form.apiId
				this.$jr.lc.set(key,ps)
				
				let qry = {jsonParam: JSON.stringify(this.testParam), apiId:this.form.apiId}
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
				this.updateModel = false;
				this.form = c;
				this.errorMsg = '';
				this.defInfoDrawer.drawerStatus = true;
			},
			
			async testDef(c){
				let resp = await this.$jr.rpc.callRpcWithParams(sn, ns, v, 'getDefDetail', [c.id])
				if(resp && resp.code == 0){
					this.testParam = {}
				    this.form = resp.data
					
					let key = "__inttps_" + this.form.apiId
					let js = this.$jr.lc.get(key)
					if(js && js.length > 0) {
						this.testParam = JSON.parse(js)
					}
					
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
				this.updateModel = true;
				this.form = c
				this.errorMsg = '';
				this.defInfoDrawer.drawerStatus = true;
			},
			
			deleteDef(defId){
				//delFunDef
				this.$jr.rpc.invokeByCode(-385569651, [defId])
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
				this.form = {}
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
					//deviceFunDefs
                    this.$jr.rpc.invokeByCode(2089184007, [params])
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
    .JDeviceFunctionDefEditor{
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