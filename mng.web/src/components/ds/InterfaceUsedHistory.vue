<template>
	<div class="InterfaceUsedHistory">
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"apiId"|i18n}}</td><td>{{'reqId'|i18n}}</td>
		        <td>{{'code'|i18n}}</td><td>{{'msg'|i18n}}</td><td>{{'createdBy'|i18n}}</td>
				<td>{{'price'|i18n}}</td><td>{{'createdTime'|i18n}}</td>
		        <td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h_'+c.req.reqId">
		        <td>{{c.req.apiId}}</td><td>{{c.req.reqId}}</td>
				 <td>{{c.resp.code}}</td> <td class="valCol">{{c.resp.msg}}</td><td>{{c.createdBy}}</td>
				 <td>{{c.price}}</td><td>{{c.createdTime|formatDate(2)}}</td>
		        <td>
		           <a @click="viewParam(c)">{{'View'|i18n}}</a>
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

        <Drawer ref="defInfo"  v-model="defInfoDrawer.drawerStatus" :closable="true" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50" :mask-closable="true" :mask="true">
		 
		 <div v-if="p && p.req" class="paramContainer">
		 	<el-row v-for="(val,key) in p.req">
		 		<el-col class="title" :span="3">{{key|i18n}}</el-col>
		 		<el-col class="paramCol" :span="21">{{val}}</el-col>
		 	</el-row>
		 </div>
		 
		 <RespView v-if="p.resp" :resp="p.resp"></RespView>
		
	</Drawer>
	
	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<Drawer v-if="isLogin"   v-model="queryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
	         :draggable="true" :scrollable="true" width="50">
	    <table id="queryTable">
	        <tr>
				<td>startTime</td>
	            <td>
					 <el-date-picker v-model="queryParams.ps.startTime" type="date" placeholder="选择日期"
					       value-format="yyyy-MM-dd">
					    </el-date-picker>
	             </td>
				 <td>endTime</td>
	            <td>
					<el-date-picker v-model="queryParams.ps.endTime" type="date" placeholder="选择日期"
					     value-format="yyyy-MM-dd">
					   </el-date-picker>
	            </td>
	        </tr>
	
	        <tr>
	            <td>ActId</td><td> <Input  v-model="queryParams.ps.actId"/></td>
	            <td>ClientId</td><td> <Input  v-model="queryParams.ps.clientId"/></td>
	        </tr>
			<tr>
			    <td>Code</td><td> <Input  v-model="queryParams.ps.code"/></td>
				<td>状态</td><td> 
					<el-select v-model="queryParams.ps.success" placeholder="请选择">
					<el-option label="全部" value=""></el-option>
				    <el-option label="成功" value="true"></el-option>
					<el-option label="失败" value="false"></el-option>
				  </el-select>
				</td>
			</tr>
			
	        <tr>
	            <td><i-button @click="doQuery()">{{"Query"|i18n}}</i-button></td><td></td>
	        </tr>
	    </table>
	</Drawer>

	</div>
</template>

<script>
	import defCons from "./cons.js"
    import RespView from "./RespView.vue"
	const sn = "cn.jmicro.api.ds.IDataApiJMSrv";
	const ns = defCons.ns;
	const v = defCons.v;

	const cid = 'InterfaceUseHistory';

	export default {
		name: cid,
		components: {
			RespView
        },
		data() {
			return {
				p: {},
								
				errorMsg:'',
				isLogin:false,
				plist: [],
				
				queryParams:{size:10,curPage:1,ps:{}},
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
			
			openQueryDrawer() {
			    this.queryDrawer.drawerStatus = true;
			    this.queryDrawer.drawerBtnStyle.zindex = 10000;
			    this.queryDrawer.drawerBtnStyle.left = '0px';
			},
			
			viewParam(c){
				this.model = 3;
				this.p = c;
				this.p.cost = c.cost
				this.defInfoDrawer.drawerStatus = true;
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
			
			doQuery() {
				this.queryParams.curPage = 1
			    this.refresh();
			},
			
			refresh() {
			    let self = this;
			    this.isLogin = this.$jr.auth.isLogin();
			    if(this.isLogin) {
			        let params = this.getQueryConditions();
			        let self = this;
			        this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listHistory', [params])
			            .then((resp)=>{
			                if(resp.code == 0){
			                   this.plist = resp.data;
			                   this.totalNum = resp.total;
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
		    this.$jr.auth.addActListener(cid,this.refresh);
		    this.refresh();
		    let self = this;
		    this.$bus.$emit("editorOpen",
		        {"editorId":cid, "menus":[
		            {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
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
	

	.InterfaceUsedHistory {
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
		max-width: 120px;
		
	}
	
	.paramCol{
		overflow-x: hidden;
		max-height: 120px;
	}
</style>
