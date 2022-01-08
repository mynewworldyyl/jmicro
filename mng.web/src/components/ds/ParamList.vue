<template>
	<div class="ParamList">
		<el-row>
			<el-col class="title" :span="2">{{type|i18n}}</el-col>
			<el-col :span="22"  v-if="!viewDetail">
				<el-button size="mini" @click="addParam()">{{'Add'|i18n}}</el-button>
			</el-col>
		</el-row>
		<el-row>
			<el-col :span="4">{{"name"|i18n}}</el-col>
			<el-col :span="2">{{"type"|i18n}}</el-col>
			<!-- <el-col :span="3">{{"belongTo"|i18n}}</el-col> -->
			<el-col :span="3">{{"isRequired"|i18n}}</el-col>
			<el-col :span="3">{{"defVal"|i18n}}</el-col>
			<el-col :span="6">{{"val"|i18n}}</el-col>
			<!-- <el-col :span="3">{{"desc"|i18n}}</el-col>-->
			<el-col v-if="!viewDetail" :span="3">{{"Operation"|i18n}}</el-col>
		</el-row>
		
		<div v-if="plist && plist.length > 0">
			<el-row v-for="p in plist" :key="'h_'+p.name">
				<el-col :span="4">{{p.name}}</el-col>
				<el-col :span="2">{{p.type}}</el-col>
				<!-- <el-col :span="3">{{p.belongTo}}</el-col>  -->
				<el-col :span="3">{{p.isRequired}}</el-col>
				<el-col :span="3">{{p.defVal}}</el-col>
				<el-col :span="6">{{p.val}}</el-col>
			<!--	<el-col class="descCol" :title="p.desc" :span="3">{{p.desc}}</el-col> -->
				<el-col v-if="!viewDetail" :span="3">
					<!-- <a @click="addOUpdateParam(p)">{{'Update'|i18n}}</a>&nbsp; -->
					<a @click="deleteParam(p)">{{'Delete'|i18n}}</a>
				</el-col>
			</el-row>
		</div>
		<div v-else>
			No data
		</div>

	<el-dialog title="参数" :visible.sync="addOrUpdateDialog" width="60%"
		:modal="false" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="true" :center="true"
		:append-to-body="true" :modal-append-to-body="true">
		
		<el-row>
			<el-col :span="4">{{"name"|i18n}}</el-col>
			<el-col :span="3">{{"type"|i18n}}</el-col>
			<!-- <el-col :span="3">{{"belongTo"|i18n}}</el-col> -->
			<el-col :span="3">{{"isRequired"|i18n}}</el-col>
			<el-col :span="3">{{"defVal"|i18n}}</el-col>
			<el-col :span="7">{{"val"|i18n}}</el-col>
			<!-- <el-col :span="2">{{"desc"|i18n}}</el-col> -->
			<el-col :span="2">{{"Operation"|i18n}}</el-col>
		</el-row>
		
		<div v-if="splist && splist.length > 0">
			<el-row v-for="p in splist" :key="'sh_'+p.name">
				<el-col :span="4">{{p.name}}</el-col>
				<el-col :span="3">{{p.type}}</el-col>
			<!--	<el-col :span="3">{{p.belongTo}}</el-col>    -->
				<el-col :span="3">{{p.isRequired}}</el-col>
				<el-col :span="3">{{p.defVal}}</el-col>
				<el-col :span="7">{{p.val}}</el-col>
			<!--	<el-col class="descCol"  :title="p.desc" :span="2">{{p.desc}}</el-col> -->
				<el-col :span="2">
					<a :disabled="p.selected" @click="selectParam(p)">{{'Select'|i18n}}</a>
				</el-col>
			</el-row>
		</div>

		
	</el-dialog>
	
	</div>
</template>

<script>
	import defCons from "./cons.js"

	const sn = defCons.sn;
	const ns = defCons.ns;
	const v = defCons.v;

	const cid = 'ParamList';

	export default {
		name: cid,
		props: {
			
			viewDetail: {
				type: Boolean,
				default: false
			},
			
			plist: {
				//type: Array,
				default: ()=>{return []}
			},
			type: {
				type: String, default: ""
			},
			defId: {
				type: Number, default: 0
			}
		},

		data() {
			return {
				p: {},
				addOrUpdateDialog: false,
				updateModel: false,
				splist:[]
			}
		},

		methods: {
			 handleClose(done) {
				this.$confirm('确认关闭？')
				  .then(_ => {
					done();
				  })
				  .catch(_ => {});
			  },
			
			addParam() {
				let qry = { size:1000,curPage:1, ps:{belongTo:this.type} }
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listParams', [qry])
				    .then((resp)=>{
				        if(resp.code == 0){
				            if(resp.total == 0) {
				                this.$notify.error({
				                	title: '错误',
				                	message: "无数据，请先创建参数"
				                });
				            } else {
				               this.addOrUpdateDialog = true
							   this.splist = resp.data;
							   if(this.splist && this.splist.length > 0 && this.plist && this.plist.length > 0) {
								  this.splist.forEach(p=>{
										let idx = this.plist.findIndex(it=>{return it.id == p.id})
										p.selected = idx >= 0						   
								   })
							   }  
				            }
				        } else {
				            this.$notify.error({
				            	title: '错误',
				            	message: resp.msg
				            });
				        }
				    }).catch((err)=>{
						this.$notify.error({
							title: '错误',
							message: jSON.stringify(err)
						});
				});
				
			},
			
			deleteParam(p) {
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'delDefParam', [this.defId, p.id])
					.then((resp) => {
						if (resp.code == 0 && resp.data) {
							this.plist.splice(this.plist.findIndex(it=>{return it.id == p.id}),1)
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

			selectParam(p) {
			
				if (!this.defId) {
					this.plist.push(this.p) //新建接口配置时增加参数
					return
				}

				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addDefParam', [this.defId, p.id])
					.then((resp) => {
						if (resp.code == 0 && resp.data) {
							p.selected = true
							this.plist.push(p)
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

		},

		mounted() {

		},

		beforeDestroy() {

		},

	}
</script>

<style>
	.ParamList {
		border-top: 1px dotted lightgray;
		margin-top: 6px;
		padding-top: 10px;
	}
	
	.title{
		font-weight: bold;
		font-size: 17px;
	}
	
	.descCol{
		overflow: hidden;
		text-overflow: ellipsis;
		flex-wrap: nowrap;
	}
</style>
