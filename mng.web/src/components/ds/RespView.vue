<template>
	<div>
		<div  class="RespView">
			<el-row class="blockSt">
				<el-col class="title">{{"Resp"|i18n}}</el-col>
				<el-col>{{resp.data}}</el-col>
			</el-row>
			<el-row  class="blockSt">
				<el-col  class="title"  :span="5">{{"RespCode"|i18n}}</el-col>
				<el-col :span="19">{{resp.code}}</el-col>
			</el-row>
			<el-row  class="blockSt">
				<el-col  class="title" :span="5">{{"RespMsg"|i18n}}</el-col>
				<el-col  :span="19">{{resp.msg}}</el-col>
			</el-row>
			<el-row  class="blockSt">
				<el-col  class="title" :span="5">{{"Key"|i18n}}</el-col>
				<el-col  :span="19">{{resp.key}}</el-col>
			</el-row>
			<el-row  class="blockSt">
				<el-col  class="title" :span="5">{{"total"|i18n}}</el-col>
				<el-col  :span="19">{{resp.total}}</el-col>
			</el-row>
			<el-row  class="blockSt">
				<el-col  class="title" :span="5">{{"Key"|i18n}}</el-col>
				<el-col  :span="19">{{resp.pageSize}}</el-col>
			</el-row>
			<el-row  class="blockSt">
				<el-col  class="title" :span="5">{{"Key"|i18n}}</el-col>
				<el-col  :span="19">{{resp.curPage}}</el-col>
			</el-row>
		</div>
		
	
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
		
		watch:{
			"resp":function(nv){
				if(nv && nv.code == 0) {
					this.bdata = nv.data
					this.body = null
				}else {
					this.bdata = null
					this.body = JSON.stringify(nv)
				}
			}
		},
		
		beforeCreate() {
			this.body = null
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
