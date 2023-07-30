<template>
	<div class="DeviceFunCmdList">

		<div>
			<el-button size="mini" type="primary" @click="refresh()">查询</el-button>
			<el-button size="mini" type="primary" @click="addCmd()">增加</el-button>
			<el-button size="mini" type="primary" @click="closeFunListDrawer()">关闭</el-button>
		</div>
		
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"name"|i18n}}</td><td>{{'productId'|i18n}}</td>
				<td>{{'InterfaceId'|i18n}}</td><td>{{'funId'|i18n}}</td> <td>{{'clientId'|i18n}}</td> 
				<td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h_'+c.id">
		        <td>{{c.name}}</td><td>{{c.productId}}</td><td>{{c.defId}}</td><td>{{c.funId|i18n}}</td>
				 <td>{{c.clientId}}</td>
		        <td>
		           <a @click="viewDetail(c)">{{'详情'|i18n}}</a>&nbsp;
				   <a  v-if="canShow(c)"  @click="updateCmd(c)">{{'更新'|i18n}}</a>&nbsp;
				   <a  v-if="canShow(c)"  @click="deleteCmd(c)">{{'删除'|i18n}}</a>
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

	<Drawer ref="cmdInfo"  v-model="cmdDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
	         :styles="cmdDrawer.drawerBtnStyle" :draggable="true" :scrollable="true" width="80" :mask-closable="false" :mask="false">
		<el-row>
			<el-col :span="6">{{"功能名称"|i18n}}</el-col>
			<el-col><el-input v-model="funDef.labelName" disabled/></el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"功能标识"|i18n}}</el-col>
			<el-col><el-input v-model="funDef.funName" disabled/></el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"功能描述"|i18n}}</el-col>
			<el-col><el-input v-model="funDef.funDesc" disabled/></el-col>
		</el-row>
				
		 <el-row>
			<el-col :span="6">{{"指令名称"|i18n}}</el-col>
			<el-col><el-input v-model="cmd.name" :disabled="model==3" /></el-col>
		 </el-row>
		 <el-row>
			<el-col :span="6">{{"指令描述"|i18n}}</el-col>
			<el-col><el-input v-model="cmd.desc" :disabled="model==3" /></el-col>
		 </el-row>
		 
		 <el-row v-for="ar in cmd.args">
			<el-col class="argLabel" :span="6" @click.native="showArgDesc(ar)">{{ar.label+'('+ar.name+')'}}</el-col>
			<el-col>
				 <Input v-model="ar.val" placeholder="" :disabled="model==3" />
				<!-- <el-col><el-input v-model="ar.val" :disabled="model==3" @change="valChange(e)"/></el-col> -->
			</el-col>
		 </el-row>
		<el-row>
			<el-col :span="6">{{"ClientId"|i18n}}</el-col>
			<el-col>
				<el-select style="width:100%" v-model="cmd.clientId" :disabled="model==3" placeholder="请选择">
					<el-option v-for="o in $jr.auth.getClients()" :key="'c_'+o" :value="o" :label="o"></el-option>
				</el-select>
			</el-col>
		 </el-row>
		 <el-row>
			<el-button size="mini" @click="cmdDrawer.drawerStatus = false">关闭</el-button>
			<el-button v-if="model!=3" :disabled="model==3" size="mini" type="primary" @click="doAddOrUpdateParam">确定</el-button>
		 </el-row>
	</Drawer>
	

	</div>
</template>

