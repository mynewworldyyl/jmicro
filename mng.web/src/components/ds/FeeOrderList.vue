<template>
	<div class="FeeOrderList">
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"OrderId"|i18n}}</td><td>{{'ActId'|i18n}}</td>
		        <td>{{'Amount'|i18n}}(元)</td><td>{{'PayAmount'|i18n}}(元)</td>
				<td>{{'status'|i18n}}</td><td>{{"History"|i18n}}</td><td>{{'CreatedTime'|i18n}}</td>
				<td>{{'UpdatedTime'|i18n}}</td>
		        <td>{{"Operation"|i18n}}</td></tr>
		    </thead>
		    <tr v-for="c in plist" :key="'h'+c.ccv">
		         <td>{{c.id}}</td><td>{{c.actId}}</td>
				 <td>{{c.amount}}</td><td>{{c.payAmount}}</td><td>{{c.desc}}</td><td>{{c.history}}</td>
				 <td>{{c.createdTime|formatDate(2)}}</td><td>{{c.updatedTime|formatDate(2)}}</td>
		        <td>
		           <a @click="viewParam(c)">{{'Detail'|i18n}}</a>&nbsp;
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
		<table v-if="dlist && dlist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"actId"|i18n}}</td><td>{{'orderId'|i18n}}</td>
		        <td>{{'type'|i18n}}</td><td>{{'desc'|i18n}}</td><td>{{'Type'|i18n}}</td>
				<td>{{'amount'|i18n}}</td><td>{{'createdTime'|i18n}}</td>
				</tr>
		    </thead>
		    <tr v-for="c in dlist" :key="'h_'+c.id">
		        <td>{{c.actId}}</td><td>{{c.orderId}}</td>
				 <td>{{c.type}}</td> <td>{{c.desc}}</td><td>{{c.add?"收入":"支出"}}</td>
				 <td>{{c.amount}}</td><td>{{c.createdTime|formatDate(2)}}</td>
		    </tr>
		</table>
		
		<!--
		<div v-if="isLogin && dlist && dlist.length > 0" style="position:relative;text-align:center;">
		    <Page ref="pager" :total="dtotalNum" :page-size="dqueryParams.size" :current="dqueryParams.curPage"
		          show-elevator show-sizer show-total @on-change="dcurPageChange"
		          @on-page-size-change="dpageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
		</div>
		-->
	</Drawer>
	
	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<Drawer v-if="isLogin"   v-model="queryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
	         :draggable="true" :scrollable="true" width="50">
	    <table id="queryTable">
	        <tr>
				<td>{{'StartTime'|i18n}}</td>
	            <td>
					 <el-date-picker v-model="queryParams.ps.startTime" type="date" placeholder="选择日期"
					       value-format="yyyy-MM-dd">
					    </el-date-picker>
	             </td>
				 <td>{{'EndTime'|i18n}}</td>
	            <td>
					<el-date-picker v-model="queryParams.ps.endTime" type="date" placeholder="选择日期"
					     value-format="yyyy-MM-dd">
					   </el-date-picker>
	            </td>
	        </tr>
	
	        <tr>
	            <td>{{'ActId'|i18n}}</td><td> <Input  v-model="queryParams.ps.actId"/></td>
	            <td>{{'Name'|i18n}}</td><td> <Input  v-model="queryParams.ps.name"/></td>
	        </tr>
			<tr>
				<td>{{'Enable'|i18n}}</td><td> 
					<el-select v-model="queryParams.ps.enable" placeholder="请选择">
				    <el-option label="全部" value=""></el-option>
					<el-option label="启用" value="true"></el-option>
					<el-option label="禁用" value="false"></el-option>
				  </el-select>
				</td>
				 <td>{{'ApiId'|i18n}}</td><td> <Input  v-model="queryParams.ps.apiId"/></td>
			</tr>
			
	        <tr>
	            <td><i-button @click="doQuery()">{{'Query'|i18n}}</i-button></td><td></td>
	        </tr>
	    </table>
	</Drawer>

	</div>
</template>

<script>
	import defCons from "./cons.js"

	const sn = defCons.sn;
	const ns = defCons.ns;
	const v = defCons.v;

	const cid = 'feeOrderList';

	export default {
		name: cid,

		data() {
			return {
				did:0,
				ccv:1,
				p: {},
				
				dlist: [],
				dqueryParams:{size:100,curPage:1,ps:{}},
				dtotalNum:0,
				
				errorMsg:'',
				isLogin:false,
				plist: [],
				totalCost:0,
				totalCnt:0,
				
				queryParams:{size:10,curPage:1,ps:{enable:''}},
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
			getCnt() {
				return ++this.ccv
			},
			
			openQueryDrawer() {
			    this.queryDrawer.drawerStatus = true;
			    this.queryDrawer.drawerBtnStyle.zindex = 10000;
			    this.queryDrawer.drawerBtnStyle.left = '0px';
			},
			
			viewParam(c){
				this.p = c;
				this.defInfoDrawer.drawerStatus = true;
				
				this.dlist = [],
				this.dqueryParams={size:10,curPage:1,ps:{orderId:c.id}},
				this.dtotalNum=0,
				
				this.drefresh()
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
			
			dcurPageChange(curPage){
				this.dqueryParams.curPage = curPage
			    this.drefresh();
			},
			
			dpageSizeChange(pageSize){
				this.dqueryParams.size = pageSize
				this.dqueryParams.curPage = 1
			    this.drefresh();
			},
			
			changeEnable(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'changeActEnable', [c.actId])
				    .then((resp)=>{
				        if(resp.code != 0){
				           window.console.log(resp.msg);
				        }
						this.refresh()
				    }).catch((err)=>{
				    window.console.log(err);
				});
			},
			
			drefresh() {
			    let self = this;
			    this.isLogin = this.$jr.auth.isLogin();
			    if(this.isLogin) {
			        let params = this.dqueryParams;
					params.ps.code = 0
			        let self = this;
			        this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listActChangeRecord', [params])
			            .then((resp)=>{
			                if(resp.code == 0){
			                    this.dlist = resp.data;
			                    this.dtotalNum = resp.total;
			                } else {
			                    window.console.log(resp.msg);
			                }
			            }).catch((err)=>{
			            window.console.log(err);
			        });
			    }else {
			        this.dlist =  [];
			    }
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
			        this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listFeeOrder', [params])
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
			         this.plist =  [];
			    }
			},
			
			getQueryConditions() {
				for(let key in this.queryParams.ps) {
					if(!this.queryParams.ps[key]) {
						delete this.queryParams.ps[key]
					}
				}
			    return this.queryParams;
			},

		},

		mounted () {
		    this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
		    this.$jr.auth.addActListener(this.refresh);
		    this.refresh();
		    let self = this;
		    this.$bus.$emit("editorOpen",
		        {"editorId":cid, "menus":[
		            {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}
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
	
	.FeeOrderList {
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
