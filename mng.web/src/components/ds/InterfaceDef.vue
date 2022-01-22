<template>
	<div>
		<el-row v-if="updateModel">
			<el-col :span="24">
			    <el-button size="mini" v-if="form.id>0" @click="addOUpdateDef()">{{'Update'|i18n}}</el-button>
				<el-button size="mini" v-else @click="addOUpdateDef()">{{'Save'|i18n}}</el-button>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label" :span="3">{{'id'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.id" disabled></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'clientId'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.clientId" disabled></el-input>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label"  :span="3">{{'apiId'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.apiId" :disabled="!updateModel"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'Name'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.name" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
		<el-row>
			<!--
			<el-col class="label"  :span="3">{{'ReturnType'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.returnType" :disabled="!updateModel">
					<el-option value="async">{{'async'|i18n}}</el-option>
					<el-option value="sync">{{'sync'|i18n}}</el-option>
				</el-select>
			</el-col>
			-->
			<el-col  class="label" :span="3">{{'ReqEnc'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.reqEnc" :disabled="!updateModel">
					<el-option value="utf-8">{{'utf8'|i18n}}</el-option>
					<el-option value="gbk">{{'gbk'|i18n}}</el-option>
					<el-option value="gb2312">{{'gb2312'|i18n}}</el-option>
					<el-option value="8859-1">{{'8859-1'|i18n}}</el-option>
				</el-select>
			</el-col>
			<el-col  class="label" :span="3">{{'RespEnc'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.respEnc" :disabled="!updateModel">
					<el-option value="utf-8">{{'utf8'|i18n}}</el-option>
					<el-option value="gbk">{{'gbk'|i18n}}</el-option>
					<el-option value="gb2312">{{'gb2312'|i18n}}</el-option>
					<el-option value="8859-1">{{'8859-1'|i18n}}</el-option>
				</el-select>
			</el-col>
		</el-row>
		<el-row>
			<el-col  class="label" :span="3">{{'Url'|i18n}}</el-col>
			<el-col  :span="9">
				<el-input v-model="form.url" :disabled="!updateModel"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'UrlRefParamId'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.urlRefParamId" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
		<!--
		<el-row>
			<el-col  class="label" :span="3">{{'RespType'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.respType" :disabled="!updateModel">
					<el-option value="json">{{'json'|i18n}}</el-option>
				</el-select>
			</el-col>
		</el-row>
		-->
		<el-row>
			
			<el-col  class="label" :span="3">{{'Method'|i18n}}</el-col>
			<el-col :span="9">
				<el-select style="width:100%" v-model="form.method" :disabled="!updateModel">
					<el-option value="POST">{{'POST'|i18n}}</el-option>
				</el-select>
			</el-col>
			<el-col class="label"  :span="3">{{'SuccessCode'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.successCode" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label"  :span="3">{{'SuccessKey'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.successKey" :disabled="!updateModel"></el-input>
			</el-col>
			<el-col  class="label" :span="3">{{'FailMessageKey'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.failMessageKey" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
		<el-row>
			<el-col class="label"  :span="3">{{'ExtParams'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.extParams" :disabled="!updateModel"></el-input>
			</el-col>
			<el-col  class="label" :span="3">{{'TokenApiCode'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.tokenApiCode" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
		
		<el-row>
			<el-col class="label"  :span="3">{{'priceUnit'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.priceUnit" :disabled="!updateModel"></el-input>
			</el-col>
			<el-col  class="label" :span="3">{{'price'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.price" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
		
		<el-row>
			<el-col  class="label" :span="3">{{'freeCnt'|i18n}}</el-col>
			<el-col :span="9">
				<el-input v-model="form.freeCnt" :disabled="!updateModel"></el-input>
			</el-col>
			<el-col class="label"  :span="3">{{'Desc'|i18n}}</el-col>
			<el-col :span="9">
				<el-input type="textarea" autosize v-model="form.desc" :disabled="!updateModel"></el-input>
			</el-col>
		</el-row>
	</div>
</template>

<script>
	import i18n from "@/rpc/i18n"
	import defCons from "./cons.js"
	const cid = 'InterfaceDef';

	const sn = defCons.sn;
	const ns = defCons.ns;
	const v = defCons.v;

	export default {

		name: cid,
		props: {
			updateModel: {
				type: Boolean,
				default: false
			},

			form: {
				type: Object,
				default: {}
			},
		},

		data() {
			return {
				statusMap: defCons.statusMap,
			}
		},

		methods: {


			addOUpdateDef() {
				if (!this.updateModel) {
					this.$notify.error({
						title: '错误',
						message: "非法操作"
					});
					return
				}

				if (!this.checkParam(this.form)) {
					return
				}

				if (this.form.id) {
					this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateDef', [this.form])
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
					this.$jr.rpc.callRpcWithParams(sn, ns, v, 'addDef', [this.form])
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

			checkParam(def) {

				return true
			},

			i18n(key) {
				return i18n.get(key, key);
			}
		},

		mounted() {

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
