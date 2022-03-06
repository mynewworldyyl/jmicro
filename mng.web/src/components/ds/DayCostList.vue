<template>
	<div class="DayCostList">
		<table v-if="plist && plist.length > 0" class="configItemTalbe" width="99%">
		    <thead>
				<tr><td>{{"providerId"|i18n}}</td><td>{{'actId'|i18n}}</td>
		        <td>{{'apiId'|i18n}}</td><td>{{'bedate'|i18n}}</td>
				<td>{{'cost'|i18n}}(元)</td><td>{{'Count'|i18n}}(次)</td>
		        <td>{{"Operation"|i18n}}</td></tr>
		    </thead>
			<tr>
			    <td></td><td></td>
				 <td></td><td></td><td>{{totalCost}}</td><td>{{totalCnt}}</td>
				 <td>{{'Total'|i18n}}</td>
			</tr>
		    <tr v-for="c in plist" :key="'h'+c.ccv">
		        <td>{{c.clientId}}</td><td class="descCol">{{c.actId}}</td>
				 <td>{{c.apiId}}</td><td>{{c.bedate}}</td><td>{{c.cost}}</td>
				 <td>{{c.cnt}}</td>
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
				<tr><td>{{"apiId"|i18n}}</td><td>{{'reqId'|i18n}}</td>
		        <td>{{'code'|i18n}}</td><td>{{'msg'|i18n}}</td><td>{{'actId'|i18n}}</td>
				<td>{{'price'|i18n}}</td><td>{{'createdTime'|i18n}}</td>
				</tr>
		    </thead>
		    <tr v-for="c in dlist" :key="'h_'+c.req.reqId">
		        <td>{{c.req.apiId}}</td><td>{{c.req.reqId}}</td>
				 <td>{{c.resp.code}}</td> <td class="valCol">{{c.resp.msg}}</td><td>{{c.createdBy}}</td>
				 <td>{{c.price}}</td><td>{{c.createdTime|formatDate(2)}}</td>
		    </tr>
		</table>
		
		<div v-if="isLogin && dlist && dlist.length > 0" style="position:relative;text-align:center;">
		    <Page ref="pager" :total="dtotalNum" :page-size="dqueryParams.size" :current="dqueryParams.curPage"
		          show-elevator show-sizer show-total @on-change="dcurPageChange"
		          @on-page-size-change="dpageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
		</div>
	</Drawer>
	
	<div v-if="isLogin"  :style="queryDrawer.drawerBtnStyle" class="drawerJinvokeBtnStatu" @mouseenter="openQueryDrawer()"></div>
	
	<Drawer v-if="isLogin"   v-model="queryDrawer.drawerStatus" :closable="false" placement="left" :transfer="true"
	         :draggable="true" :scrollable="true" width="50">
	    <table id="queryTable">
	        <tr>
				<td>startTime</td>
	            <td>
					 <el-date-picker v-model="queryParams.ps.startTime" type="date" placeholder="选择日期"
					       value-format="yyyyMMdd">
					    </el-date-picker>
	             </td>
				 <td>endTime</td>
	            <td>
					<el-date-picker v-model="queryParams.ps.endTime" type="date" placeholder="选择日期"
					     value-format="yyyyMMdd">
					   </el-date-picker>
	            </td>
	        </tr>
	
	        <tr>
	            <td>ActId</td><td> <Input  v-model="queryParams.ps.actId"/></td>
	            <td>ClientId</td><td> <Input  v-model="queryParams.ps.clientId"/></td>
	        </tr>
			<tr>
			    <td>apiId</td><td> <Input  v-model="queryParams.ps.apiId"/></td>
				<td>By</td><td> 
					<el-select v-model="queryParams.ps.by" placeholder="请选择">
				    <el-option label="按日" value="d"></el-option>
					<el-option label="按月" value="m"></el-option>
					<el-option label="按年" value="y"></el-option>
				  </el-select>
				</td>
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

	const cid = 'dayCostList';

	export default {
		name: cid,

		data() {
			return {
				did:0,
				ccv:1,
				p: {},
				
				dlist: [],
				dqueryParams:{size:10,curPage:1,ps:{by:'d'}},
				dtotalNum:0,
				
				errorMsg:'',
				isLogin:false,
				plist: [],
				totalCost:0,
				totalCnt:0,
				
				queryParams:{size:10,curPage:1,ps:{by:'d'}},
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
				this.dqueryParams={size:10,curPage:1,ps:c},
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
			
			drefresh() {
			    let self = this;
			    this.isLogin = this.$jr.auth.isLogin();
			    if(this.isLogin) {
			        let params = this.dqueryParams;
					params.ps.code = 0
			        let self = this;
			        this.$jr.rpc.callRpcWithParams("cn.jmicro.api.ds.IDataApiJMSrv", ns, v, 'listHistory', [params])
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
			        this.$jr.rpc.callRpcWithParams(sn, ns, v, 'listDayCost', [params])
			            .then((resp)=>{
			                if(resp.code == 0){
			                    this.plist = resp.data;
			                    this.totalNum = resp.total;
								if(this.plist && this.plist.length > 0) {
									let ct = 0
									let cnt = 0
									this.plist.forEach(e => {
										ct += e.cost
										cnt += e.cnt
										e.ccv = ++this.ccv
										
									})
									this.totalCost = ct
									this.totalCnt = cnt
								}
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
	
	.DayCostList {
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
