<template>
	<div class="RespView">
			<el-row class="blockSt" v-if="resp.data">
				<el-col class="title">{{"Resp"|i18n}}</el-col>
				<el-col>{{resp.data}}</el-col>
			</el-row>
			<el-row  class="blockSt"  v-if="resp.code != 0">
				<el-col  class="title"  :span="5">{{"RespCode"|i18n}}</el-col>
				<el-col :span="19">{{resp.code}}</el-col>
			</el-row>
			<el-row  class="blockSt" v-if="resp.msg">
				<el-col  class="title" :span="5">{{"RespMsg"|i18n}}</el-col>
				<el-col  :span="19">{{resp.msg}}</el-col>
			</el-row>
			<el-row  class="blockSt" v-if="resp.key">
				<el-col  class="title" :span="5">{{"Key"|i18n}}</el-col>
				<el-col  :span="19">{{resp.key}}</el-col>
			</el-row>
			<el-row  class="blockSt" v-if="resp.total">
				<el-col  class="title" :span="5">{{"total"|i18n}}</el-col>
				<el-col  :span="19">{{resp.total}}</el-col>
			</el-row>
			<el-row  class="blockSt" v-if="resp.pageSize">
				<el-col  class="title" :span="5">{{"Key"|i18n}}</el-col>
				<el-col  :span="19"resp.pageSize}}</el-col>
			</el-row>
			<el-row  class="blockSt" v-if="resp.curPage">
				<el-col  class="title" :span="5">{{"Key"|i18n}}</el-col>
				<el-col  :span="19">{{resp.curPage}}</el-col>
			</el-row>
			<el-row  class="blockSt"  v-if="resp.cost">
				<el-col  class="title" :span="5">{{"Cost"|i18n}}</el-col>
				<el-col  :span="19">{{resp.cost+'(毫秒)'}}</el-col>
			</el-row>
	
	</div>
</template>

<script>
	const cid = 'RespView';
	export default {
		name: cid,
		props: {
			resp: {
				type: Object,
				default: {}
			},
		},

		data() {
			return {
			}
		},

		methods: {
			
		},

		mounted() {

		},

		beforeDestroy() {

		},

	}
</script>

<style>
	.RespView {
		border-top: 1px dotted lightgray;
		margin-top: 6px;
		padding-top: 10px;
	}
	
	.blockSt {
		border-bottom:1px solid lightgray;
		margin-bottom: 10px;
	}
</style>
