<template>
    <div class="JDeviceList">

        <div v-if="isLogin && deviceList && deviceList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
					<tr><td>{{'name'|i18n}}</td><td>{{'deviceId'|i18n}}</td><td>{{'product'|i18n}}</td>
					<td>{{'macAddr'|i18n}}</td><td>{{'LastLoginTime'|i18n}}</td><td>{{'Master'|i18n}}</td>
					<td>{{'status'|i18n}}</td><td>{{'actId'|i18n}}</td><td>{{'clientId'|i18n}}</td>
                    <td>{{"Operation"|i18n}}</td></tr>
                </thead>
                <tr v-for="c in deviceList" :key="'ide' + c.id">
                      <td>{{c.name}}</td><td>{{c.deviceId}}</td><td>{{prdMap[c.productId]}}</td>
					  <td class="descCol">{{c.macAddr}}</td><td>{{c.updatedTime | formatDate(2)}}</td>
					  <td>{{c.master==true ? '主设备':'从设备'}}</td><td>{{c.status}}</td>
					  <td>{{c.srcActId}}</td> <td>{{c.srcClientId}}</td> 
                    <td>
                        <a  @click="viewDev(c)">{{"View"|i18n}}</a>&nbsp;
                        <a  @click="updateDev(c)">{{"Update"|i18n}}</a>&nbsp;
						<!-- <a  @click="deleteDef(c.id)">{{"Delete"|i18n}}</a> -->
						<a  @click="funList(c)">{{"Funtions"|i18n}}</a>&nbsp;
						<a  @click="configDeviceActiveSource(c)">{{"接线"}}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && deviceList && deviceList.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="queryParams.size" :current="queryParams.curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

		<div v-if="!isLogin">
            No permission!
        </div>
		
        <div v-if="!deviceList || deviceList.length == 0">
            No data!
        </div>

	<!-- 弹出查询页面 -->
	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<Drawer ref="deviceInfoDrawer"  v-model="deviceDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
	         :styles="deviceDrawer.drawerBtnStyle" :draggable="true" :scrollable="true" width="80" :mask-closable="true" :mask="true">
		<el-row>
			<el-col :span="6">{{"产品分类"|i18n}}</el-col>
			<el-col>
				<el-select style="width:100%" v-model="selProductId" :disabled="model==3">
					<el-option key="'prd_0'" value="">请选择</el-option>
					<el-option v-for="(val,key) in prdMap" :key="'prd_'+key" :value="key" :label="val"></el-option>
				</el-select>
			</el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"主从设备"|i18n}}</el-col>
			<el-col>
				<el-select style="width:100%" v-model="dev.master" :disabled="model==3">
					<el-option key="m_true" value="false" label="从设备">从设备</el-option>
					<el-option key="m_false" value="true" label="主设备">主设备</el-option>
				</el-select>
			</el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"设备名称"|i18n}}</el-col>
			<el-col><el-input v-model="dev.name" :disabled="model==3" /></el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"描述"|i18n}}</el-col>
			<el-col><el-input v-model="dev.desc" :disabled="model==3" /></el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"类型"|i18n}}</el-col>
			<el-col><el-input v-model="dev.type" :disabled="model==3" /></el-col>
		</el-row>
		<el-row>
			<el-col :span="6">{{"所属分组"|i18n}}</el-col>
			<el-col><el-input v-model="dev.grpName" :disabled="model==3"/></el-col>
		</el-row>
		<el-row>
			<el-button size="mini" @click="deviceDrawer.drawerStatus = false">取消</el-button>
			<el-button  :disabled="model==3" size="mini" type="primary" @click="doAddOrUpdateParam">保存</el-button>
		</el-row>
		
		<div style="margin-top: 8px;" v-if="model != 2">
			
			<el-row>
				<el-col :span="6">{{"状态"|i18n}}</el-col>
				<el-col><el-input v-model="dev.status" :disabled="model==3" /></el-col>
			</el-row>
			<el-row>
				<el-col :span="6">{{"设备ID"|i18n}}</el-col>
				<el-col><el-input v-model="dev.deviceId" disabled/></el-col>
			</el-row>
			<el-row>
				<el-col :span="6">{{"物理地址"|i18n}}</el-col>
				<el-col><el-input v-model="dev.macAddr" disabled/></el-col>
			</el-row>
			
			 <el-row>
				<el-col :span="6">{{"所属账号"|i18n}}</el-col>
				<el-col><el-input v-model="dev.srcActId" disabled /></el-col>
			 </el-row>
			 
			 <el-row>
				<el-col :span="6">{{"所属租户"|i18n}}</el-col>
				<el-col><el-input v-model="dev.srcClientId" disabled/></el-col>
			 </el-row>
			
			  <el-row v-for="(val , key) in dev.devInfo">
				<el-col class="argLabel" :span="6">{{key}}</el-col>
			<el-col><el-input v-model="dev.devInfo[key]" disabled /></el-col>
			 </el-row> 
		</div>

	</Drawer>
	
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
	
	<!--  产品功能列表 -->
	<Drawer ref="devFunListInfo"  v-model="devFunListInfoDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
	        :draggable="true" :scrollable="true" width="80" :mask-closable="true" :mask="true" :z-index="1">
		    <DeviceFunctionList ref="devFunListInfoPanel"></DeviceFunctionList>		
	</Drawer>
	
	<!--  功能指令列表 -->
	<Drawer v-model="devFunCmdListDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
	        :draggable="true" :scrollable="true" width="60" :mask-closable="false" :mask="false"  :z-index="9999999">
		    <DeviceFunCmdList ref="devFunCmdListPanel"></DeviceFunCmdList>
	</Drawer>
	
	<Drawer v-model="activeSourceDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
	        :draggable="true" :scrollable="true" width="60" :mask-closable="false" :mask="false"  :z-index="9999999">
		    <DeviceActiveSourceList ref="activeSourcePanel" :masterMap="masterDeviceMap" :productFunsMap="productFunsMap"
				:deviceFunOpMap="deviceFunOpMap" :myDeviceMap="myDeviceMap"></DeviceActiveSourceList>
	</Drawer>
	
    </div>
