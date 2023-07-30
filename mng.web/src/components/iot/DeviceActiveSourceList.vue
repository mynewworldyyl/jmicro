<template>
	<div class="DeviceActiveSourceList">

		<div>
			<el-button size="mini" type="primary" @click="refresh()">查询</el-button>
			<el-button size="mini" type="primary" @click="closeDrawer()">关闭</el-button>
			<el-button size="mini" type="primary" @click="addCmd()">新增</el-button>
		</div>
		
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"name"|i18n}}</td><!-- <td>{{'desc'|i18n}}</td> -->
				<td>{{'opId'|i18n}}</td><td>{{'masterDevice'|i18n}}</td><td>{{'slaveDevice'|i18n}}</td>
				<td>{{'cmdId'|i18n}}</td><td>{{'srcType'|i18n}}</td><td>{{'ActId'|i18n}}</td>
				<td>{{'ClientId'|i18n}}</td>
				<td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h_'+c.id">
		        <td>{{c.name}}</td><!-- <td>{{c.desc}}</td> --><td>{{c.opId}}</td><td>{{myDeviceMap[c.masterDeviceId]}}</td>
				<td>{{myDeviceMap[c.slaveDeviceId]}}</td><td>{{c.cmdId}}</td><td>{{c.srcType}}</td><td>{{c.srcActId}}</td>
		        <td>{{c.clientId}}</td>
				<td>
		           <a  @click="viewDetail(c)">{{'详情'|i18n}}</a>&nbsp;
				   <a  @click="updateCmd(c)">{{'更新'|i18n}}</a>&nbsp;
				   <a  @click="deleteAs(c)">{{'删除'|i18n}}</a>&nbsp;
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
		
		<Drawer ref="asInfo"  v-model="asDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
		         :styles="asDrawer.drawerBtnStyle" :draggable="true" :scrollable="true" width="80" :mask-closable="false" :mask="false">
			
			<!--在主设备上增加从设备指令，这里选择从设备-->
			<el-row v-if="dev.master">
				<el-col :span="6">从设备</el-col>
				<el-col>
					<el-select style="width:100%" v-model="selMasterDeviceId" :disabled="model==3">
						<el-option v-for="(v,k) in myDeviceMap" :key="'p_'+k" :value="k" :label="v"></el-option>
					</el-select>
				</el-col>
			</el-row>
			
			<!--在从设备上增加从设备指令，这里选择主设备-->
			<el-row v-else>
				<el-col :span="6">主设备</el-col>
				<el-col>
					<el-select style="width:100%" v-model="selMasterDeviceId" :disabled="model==3">
						<el-option v-for="(v,k) in masterDeviceMap" :key="'p_'+k" :value="k" :label="v"></el-option>
					</el-select>
				</el-col>
			</el-row>
			
			<el-row>
				<el-col :span="6">{{dev.master?"从设备ID":"主设备ID"}}</el-col>
				<el-col><el-input v-model="selMasterDeviceId" disabled/></el-col>
			</el-row>
			
			<el-row>
				<el-col :span="6">名称</el-col>
				<el-col><el-input v-model="as.name" :disabled="model==3"/></el-col>
			</el-row>
			<el-row>
				<el-col :span="6">描述</el-col>
				<el-col><el-input v-model="as.desc" :disabled="model==3"/></el-col>
			</el-row>
			 <el-row>
				<el-col :span="6">信号源类型</el-col>
				<el-col><el-input v-model="as.srcType" :disabled="model==3" /></el-col>
			 </el-row>
			 <el-row>
				<el-col :span="6">信号ID</el-col>
				<el-col><el-input v-model="as.cmdId" :disabled="model==3" /></el-col>
			 </el-row>
			 
			 <el-row>
			 	<el-col :span="6">接口功能</el-col>
			 	<el-col>
			 		<el-select style="width:100%" :loading="loadingFun" v-model="selFunId" :disabled="model==3" placeholder="请选择">
			 			<el-option v-for="f in productFunsMap[dev.productId]" 
			 				:key="'f_'+f.funId" :value="f.funId" :label="f.labelName">{{f.labelName}}</el-option>
			 		</el-select>
			 	</el-col>
			 </el-row>
			 
			 <el-row>
			 	<el-col :span="6">执行指令</el-col>
			 	<el-col>
			 		<el-select @focus="loadDeviceFunOpMap()" style="width:100%" v-model="selOpId" :disabled="model==3 || !selFunId" placeholder="请选择">
			 			<el-option v-for="o in deviceFunOpMap[selFunId]" 
			 				:key="'fm_'+o.id" :value="o.id" :label="o.name"></el-option>
					</el-select>
			 	</el-col>
			 </el-row>

			<el-row>
				<el-col :span="6">{{"ClientId"|i18n}}</el-col>
				<el-col>
					<el-select style="width:100%" v-model="as.clientId" :disabled="model==3" placeholder="请选择">
						<el-option v-for="o in $jr.auth.getClients()" :key="'c_'+o" :value="o" :label="o"></el-option>
					</el-select>
				</el-col>
			 </el-row>
		 
			 <el-row>
				<el-button size="mini" @click="asDrawer.drawerStatus = false">关闭</el-button>
				<el-button  :disabled="model==3" size="mini" type="primary" @click="doAddOrUpdateParam">确定</el-button>
			 </el-row>

			<el-row>
				<el-col :span="6">{{dev.master? "主设备ID" :'从设备ID'}}</el-col>
				<el-col><el-input v-model="dev.deviceId" disabled/></el-col>
			</el-row>

			<el-row>
				<el-col :span="6">{{dev.master? "主设备名称" :'从设备名称'}}</el-col>
				<el-col><el-input v-model="dev.name" disabled/></el-col>
			</el-row>
			
			<el-row>
				<el-col :span="6">{{"接口ID"}}</el-col>
				<el-col><el-input v-model="as.defId" disabled/></el-col>
			</el-row> 
			
			<el-row>
				<el-col :span="6">{{"srcActId"|i18n}}</el-col>
				<el-col><el-input v-model="as.srcActId" disabled/></el-col>
			</el-row>
			<el-row>
				<el-col :span="6">{{"ClientId"|i18n}}</el-col>
				<el-col><el-input v-model="as.clientId" disabled/></el-col>
			</el-row>
		</Drawer>
		
	</div>
