<template>
    <div class="JApproveInfoEditor">

        <div v-if="isLogin && roleList && roleList.length > 0" style="position:relative;height:auto;margin-top:10px;">
            <table class="configItemTalbe" width="99%">
                <thead>
					<tr><td>{{'ID'|i18n}}</td><td>{{'InsId'|i18n}}</td><td>{{'NodeId'|i18n}}</td>
                    <td>{{'applyId'|i18n}}</td><td>{{'ApplyName'|i18n}}</td><td>{{'Status'|i18n}}</td>
					<td>{{'ApproverId'|i18n}}</td><td>{{'type'|i18n}}</td><td>{{'Remark'|i18n}}</td>
					<td>{{'UpdatedBy'|i18n}}</td><td>{{'CreatedTime'|i18n}}</td>
                    <td>{{"Operation"|i18n}}</td></tr>
                </thead>
                <tr v-for="c in roleList" :key="c._id">
                    <td>{{c.id}}</td> <td>{{c.insId}}</td> <td>{{c.nodeId}}</td>
					 <td>{{c.applyId}}</td> <td>{{c.applyName}}</td><td>{{statusMap[c.status]}}</td>
					  <td>{{c.approverId}}</td> <td>{{type2Map[c.type]}}</td> <td>{{c.remark}}</td>
                    <td>{{c.updatedBy}}</td><td>{{c.createdTime | formatDate(1)}}</td>
                    <td>
                        <a v-if="c.status!=1"  @click="openApproveInfoDrawer(c)">{{"View"|i18n}}</a>
                        <a v-if="c.status==1"  @click="updateApproveInfoDrawer(c)">{{"Update"|i18n}}</a>
                    </td>
                </tr>
            </table>
        </div>

        <div v-if="isLogin && roleList && roleList.length > 0" style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="queryParams.size" :current="queryParams.curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10, 30, 60,100]"></Page>
        </div>

        <div v-if="!isLogin || !roleList || roleList.length == 0">
            No permission!
        </div>

        <!--  查看 或 审批 -->
        <Drawer ref="approveInfo"  v-model="approveInfoDrawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
            <div>
				<el-row>
				  <el-button @click="update()" type="primary" :disabled="!updateModel">{{'submit'|i18n}}</el-button>
				</el-row>
					
				<el-row>
					<Label for="status">{{'status'|i18n}}</Label>
					<Select id="status" v-model="status" :disabled="!updateModel">
						<Option v-for="(val,key) in statusMap" :value="key">{{val|i18n}}</Option>
					</Select>
				</el-row>
				
				<el-row>
					<Label for="remark" :disabled="!updateModel">{{'remark'|i18n}}</Label>
					<Input id="remark" v-model="remark" :disabled="!updateModel"/>
				</el-row>
            </div>
            <div>{{errorMsg}}</div>
        </Drawer>


    </div>
</template>

<script>

 import cons from "@/rpc/constants"
 
	const STATUS_WAIT_APPROVE = 1;//待审批
	const STATUS_APPROVED = 2;//审批通过
	const STATUS_REJECT = 3;//审批拒绝
	const STATUS_BACK = 4;//审批驳回
	
	//流程类审批
	const  CFG_APPROVER_TYPE_FLOW = 1;
		
	//非流程类审批 实名认证
	const CFG_APPROVER_TYPE_REALNAME = 2;
		
	//申请特定角色，如配送
	const CFG_APPROVER_TYPE_ROLE = 3;
	
	const  type2Map={1:'流程类',2:'实名认证',3:'角色',}
	
	const statusMap = {'1':'待审批','2':'通过','3':'拒绝','4':'驳回',}
	
    const sn = 'cn.jmicro.security.apply.api.IApplyMngServiceJMsrv';
    const ns = cons.NS_SECURITY;
    const v = '0.0.1';

    const cid = 'JApproveInfoEditor';

    export default {
        name: cid,
        components: {
        },
        data() {
            return {
				type2Map,
                errorMsg:'',
                isLogin:false,
                roleList: [],

				queryParams:{size:10,curPage:1,},
				totalNum:0,

                updateModel:false,
                approveInfo : {},
				
				remark:'',
				status:"",
				statusMap:statusMap,
				
                approveInfoDrawer: {
                    drawerStatus : false,
                    drawerBtnStyle : {left:'0px',zindex:1000},
                },

                selOptions:{

                },

            }
        },

        methods: {

            updateApproveInfoDrawer(c) {
                this.updateModel = true;
                this.errorMsg = '';
                this.approveInfo = c;
                this.approveInfoDrawer.drawerStatus = true;
				this.remark = this.approveInfo.remark;
				this.status = this.approveInfo.status+"";
            },

            openApproveInfoDrawer(c){
				this.updateModel = false;
                this.errorMsg = '';
                this.approveInfo = c;
                this.approveInfoDrawer.drawerStatus = true;
				this.remark = this.approveInfo.remark;
				this.status = this.approveInfo.status+"";
            },

            update() {
				if(!this.updateModel) {
					return;
				}
				
				if(!this.status) {
					 this.$Message.success('状态不能为空')
					 return
				}
				
				if(this.status == STATUS_WAIT_APPROVE) {
					 this.$Message.success('不能选择待审批状态')
					 return
				}
				
				if(!this.remark || this.remark.length == 0) {
					 this.$Message.success('备注不能为空')
					 return
				}
				
                let self = this;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'approve', [this.approveInfo.id, parseInt(this.status), this.remark,""])
                    .then((resp)=>{
                    if(resp.code == 0 && resp.data) {
						self.approveInfoDrawer.drawerStatus = false;
						self.refresh()
                    } else {
                        self.$Message.success(resp.msg)
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
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
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'applyList', [params])
                        .then((resp)=>{
                            if(resp.code == 0){
                                if(resp.total == 0) {
                                    console.log("success");
                                }else {
                                    self.roleList = resp.data;
                                    self.totalNum = resp.total;
                                    self.curPage = 1;
                                }

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
            this.$jr.auth.addActListener(this.refresh);
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
    .JApproveInfoEditor{
        min-height: 500px;
    }

    .JApproveInfoEditor a {
        display: inline-block;
        margin-right: 8px;
    }

</style>