</template>

<script>
 import DeviceActiveSourceList from "./DeviceActiveSourceList.vue"
 import DeviceFunCmdList from "./DeviceFunCmdList.vue"
 import DeviceFunctionList from "./DeviceFunctionList.vue"
 
 const cid = 'JDeviceList'
 
export default {
        name: cid,
        components: {DeviceFunCmdList,DeviceFunctionList,DeviceActiveSourceList},
        data() {
            return {
                errorMsg:'',
                isLogin:false,
                deviceList: [],
				
				myDeviceMap:{},//全部设备KV列表
				masterDeviceMap:{},//当前主设备
				productFunsMap:{},//产品功能列表
				deviceFunOpMap:{},//设备操作列表
				
				queryParams:{size:10,curPage:1,ps:{}},
				totalNum:0,

				model:3,
                selProductId:"",
                dev : {},
				
				prdMap:{},
				
                deviceDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },
				
				queryDrawer: {
				    drawerStatus:false,
				    drawerBtnStyle:{left:'0px',zindex:1000},
				},
				
				devFunListInfoDrawer:{
					drawerStatus:false,
					drawerBtnStyle:{left:'0px',zindex:1},
				},
				
				devFunCmdListDrawer:{
					drawerStatus:false,
					drawerBtnStyle:{left:'0px',zindex:9},
				},
				
				activeSourceDrawer: {
				    drawerStatus : false,
				    drawerBtnStyle : {left:'0px',zindex:1000},
				},
            }
        },

        methods: {
			
			configDeviceActiveSource(dev){
				this.$refs.activeSourcePanel.loadDataByDev(dev)
				this.activeSourceDrawer.drawerBtnStyle.zindex=1
				this.activeSourceDrawer.drawerStatus = true;
			},
			
			closeAsDrawer(){
				this.activeSourceDrawer.drawerStatus = false;
			},
			
			funList(dev){
				this.fdev = dev
				this.$refs.devFunListInfoPanel.loadDataByDev(dev)
				this.devFunListInfoDrawer.drawerBtnStyle.zindex=1
				this.devFunListInfoDrawer.drawerStatus = true;
				console.log(this.devFunListInfoDrawer)
			},
			
			openFunCmdList(fun,by,dev){
				this.$refs.devFunCmdListPanel.loadCmdData(fun,by,dev)
				this.devFunCmdListDrawer.drawerBtnStyle.zindex=99999999
				this.devFunCmdListDrawer.drawerStatus = true;
			},
			
			closeFunListDrawer(){
				this.devFunCmdListDrawer.drawerStatus = false;
			},

			doQuery() {
				this.queryParams.curPage = 1
			    this.refresh()
			},
			
			openQueryDrawer() {
			    this.queryDrawer.drawerStatus = true
			    this.queryDrawer.drawerBtnStyle.zindex = 10000
			    this.queryDrawer.drawerBtnStyle.left = '0px'
			},
			
			viewDev(c){
				this.selProductId = c.productId
				this.model = 3
				this.dev = c
				this.errorMsg = ''
				this.deviceDrawer.drawerStatus = true
			},
			
			addDev() {
				this.selProductId = ""
				this.model = 2
				this.dev = {}
				this.errorMsg = ''
				this.deviceDrawer.drawerStatus = true
			},
			
			updateDev(c){
				this.selProductId = c.productId
				this.model = 1
				this.dev = c
				this.errorMsg = ''
				this.deviceDrawer.drawerStatus = true
			},

			doAddOrUpdateParam() {
				if (!this.checkParam(this.dev)) {
					return
				}
			
				this.dev.productId = parseInt(this.selProductId)
				
				console.log(this.dev)
				if (this.model == 1) {
					//update
					this.$jr.rpc.invokeByCode(612799971, [this.dev])
						.then((resp) => {
							if (resp.code == 0) {
								this.deviceDrawer.drawerStatus = false;
								this.$notify.info({title: '提示',message:"更新成功"});
							} else {
								this.$notify.error({title: '错误',message: resp.msg || "未知错误"});
							}
						}).catch((err) => {
							this.$notify.error({title: '错误',message: err});
						});
				} else if(this.model == 2) {
					//add
					this.$jr.rpc.invokeByCode(987986757, [this.dev])
						.then((resp) => {
							if (resp.code == 0 && resp.data) {
								this.deviceDrawer.drawerStatus = false
								this.refresh()
								this.$notify.info({title: '提示',message:"保存存成功"});
							} else {
								this.$notify.error({
									title: '错误',
									message: resp.msg || "未知错误"
								});
							}
						}).catch((err) => {
							this.$notify.error({
								title: '错误',
								message: err
							});
						});
				}
			},
			
			checkParam(d) {
				if(!this.selProductId) {
					this.$notify.error({
						title: '错误',
						message: "缺少设备所属产品分类"
					});
					return false
				}
				
				if(!d.name) {
					this.$notify.error({
						title: '错误',
						message: "设备名称不能为空"
					});
					return false
				}
				return true
			},
			
			deleteDev(defId){
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

			testDev(c){
				
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
                    this.$jr.rpc.invokeByCode(-460237167, [params])
                        .then((resp)=>{
                            if(resp.code == 0){
                                self.deviceList = resp.data;
								self.deviceList.forEach(e=>{
									if(!e.productId) {
										e.productId = ""
									}else {
										e.productId = e.productId + ""
									}
								})
								console.log( self.deviceList)
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
                    self.deviceList = [];
                }
            },
			
			async getPrdMap() {
				//产品的KEYVAlue集合
				let r = await this.$jr.rpc.invokeByCode(-414142670, [])
				console.log("getPrdMap result: ",r)
				if(r.code != 0) {
					 this.$notify.error({title: '提示',message: r.msg});
					return
				}
				let pm = {}
				for(let k in r.data) {
					pm[k+''] = r.data[k]
				}
				this.prdMap = pm
				console.log(this.prdMap)
			},

            getQueryConditions() {
                return this.queryParams;
            },
        },

        async mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.refresh);
			await this.getPrdMap();
            this.refresh();
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid, "menus":[
                    {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh},
					{name:"Add",label:"Add",icon:"ios-cog",call:self.addDev},
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
    .JDeviceList{
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