<script>
	const cid = 'DeviceFunCmdList';

	export default {
		name: cid,
		
		data() {
			return {
				by:1, //by 1:产品， 2： 设备
				funDef:{},
				fun:{},
				dev:{},
				cmd:{},
				
				model: 3,

				errorMsg:'',
				isLogin:true,
				plist: [],
				
				queryParams:{size:30,curPage:1,ps:{}},
				totalNum:0,
				
				cmdDrawer:{
					drawerStatus:false,
					drawerBtnStyle:{left:'0px',zindex:9999},
				},
			}
		},

		methods: {
			canShow(c) {
				return this.$jr.auth.updateAuth(c.createdBy) 
			},
			
			valChange(e){
				console.log(e)
				this.$forceUpdate()	
			},
			
			closeFunListDrawer(){
				this.$parent.$parent.closeFunListDrawer()
			},
			
			showArgDesc(ar){
				this.$notify.info({title: ar.label+'('+ar.name+')', message: ar.def.desc});
			},

			viewDetail(cmd){
				this.model = 3
				this.cmd = cmd
				this.parseCmdArgs()
				this.cmdDrawer.drawerBtnStyle.zindex=99999999
				this.cmdDrawer.drawerStatus = true;
			},
			
			addCmd(){
				if(!this.funDef.id) {
					this.$notify.error({
						title: '错误',
						message: "数据异常，需刷新重试"
					});
					return;
				}
				this.model = 2;
				this.cmd = {}
				this.parseCmdArgs()
				this.cmdDrawer.drawerBtnStyle.zindex=99999999
				this.cmdDrawer.drawerStatus = true;
			},
			
			updateCmd(o){
				if(!this.funDef.id) {
					this.$notify.error({
						title: '错误',
						message: "数据异常，需刷新重试"
					});
					return;
				}
				this.model = 1;
				this.cmd = o
				
				this.parseCmdArgs()
				
				this.cmdDrawer.drawerBtnStyle.zindex=99999999
				this.cmdDrawer.drawerStatus = true;
			},

			doQuery() {
				this.queryParams.curPage = 1
			    this.refresh();
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
					console.log(params)
					
					//listFunOpWithFunId
			        this.$jr.rpc.invokeByCode(1747941357, [params])
			            .then((resp)=>{
							console.log(resp)
			                if(resp.code == 0){
			                    if(resp.total == 0) {
									this.plist = [];
									this.totalNum = 0;
			                        //this.$notify.info({title: '提示',message: "查无数据"});
			                    } else {
									console.log(resp)
			                        this.plist = resp.data;
			                        this.totalNum = resp.total;
									if(this.totalNum) {
										this.plist.forEach(e=>{
											e.selected = !!e.productId
										})
									}
			                    }
			                } else {
								this.plist = [];
								this.totalNum = 0;
			                    this.$notify.error({title: '提示',message: r.msg});
			                }
			            }).catch((err)=>{
			            window.console.log(err);
			        });
			    } else {
			        self.roleList = [];
			    }
			},
			
			getQueryConditions() {
				this.queryParams.ps.funId = this.fun.funId
				this.queryParams.ps.by = this.by
			    return this.queryParams;
			},
			
			doAddOrUpdateParam() {
				
				if(!this.fun.funId) {
					this.$notify.error({
						title: '错误',
						message:  "当前指令所属功能无效"
					});
					return;
				}
				
				if (!this.checkParam(this.cmd)) {
					return
				}
				
				if (this.model == 1) {
					//update
					this.$jr.rpc.invokeByCode(1108373692, [this.cmd])
						.then((resp) => {
							this.refresh();
							if (resp.code == 0 ) {
								this.$notify.info({title: '提示',message: "更新成功"});
								this.cmdDrawer.drawerStatus = false;
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
					
					if(this.dev && this.dev.deviceId) {
						this.cmd.deviceId = this.dev.deviceId
					} 
					
					this.cmd.by = this.by
					this.cmd.defId = this.funDef.id
					this.cmd.funId = this.fun.funId
					this.cmd.productId = this.fun.productId
					//this.cmd.by = 2 //SRC_PRODUCE
					this.cmd.enable = true
					
					this.$jr.rpc.invokeByCode(-1475470624, [this.cmd])
						.then((resp) => {
							if (resp.code == 0 ) {
								this.$notify.info({title: '提示',message: "保存成功"});
								this.cmdDrawer.drawerStatus = false;
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
			
			async deleteCmd(c) {
				//getDeviceDef设备功能定义
				let r = await this.$jr.rpc.invokeByCode(-1512289002, [c.id])
				console.log(r)
				if(r.code != 0) {
					 this.$notify.error({title: '提示',message: r.msg});
					return
				}
				let idx = this.plist.findIndex(e=>e.id = c.id)
				if(idx>=0) {
					this.plist.splice(idx,1)
				}
			},
			
			async getFunDef() {
				//getDeviceDef设备功能定义
				let r = await this.$jr.rpc.invokeByCode(-319030539, [this.fun.defId])
				console.log(r)
				if(r.code != 0) {
					 this.$notify.error({title: '提示',message: r.msg});
					return
				}
				this.funDef = r.data
			},
			
			//by 1:产品， 2： 设备
			async loadCmdData(fun,by,dev){
				console.log("loadCmdData by: ",by)
				this.by = by
				this.plist = []
				this.totalNum = 0
				this.fun = fun
				this.dev = dev
				await this.getFunDef()
				this.refresh()
			},
			
			parseCmdArgs() {
				
				if(this.model == 2) {
					//add
					//单指令功能不能存在多于一条指令
					if((this.plist && this.plist.length > 0) && (!this.funDef.args || this.funDef.args.length == 0)) {
						this.$notify.error({title: '提示',message: "单指令功能，不能存在多于一条指令"});
						return;
					}
				}
				
				if (this.model == 2) {
					//add
					this.cmd.args = []
					if(this.funDef && this.funDef.args && this.funDef.args.length > 0) {
						for(let i = 0; i < this.funDef.args.length; i++) {
							let ca = this.funDef.args[i]
							let ar = {name: ca.name, val: ca.defVal, valType: ca.type, len: ca.maxLen, label:ca.label, def:ca}
							this.cmd.args.push(ar)
						}
					}
				} else {
					console.log(this.funDef)
					if(this.funDef && this.funDef.args && this.funDef.args.length > 0) {
						for(let i = 0; i < this.funDef.args.length; i++) {
							let ca = this.funDef.args[i]
							let ar = this.cmd.args.find(ae=> ae.name == ca.name)
							if(ar) {
								//ar.label = ca.label
								this.$set(ar,"label",ca.label)
								this.$set(ar,"def",ca)
							}
						}
					}
				}
				console.log(this.cmd)
			},

			checkParam(cmd) {
				if (this.model == 3) {
					this.$notify.error({
					 title: '错误',
						message: '非法操作'
					});
					return false
				}
				
				if (!cmd.name) {
					this.$notify.error({
					 title: '错误',
						message: '参数名称不能为空'
					});
					return false
				}
				
				if (!cmd.desc) {
					this.$notify.error({
					 title: '错误',
						message: '参数键值不能为空'
					});
					return false
				}
				
				return true
			},
			
		},

		async mounted () {
		    //this.refresh()
			this.plist = [];
			this.totalNum = 0;
		},
		
		beforeDestroy() {
		    this.$jr.auth.removeActListener(cid);
		},

	}
</script>

<style>
	.DeviceFunCmdList {
		border-top: 1px dotted lightgray;
		margin-top: 6px;
		padding-top: 10px;
		text-align: left;
	}
	
	.argLabel{
		color: blue;
		cursor: pointer;
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
