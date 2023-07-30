<template>
	<div class="DeviceFuntionList">
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"funName"|i18n}}</td><td>{{'labelName'|i18n}}</td>
		        <td>{{'productId'|i18n}}</td><td>{{'defId'|i18n}}</td>
				<td>{{'selfDefArg'|i18n}}</td><td>{{'showFront'|i18n}}</td>
				<td  class="descCol">{{'desc'|i18n}}</td><td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h_'+c.id">
		        <td>{{c.funName}}</td><td>{{c.labelName}}</td><td>{{c.productId}}</td><td>{{c.defId}}</td>
				 <td>{{c.selfDefArg}}</td><td>{{c.showFront}}</td><td class="descCol">{{c.desc}}</td> 
		        <td>
		           <a @click="viewParam(c)">{{'View'|i18n}}</a>&nbsp;
		           <a @click="updateParam(c)">{{'Update'|i18n}}</a>&nbsp;
		           <a @click="deleteParam(c)">{{'Delete'|i18n}}</a>
				   <a @click="funList(c)">{{'Functions'|i18n}}</a>
		        </td>
		    </tr>
		</table>
		
		<div v-if="isLogin && plist && plist.length > 0" style="position:relative;text-align:center;">
		    <Page ref="pager" :total="totalNum" :page-size="queryParams.size" :current="queryParams.curPage"
		          show-elevator show-sizer show-total @on-change="curPageChange"
		          @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
		</div>
		
		<div v-if="!isLogin">
		    No permission!
		</div>
		
		<div v-if="!plist || plist.length == 0">
		    No data!
		</div>

        <Drawer ref="defInfo"  v-model="defInfoDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50" :mask-closable="false" :mask="true">
		 <el-row>
			<el-col :span="6">{{"Name"|i18n}}</el-col>
			<el-col><el-input v-model="p.name" :disabled="model==3" /></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"code"|i18n}}</el-col>
			<el-col><el-input v-model="p.code" :disabled="model==3" /></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"desc"|i18n}}</el-col>
			<el-col><el-input v-model="p.desc" :disabled="model==3" /></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"stage"|i18n}}</el-col>
			<el-col>
				<el-select style="width:100%" v-model="p.stage" :disabled="model==3">
					<el-option value="1">{{'开发阶段'}}</el-option>
					<el-option value="2">{{'测试产阶段'}}</el-option>
					<el-option value="3">{{'试投产阶段'}}</el-option>
					<el-option value="4">{{'正式投产'}}</el-option>
					<el-option value="5">{{'终止'}}</el-option>
				</el-select>
			</el-col>
		 </el-row>
		
		 <el-row>
			<el-button size="mini" @click="defInfoDrawer.drawerStatus = false">取消</el-button>
			<el-button  :disabled="model==3" size="mini" type="primary" @click="doAddOrUpdateParam">确定</el-button>
		 </el-row>
	</Drawer>

	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<Drawer v-if="isLogin"   v-model="queryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
	         :draggable="true" :scrollable="true" width="50">
	    <table id="queryTable">
			<tr>
				 <td>Name</td><td> <Input  v-model="queryParams.ps.name"/></td>
			     <td>Code</td><td> <Input  v-model="queryParams.ps.code"/></td>
			</tr>
			<tr>
			    <td>Desc</td><td> <Input  v-model="queryParams.ps.desc"/></td>
			    <td>ClientId</td><td> <Input  v-model="queryParams.ps.clientId"/></td>
			</tr>
			<tr>
				<td>Stage</td><td> 
					<el-select style="width:100%" v-model="p.stage" :disabled="model==3">
						<el-option label="全部" value=""></el-option>
						<el-option value="1">{{'开发阶段'}}</el-option>
						<el-option value="2">{{'测试产阶段'}}</el-option>
						<el-option value="3">{{'试投产阶段'}}</el-option>
						<el-option value="4">{{'正式投产'}}</el-option>
						<el-option value="5">{{'终止'}}</el-option>
					</el-select>
				</td>
				<td></td>
				<td>
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
	const cid = 'DeviceProductList';

	export default {
		name: cid,

		props: {
			updateModel: {
				type: Boolean,
				default: false
			},

			product: {
				type: Object,
				default: {},
				args:[],
			},
		},
		
		data() {
			return {
				p: {},
				addOrUpdateDialog: false,
				model: 3,
				
				errorMsg:'',
				isLogin:false,
				plist: [],
				
				queryParams:{size:10,curPage:1,ps:{}},
				totalNum:0,
				
				defInfoDrawer: {
				    drawerStatus : false,
				    drawerBtnStyle : {left:'0px',zindex:1000},
				},
				
				queryDrawer: {
				    drawerStatus:false,
				    drawerBtnStyle:{left:'0px',zindex:1000},
				},
				
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
			
			viewParam(c){
				this.model = 3;
				this.p = c;
				this.defInfoDrawer.drawerStatus = true;
			},
			
			updateParam(c){
				this.model = 1;
				this.p = c
				this.errorMsg = '';
				this.defInfoDrawer.drawerStatus = true;
			},
			
			addParam(){
				this.model = 2;
				this.p = {type:'string',belongTo:'req',tags:'defa'};
				this.errorMsg = '';
				this.defInfoDrawer.drawerStatus = true;
			},
		
			doAddOrUpdateParam() {
				if (!this.checkParam(this.p)) {
					return
				}

				if (this.model == 1) {
					//update
					this.$jr.rpc.invokeByCode(-348618462, [this.p])
						.then((resp) => {
							if (resp.code == 0 && resp.data) {
								this.defInfoDrawer.drawerStatus = false;
							} else {
								this.$notify.error({
									title: '错误',
									message: resp.msg || "未知错误"
								});
							}
						}).catch((err) => {
					  this.$notify.error({
								title: '错误',
								message: JOSN.stringify(err)
							});
						});
				} else if(this.model == 2) {
					//add
					this.$jr.rpc.invokeByCode(-1207151648, [this.p])
						.then((resp) => {
							if (resp.code == 0 && resp.data) {
								this.defInfoDrawer.drawerStatus = false;
								this.refresh()
							} else {
								this.$notify.error({
									title: '错误',
									message: resp.msg || "未知错误"
								});
							}
						}).catch((err) => {
							this.$notify.error({
								title: '错误',
								message: JOSN.stringify(err)
							});
						});
				}
			},

			deleteParam(p) {
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'defParam', [p.id])
					.then((resp) => {
						if (resp.code == 0 && resp.data) {
							this.plist.splice(this.plist.findIndex(item => item.id === p.id), 1)
						} else {
							this.$notify.error({
								title: '错误',
								message: resp.msg || "未知错误"
							});
						}
					}).catch((err) => {
						this.$notify.error({
							title: '错误',
							message: JOSN.stringify(err)
						});
					});
			},

			checkParam(p) {
				if (this.model == 3) {
					this.$notify.error({
					 title: '错误',
						message: '非法操作'
					});
					return false
				}
				
				if (!p.name) {
					this.$notify.error({
					 title: '错误',
						message: '参数名称不能为空'
					});
					return false
				}
				
				if (!p.desc) {
					this.$notify.error({
					 title: '错误',
						message: '参数键值不能为空'
					});
					return false
				}

				if (!p.stage) {
					this.$notify.error({
						title: '错误',
						message: '阶段不能为空'
					});
					return false
				}
				
				return true
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
			        this.$jr.rpc.invokeByCode(437581512, [params])
			            .then((resp)=>{
			                if(resp.code == 0){
			                    if(resp.total == 0) {
			                        console.log("success");
			                    }else {
			                        this.plist = resp.data;
			                        this.totalNum = resp.total;
			                    }
			                } else {
			                    window.console.log(resp.msg);
			                }
			            }).catch((err)=>{
			            window.console.log(err);
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
					{name:"Add",label:"Add",icon:"ios-cog",call:self.addParam},
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
	.DeviceFuntionList {
		border-top: 1px dotted lightgray;
		margin-top: 6px;
		padding-top: 10px;
		text-align: left;
	}
	
	.title{
		font-weight: bold;
		font-size: 17px;
	}
	
	.valCol{
		overflow: hidden;
		text-overflow: ellipsis;
		flex-wrap: nowrap;
	}
</style>
