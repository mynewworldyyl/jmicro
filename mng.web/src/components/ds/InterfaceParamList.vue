<template>
	<div class="InterfaceParamList">
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"tags"|i18n}}</td><td>{{'key'|i18n}}</td>
		        <td>{{'name'|i18n}}</td><td>{{'type'|i18n}}</td><td>{{'belongTo'|i18n}}</td>
				<td>{{'isRequired'|i18n}}</td><td>{{'defVal'|i18n}}</td><td>{{'val'|i18n}}</td>
		        <td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h_'+c.id">
		        <td>{{c.tags}}</td><td class="descCol">{{c.key}}</td>
				 <td>{{c.name}}</td> <td>{{c.type}}</td><td>{{c.belongTo}}</td>
				  <td>{{c.isRequired}}</td> <td>{{c.defVal}}</td> <td class="valCol">{{c.val}}</td>
		        <td>
		           <a @click="viewParam(c)">{{'View'|i18n}}</a>&nbsp;
		           <a @click="updateParam(c)">{{'Update'|i18n}}</a>&nbsp;
		           <a @click="deleteParam(c)">{{'Delete'|i18n}}</a>
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
			<el-col :span="6">{{"Key"|i18n}}</el-col>
			<el-col><el-input v-model="p.key" :disabled="model==3" /></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"Type"|i18n}}</el-col>
			<el-col>
				<el-select style="width:100%" v-model="p.type" :disabled="model==3">
					<el-option value="string">{{'string'|i18n}}</el-option>
					<el-option value="integer">{{'integer'|i18n}}</el-option>
					<el-option value="float">{{'float'|i18n}}</el-option>
					<el-option value="boolean">{{'boolean'|i18n}}</el-option>
				</el-select>
			</el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"BelongTo"|i18n}}</el-col>
			<el-col><el-select style="width:100%" v-model="p.belongTo" :disabled="model==3">
					<el-option value="req">{{'req'|i18n}}</el-option>
					<el-option value="reqParam">{{'reqParam'|i18n}}</el-option>
					<el-option value="header">{{'header'|i18n}}</el-option>
					<el-option value="config">{{'config'|i18n}}</el-option>
				</el-select></el-col></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"DefValue"|i18n}}</el-col>
			<el-col><el-input v-model="p.defVal" :disabled="model==3"/></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"Value"|i18n}}</el-col>
			<el-col><el-input v-model="p.val" :disabled="model==3"/></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"isRequired"|i18n}}</el-col>
			<el-col><el-checkbox v-model="p.isRequired" :disabled="model==3"/></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"Desc"|i18n}}</el-col>
			<el-col><el-input type="textarea" autosize v-model="p.desc" :disabled="model==3"/></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"Tag"|i18n}}</el-col>
			<el-col><el-input v-model="p.tags" :disabled="model==3"/></el-col>
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
	            <td>ActId</td><td> <Input  v-model="queryParams.ps.createdBy"/></td>
	            <td>ClientId</td><td> <Input  v-model="queryParams.ps.clientId"/></td>
	        </tr>
			<tr>
			    <td>Tags</td><td> <Input  v-model="queryParams.ps.tags"/></td>
			    <td>Name</td><td> <Input  v-model="queryParams.ps.name"/></td>
			</tr>
			<tr>
			    <td>Desc</td><td> <Input  v-model="queryParams.ps.desc"/></td>
			    <td></td><td></td>
			</tr>
			<tr>
			    <td>BelongTo</td><td> 
					<el-select v-model="queryParams.ps.belongTo" placeholder="请选择">
					<el-option label="全部" value=""></el-option>
				    <el-option label="默认参数" value="reqParam"></el-option>
					<el-option label="请求参数" value="req"></el-option>
					<el-option label="头部参数" value="header"></el-option>
					<el-option label="配置参数" value="config"></el-option>
				  </el-select>
				</td>
				</td>
				<td>Type</td><td> 
					<el-select v-model="queryParams.ps.type" placeholder="请选择">
					<el-option label="全部" value=""></el-option>
				    <el-option label="字符串" value="string"></el-option>
					<el-option label="整数" value="integer"></el-option>
					<el-option label="布尔值" value="boolean"></el-option>
					<el-option label="浮点数" value="float"></el-option>
				  </el-select>
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
	import defCons from "./cons.js"

	const sn = defCons.sn;
	const ns = defCons.ns;
	const v = defCons.v;

	const cid = 'InterfaceParamList';

	export default {
		name: cid,

		data() {
			return {
				p: {},
				addOrUpdateDialog: false,
				model: 3,
				
				errorMsg:'',
				isLogin:false,
				plist: [],
				
				queryParams:{size:10,curPage:1,ps:{belongTo:"",type:""}},
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
					this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateParam', [this.p])
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
					this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addParam', [this.p])
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
				
				if (!p.key) {
					this.$notify.error({
					 title: '错误',
						message: '参数键值不能为空'
					});
					return false
				}

				if (!p.belongTo) {
					this.$notify.error({
						title: '错误',
						message: '参数归属不能为空'
					});
					return false
				}

				if (!p.type) {
					this.$notify.error({
						title: '错误',
						message: '参数类型不能为空'
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
			        this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listParams', [params])
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
	.InterfaceParamList {
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