</template>

<script>
	
	const cid = 'DeviceActiveSourceList';

	export default {
		name: cid,
		props: {
			myDeviceMap:{
				type: Object,
				default: ()=>{
					return {}
				}
			},
			
			masterDeviceMap: {
				type: Object,
				default: ()=>{
					return {}
				}
			},
			
			//funID到fun名称焦合
			productFunsMap: {
				type: Object,
				default: ()=>{
					return {}
				}
			},
			
			deviceFunOpMap: {
				type: Object,
				default: ()=>{
					return {}
				}
			}
		},
		
		data() {
			return {
				by:1,//1:产品， 2： 设备
				dev:{},
				
				as: {},
				model: 3,
				
				errorMsg:'',
				isLogin:true,
				plist: [],

				queryParams:{size:30,curPage:1,ps:{selectType:0}},
				totalNum:0,
				
				asDrawer:{
					drawerStatus:false,
					drawerBtnStyle:{left:'0px',zindex:9999},
				},

				selMasterDeviceId:"",
				selOpId:'',
				selFunId:'',
				
				loadingFun:false,
			}
		},

		methods: {
			
			closeDrawer(){
				this.$parent.$parent.closeAsDrawer()
			},
			
			async resetData() {
				
				if(this.dev.master) {
					this.selMasterDeviceId = this.as.slaveDeviceId
				} else {
					this.selMasterDeviceId = this.as.masterDeviceId
				}
			
				this.selOpId = this.as.opId
				this.selFunId = this.as.funId
				
				await this.loadProductFunsMap();
				
				if(this.as.funId) {
					await this.loadDeviceFunOpMap();
				}
				
			},
			
			viewDetail(as){
				this.model = 3
				this.as = as
				this.resetData()
				this.asDrawer.drawerBtnStyle.zindex=99999999
				this.asDrawer.drawerStatus = true;
			},
			
			addCmd(){
				this.model = 2;
				this.as = {}
				this.resetData()
				this.asDrawer.drawerBtnStyle.zindex=99999999
				this.asDrawer.drawerStatus = true;
			},
			
			updateCmd(o){
				this.model = 1;
				this.as = o
				this.resetData()
				this.asDrawer.drawerBtnStyle.zindex=99999999
				this.asDrawer.drawerStatus = true;
			},
			
			async deleteAs(c){
				//delActiveSource
				let r = await this.$jr.rpc.invokeByCode(825430292, [c.id])
				if(r.code == 0) {
					let idx = this.plist.findIndex(e=>e.id == c.id)
					if(idx >= 0) {
						this.plist.splice(idx,1)
					}
					this.$notify.info({title: '提示',message: "删除成功"})
				} else {
					this.$notify.error({title: '提示',message: r.msg || "删除失败"})
				}
			},
			
			doQuery() {
				this.queryParams.curPage = 1
			    this.refresh();
			},
		
			doAddOrUpdateParam() {
			
				if(!this.selMasterDeviceId) {
					this.$notify.error({title: '提示',message: "需选择一个设备"});
					return;
				}
				
				if(!this.as.name) {
					this.$notify.error({title: '提示',message: "名称不能为空"});
					return;
				}

				// if(!this.as.srcType) {
				// 	this.$notify.error({title: '提示',message: "触发源类型不能为空"});
				// 	return;
				// }

				// if(!this.as.cmdId) {
				// 	this.$notify.error({title: '提示',message: "设备信号标识"});
				// 	return;
				// }

				if(!this.selOpId) {
					this.$notify.error({title: '提示',message: "指令操作码无效"});
					return;
				}

				//updateOrDelFuns
				//let r = await this.$jr.rpc.invokeByCode(-1084194638, [this.as])
				
				if(this.dev.master) {
					this.as.masterDeviceId = this.dev.deviceId 
					this.as.slaveDeviceId = this.selMasterDeviceId
				} else {
					this.as.masterDeviceId = this.selMasterDeviceId
					this.as.slaveDeviceId = this.dev.deviceId
				}
				
				this.as.opId = this.selOpId
				
				if (this.model == 1) {
					//update
					this.$jr.rpc.invokeByCode(-69606124, [this.as])
						.then((resp) => {
							this.refresh();
							if (resp.code == 0 ) {
								this.$notify.info({title: '提示',message: "更新成功"});
								this.asDrawer.drawerStatus = false;
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
					this.$jr.rpc.invokeByCode(-1084194638, [this.as])
						.then((resp) => {
							if (resp.code == 0 ) {
								this.$notify.info({title: '提示',message: "保存成功"});
								this.asDrawer.drawerStatus = false;
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
					
					//listActiveSources
			        this.$jr.rpc.invokeByCode(422210590, [params])
			            .then((resp)=>{
			                if(resp.code == 0){
			                    if(resp.total == 0) {
									this.plist = [];
									this.totalNum = 0;
			                        this.$notify.info({title: '提示',message: "查无数据"});
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
				if(this.dev.master) {
					//主设备查所属从设备指令
					this.queryParams.ps.masterDeviceId = this.dev.deviceId
					delete this.queryParams.ps.slaveDeviceId
				} else {
					//从设备查自己的指令
					this.queryParams.ps.slaveDeviceId = this.dev.deviceId
					delete this.queryParams.ps.masterDeviceId
				}
			    return this.queryParams;
			},
			
			async loadDataByDev(dev){
				console.log(dev)
				
				this.selMasterDeviceId = ""
				this.selOpId = ''
				this.selFunId = ''
				this.loadingFun = false
				
				this.by = 2
				this.dev = dev
				this.plist = [];
				this.totalNum = 0;
				
				//第一次进来会加载masterDeviceMap，并存放在父控制中，起到缓存作用
				
				await this.loadMasterDevice(false, this.myDeviceMap)
				await this.loadMasterDevice(true, this.masterDeviceMap)
				
				for(let k in this.masterDeviceMap) {
					this.myDeviceMap[k] = this.masterDeviceMap[k]
				}

				this.refresh()
			},
			
			async loadMasterDevice(master, map){
				
				let loaded = false;
				for(let k in map) {
					loaded = true;
					break;
				}
				
				if(loaded) {
					return
				}
				
				let r = await this.$jr.rpc.invokeByCode(-2138649681, [master])
				console.log(r)
				if(r.code != 0) {
					this.$notify.error({title: '提示',message: r.msg || '加载主设备数据错误'})
					return
				}
				
				if(!r.data || r.data.length == 0) {
					map["0"] = "" //防止重复加载无效请求
					console.log("无主设备数据")
					return
				}
				
				for(let k in r.data) {
					map[k] = r.data[k]
				}
			},
			
			async loadProductFunsMap(){
				
				if(!this.dev.productId) {
					console.log(this.as)
					this.$notify.error({title: '提示',message: '无效产品'})
					return
				}
				
				if(this.productFunsMap[this.dev.productId]) {
					return true
				}
				
				this.loadingFun = true
				let r = await this.$jr.rpc.invokeByCode(-1606273333, [{size:500,curPage:1,ps : {productId:this.dev.productId}} ])
				this.loadingFun = false
				console.log(r)
				if(r.code != 0) {
					this.$notify.error({title: '提示',message: r.msg || '加载产品功能错误'})
					return
				}
				
				if(!r.data || r.data.length == 0) {
					console.log("产器无关联功能")
					return
				}
				
				this.productFunsMap[this.dev.productId] = r.data
			},
			
			async loadDeviceFunOpMap(){
				
				if(!this.selFunId) {
					this.$notify.error({title: '提示',message:  '当前选择接口无效'})
					return
				}
				
				if(this.deviceFunOpMap[this.selFunId]) {
					return true
				}
				
				let r = await this.$jr.rpc.invokeByCode(74602086, [{size:500,curPage:1, ps : {funId:this.selFunId}}])
				console.log(r)
				if(r.code != 0) {
					this.$notify.error({title: '提示', message: r.msg || '加载接口操作列表失败'})
					return
				}
				
				if(!r.data || r.data.length == 0) {
					console.log("当前接口无操作指令")
					return
				}
				
				this.deviceFunOpMap[this.selFunId] = r.data
				
				this.$forceUpdate()
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
	.DeviceActiveSourceList {
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
