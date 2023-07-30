<template>
	<div class="DeviceFuntionList">

		<div>
			<el-button v-if="canShow()" :disabled="queryParams.ps.selectType==1" size="mini" type="primary" @click="changeQryType(1)">已选</el-button>
			<el-button v-if="canShow()"  :disabled="queryParams.ps.selectType==2" size="mini" type="primary" @click="changeQryType(2)">未选</el-button>
			<el-button v-if="canShow()"  :disabled="queryParams.ps.selectType==0" size="mini" type="primary" @click="changeQryType(0)">全部</el-button>
			<el-button size="mini" type="primary" @click="refresh()">查询</el-button>

			<el-button v-if="canShow()" style="float:right;margin-right: 8px;" size="mini" type="primary" @click="doAddOrUpdateParam()">提交</el-button>
		</div>
		
		
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"funName"|i18n}}</td><td>{{'labelName'|i18n}}</td>
				<td>{{'InterfaceId'|i18n}}</td><td>{{'FunId'|i18n}}</td><td>{{'clientId'|i18n}}</td>
				<td>{{'selfDefArg'|i18n}}</td><td>{{'showFront'|i18n}}</td>
		        <td>{{'productId'|i18n}}</td>
				<td  v-if="canShow()" >{{"Select"|i18n}}</td>
				<td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h_'+c.defId">
		        <td>{{c.funName}}</td><td>{{c.labelName}}</td><td>{{c.defId}}</td><td>{{c.funId}}</td>
				<td>{{c.clientId|i18n}}</td><td>{{c.selfDefArg}}</td><td>{{c.showFront}}</td>
				 <td>{{c.productId}}</td>
				 
				 <td v-if="canShow()" >
					<el-checkbox-group v-model="c.selected" size="small">
					      <el-checkbox-button :key="c.defId" @change="selectDef(c)" :checked="c.selected">{{c.selected ? "已选":"未选"}}
						  </el-checkbox-button>
					</el-checkbox-group>
				 </td>
		        <td>
		           <a v-if="c.funId" @click="cmdPanel(c)">{{'指令'|i18n}}</a>&nbsp;
		          <!-- 
				  <a v-if="c.productId" @click="select(c)">{{'Update'|i18n}}</a>&nbsp;
		           <a @click="deleteParam(c)">{{'Delete'|i18n}}</a> -->
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
		
	</div>
</template>

<script>
	
	const cid = 'DeviceFunctionList';

	export default {
		name: cid,
		props: {
			updateModel: {
				type: Boolean,
				default: false
			},
		},
		
		data() {
			return {
				by:1,//1:产品， 2： 设备
				dev:{},
				product:{},
				productId:0,
				
				p: {},
				addOrUpdateDialog: false,
				model: 3,
				
				defList:[],
				selDef:{},
				
				errorMsg:'',
				isLogin:true,
				plist: [],

				queryParams:{size:30,curPage:1,ps:{selectType:0}},
				totalNum:0,
				
				dels:{},
				adds:{},
			}
		},

		methods: {
			
			canShow() {
				return this.by==1 && this.$jr.auth.updateAuth(this.product.createdBy) //产品维度
				// || this.by==2 && this.$jr.auth.updateAuth(this.dev.createdBy) //设备维度
			},
			
			cmdPanel(fun){
				if(fun.funId) {
					this.$parent.$parent.openFunCmdList(fun, this.by, this.dev)
				} else {
					this.$notify.error({title: '错误',message: "非产品功能，请先为产品增加此功能，再增加指令"});
				}
			},

			changeQryType(selectType) {
				this.queryParams.ps.selectType = selectType
				this.refresh();
			},
			
			selectDef(vo) {
				console.log(vo)
				
				if(vo.selected) {
					if(!vo.productId) {
						this.adds[vo.defId] = true
					}
					delete this.dels[vo.funId]
				} else {
					if(vo.productId) {
						this.dels[vo.funId] = true
					}
					delete this.adds[vo.defId]
				}
				
				this.$forceUpdate()
			},
			
			doQuery() {
				this.queryParams.curPage = 1
			    this.refresh();
			},
		
			async doAddOrUpdateParam() {
				let addFuns = []
				for(let k in this.adds) {
					addFuns.push(k)
				}
				
				let delFuns = []
				for(let k in this.dels) {
					delFuns.push(k)
				}
				
				if(addFuns.length == 0 && delFuns.length == 0) {
					this.$notify.error({title: '错误',message: "无更新"});
					return;
				}
				
				console.log(addFuns,delFuns)
				//updateOrDelFuns
				let r = await this.$jr.rpc.invokeByCode(-327910439, [this.product.id, addFuns, delFuns])
				 
				this.refresh();
				 
				if(r.code != 0) {
					this.$notify.error({title: '错误',message: r.msg});
				}else{
					this.$notify.info({title: '提示',message: "更新成功"});
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
					
					//listProductFuns
			        this.$jr.rpc.invokeByCode(22613031, [params])
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
				if(this.by == 1) {
					if(!this.canShow()) {
						this.queryParams.ps.selectType = 1
					}
					this.queryParams.ps.productId = this.product.id
				} else {
					this.queryParams.ps.selectType = 1
					this.queryParams.ps.productId = this.dev.productId
				}
			    return this.queryParams;
			},
			
			loadFunListDataByPrd(prd){
				this.by = 1
				this.plist = [];
				this.totalNum = 0;
				this.product = prd
				this.refresh()
			},
			
			loadDataByDev(dev){
				this.by = 2
				this.dev = dev
				this.plist = [];
				this.totalNum = 0;
				this.refresh()
			}

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
