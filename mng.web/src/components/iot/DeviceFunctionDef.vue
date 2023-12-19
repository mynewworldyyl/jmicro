<template>
	<div>
		<el-row v-if="updateModel">
			<el-col :span="24">
			    <el-button size="mini" v-if="form.id>0" @click="addOUpdateDef()">{{'Update'|i18n}}</el-button>
				<el-button size="mini" v-else @click="addOUpdateDef()">{{'Save'|i18n}}</el-button>
			</el-col>
		</el-row>
		
		<el-row>
			<el-col class="label"  :span="3">{{'funName'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.funName"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'ver'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.ver"></el-input>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label"  :span="3">{{'labelName'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.labelName"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'grp'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.grp" allow-create default-first-option  filterable>
					<el-option v-for="v in grpList" :key="v" :value="v">{{v}}</el-option>
				</el-select>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label"  :span="3">{{'funType'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.funType"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'funDesc'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.funDesc"></el-input>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label"  :span="3">{{'showFront'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.showFront">
					<el-option value="true">{{'Yes'|i18n}}</el-option>
					<el-option value="false">{{'No'|i18n}}</el-option>
				</el-select>
			</el-col>
			<el-col class="label"  :span="3">{{'selfDefArg'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.selfDefArg">
					<el-option value="true">{{'Yes'|i18n}}</el-option>
					<el-option value="false">{{'No'|i18n}}</el-option>
				</el-select>
			</el-col>
		</el-row>
		
		<el-row>
			<el-col class="label" :span="3">{{'id'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.id" disabled></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'clientId'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.clientId"  placeholder="请选择">
					<el-option v-for="o in $jr.auth.getClients()" :key="'c_'+o" :value="o" :label="o"></el-option>
				</el-select>
			</el-col>
		</el-row>
		<el-row>
			<el-col  class="label" :span="3">{{'updatedBy'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.updatedBy" :disabled="true"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'createdBy'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.createdBy" :disabled="true"></el-input>
			</el-col>
		</el-row>
		
		<el-row>
			<el-col  class="label" :span="3">{{'createdTime'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.createdTime" :disabled="true"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'updatedTime'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.updatedTime" :disabled="true"></el-input>
			</el-col>
		</el-row>
		
		<table v-if="form.args && form.args.length > 0" class="configItemTalbe" width="99%">
		    <thead>
		        <td>{{'label'|i18n}}</td><td>{{'name'|i18n}}</td> <td>{{'type'|i18n}}</td>
				<td>{{'isRequired'|i18n}}</td><td>{{'defVal'|i18n}}</td><td>{{'maxLen'|i18n}}</td>
				<td>{{'desc'|i18n}}</td>
		        <td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in form.args" :key="'h_'+c.name">
				  <td>{{c.label}}</td> <td>{{c.name}}</td> <td>{{$jr.cons.PREFIX_TYPE_2DESC[c.type]}}</td>
				  <td>{{c.isRequired}}</td> <td>{{c.defVal}}</td>
				  <td>{{c.maxLen}}</td><td class="descCol">{{c.desc}}</td>
		        <td>
		           <a @click="viewParam(c)">{{'View'|i18n}}</a>&nbsp;
		           <a @click="updateParam(c)">{{'Update'|i18n}}</a>&nbsp;
		           <a @click="deleteParam(c)">{{'Delete'|i18n}}</a>
		        </td>
		    </tr>
		</table>
		
		<el-row v-if="updateModel">
			<el-col :span="24">
			    <!-- <el-button size="mini" v-if="form.id>0" @click="addOUpdateDef()">{{'Update'|i18n}}</el-button> -->
				<el-button size="mini" @click="addParam()">{{'AddParam'|i18n}}</el-button>
			</el-col>
		</el-row>
		
		<Drawer ref="defInfo"  v-model="defInfoDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
		            :draggable="true" :scrollable="true" width="50" :mask-closable="false" :mask="true">
			<el-row>
				<el-col :span="6">{{"Name"|i18n}}</el-col>
				<el-col><el-input v-model="p.name" :disabled="model!=2" /></el-col>
			</el-row>
			<el-row>
				<el-col :span="6">{{"label"|i18n}}</el-col>
				<el-col>
					<el-input v-model="p.label" :disabled="model==3" />
				</el-col>
			</el-row>
			 <el-row>
				<el-col :span="6">{{"defVal"|i18n}}</el-col>
				<el-col><el-input v-model="p.defVal" :disabled="model==3" /></el-col>
			 </el-row>
			 <el-row>
				<el-col :span="6">{{"Type"|i18n}}</el-col>
				<el-col>
					<el-select style="width:100%" v-model="p.type" :disabled="model==3">
						<el-option v-for="(v,k) in $jr.cons.PREFIX_TYPE_2DESC" 
							:key="'p_'+k" :value="k" :label="v"></el-option>
					</el-select>
				</el-col>
			 </el-row>
			 <el-row>
				<el-col :span="6">{{"isRequired"|i18n}}</el-col>
				<el-col>
					<el-select style="width:100%" v-model="p.isRequired" :disabled="model==3">
						<el-option value="true">{{'Yes'|i18n}}</el-option>
						<el-option value="false">{{'No'|i18n}}</el-option>
					</el-select>
				</el-col>
			 </el-row>
			 <el-row>
				<el-col :span="6">{{"maxLen"|i18n}}</el-col>
				<el-col><el-input v-model="p.maxLen" :disabled="model==3"/></el-col>
			 </el-row>
<!-- 		<el-row>
				<el-col :span="6">{{"isRequired"|i18n}}</el-col>
				<el-col><el-checkbox v-model="p.isRequired" :disabled="model==3"/></el-col>
			 </el-row> -->
			 <el-row>
				<el-col :span="6">{{"Desc"|i18n}}</el-col>
				<el-col><el-input type="textarea" autosize v-model="p.desc" :disabled="model==3"/></el-col>
			 </el-row>
			 <el-row>
				<el-button size="mini" @click="defInfoDrawer.drawerStatus = false">取消</el-button>
				<el-button  :disabled="model==3" size="mini" type="primary" @click="doAddOrUpdateParam">确定</el-button>
			 </el-row>
		</Drawer>
		
	</div>
</template>

<script>
	import i18n from "@/rpc/i18n"
	const cid = 'deviceFunctionDef';

	export default {

		name: cid,
		props: {
			updateModel: {
				type: Boolean,
				default: false
			},

			form: {
				type: Object,
				default: {},
				args:[],
			},
		},

		data() {
			return {
				defInfoDrawer: {
				    drawerStatus : false,
				    drawerBtnStyle : {left:'0px',zindex:1000},
				},
				
				grpList:[],
				p:{},
				model:3,//参数操作模式，查看，新增，更新
			}
		},

		methods: {
			
			viewParam(c){
				this.model = 3;
				this.p = c;
				//this.p.ptypeLabel = this.$jr.cons.PREFIX_TYPE_2DESC[this.p.type]
				this.p.type = parseInt(this.p.type)
				this.defInfoDrawer.drawerStatus = true;
			},
			
			updateParam(c){
				this.model = 1;
				this.p = c
				this.errorMsg = '';
				//this.p.ptypeLabel = this.$jr.cons.PREFIX_TYPE_2DESC[this.p.type]
				this.p.type = parseInt(this.p.type)
				this.defInfoDrawer.drawerStatus = true
				
			},
			
			addParam(){
				this.model = 2;
				this.p = {type:this.$jr.cons.PREFIX_TYPE_STRINGG};
				this.errorMsg = '';
				//this.p.ptypeLabel = this.$jr.cons.PREFIX_TYPE_2DESC[this.p.type]
				this.p.type = parseInt(this.p.type)
				this.defInfoDrawer.drawerStatus = true;	
			},
			
			curParamTypeLabel2Type(p) {
				for(let k in this.$jr.cons.PREFIX_TYPE_2DESC) {
					if(this.$jr.cons.PREFIX_TYPE_2DESC[k] == p.ptypelabel) {
						p.type = k
						break;
					}
				}
			},

			addOUpdateDef() {
				if (!this.updateModel) {
					this.$notify.error({
						title: '错误',
						message: "非更新模式"
					});
					return
				}

				if (!this.checkParam(this.form)) {
					return
				}
				
				if(this.form.args && this.form.args.length > 0) {
					this.form.args.forEach(e=>{
						//this.curParamTypeLabel2Type(e)
					})
				}

				if (this.form.id) {
					this.$jr.rpc.invokeByCode(-478632343, [this.form])
						.then((resp) => {	
							if (resp.code == 0 && resp.data) {
								this.$emit('success', this.form)
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
				} else {
					this.$jr.rpc.invokeByCode(784330088, [this.form])
					.then((resp) => {
						if (resp.code == 0 && resp.data) {
							this.addOrUpdateDialog = false;
							this.$emit('success', this.form)
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
			
			doAddOrUpdateParam() {
				if(!this.updateModel) {
					this.$notify.error({
					 title: '错误',
						message: '非更新模式'
					});
					return false
				}
				
				if(!this.form.id) {
					//新增状态下，不需调用此方法存存参数
					return;
				}
				
				if(!(this.model == 1 || this.model == 2)) {
					this.$notify.error({
					 title: '错误',
						message: '非更新模式,无需单独保存参数'
					});
					return false
					return;
				}

				if(!this.p.name) {
					this.$notify.error({
					 title: '错误',
						message: '参数名称不能为空'
					});
					return false
				}
				
				if (!this.p.type) {
					this.$notify.error({
					 title: '错误',
						message: '类型不能为空'
					});
					return false
				}
							
				if (!this.p.desc) {
					this.$notify.error({
						title: '错误',
						message: '参数描述不能为空'
					});
					return false
				}
			
				//this.curParamTypeLabel2Type(this.p)
			
				this.$jr.rpc.invokeByCode(-111597450, [this.form.id, this.model, this.p])
				.then((resp) => {
					this.defInfoDrawer.drawerStatus = false;
					if (resp.code == 0 && resp.data) {
						if(this.model == 2) {
							//新增
							this.form.args.push(this.p)
						}
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
			
			deleteParam(p) {
				
				this.$jr.rpc.invokeByCode(-111597450, [this.form.id, 4, p])
				.then((resp) => {
					if (resp.code == 0 && resp.data) {
						for(let i = 0; i < this.form.args.length; i++) {
							if(this.form.args[i].name == this.p.name) {
								this.form.args.splice(i,1);
								this.p = {}
								break;
							}
						}
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
				if (!this.updateModel) {
					this.$notify.error({
					 title: '错误',
						message: '非更新模式'
					});
					return false
				}
				
				if (!p.funName) {
					this.$notify.error({
					 title: '错误',
						message: '功能名称不能为空'
					});
					return false
				}
				
				if (!p.labelName) {
					this.$notify.error({
					 title: '错误',
						message: '标签名不能为空'
					});
					return false
				}
			
				if (!p.funDesc) {
					this.$notify.error({
						title: '错误',
						message: '描述字段不能为空'
					});
					return false
				}
				
				return true
			},

			i18n(key) {
				return i18n.get(key, key);
			}
		},

		mounted() {
			this.$jr.rpc.invokeByCode(-935954061, [])
			.then((resp) => {
				if (resp.code == 0 && resp.data) {
					this.grpList = resp.data;
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

		beforeDestroy() {

		},

	}
</script>

<style>
	.InterfaceDef {}
	.label {
		text-align: right;
		line-height: 40px;
		padding-right: 6px;
	}
</style>
