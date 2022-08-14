<template>
    <div class="JScheduleJobConfig">
        <table v-if="isLogin && jobList && jobList.length > 0" class="configItemTalbe" width="99%">
            <thead><tr><td>ID</td><td>{{'expression'|i18n}}</td>
                <td>{{'jobClassName'|i18n}}</td>
                <td>{{'ClientId'|i18n}}</td> <td>{{'desc'|i18n}}</td><td>{{'Ext'|i18n}}</td>
				<td>{{'Status'|i18n}}</td>
                <td style="width:126px">{{'Operation'|i18n}}</td></tr>
            </thead>
            <tr v-for="c in jobList" :key="c.id">
                <td>{{c.id}}</td><td>{{c.cron}}</td><td>{{c.jobClassName}}</td>
               <td>{{c.clientId}}</td>  <td>{{c.desc}}</td><td>{{c.ext}}</td>
				<td>{{statusMap[c.status] | i18n}}</td>
                <td>
					<a v-if="isLogin" @click="refleshJob(c)">{{'Refresh'|i18n}}&nbsp;</a>
                    <a v-if="isLogin" @click="viewDetail(c)">{{'Detail'|i18n}}&nbsp;</a>
                    <a v-if="isLogin" @click="updateJob(c)">{{'Update'|i18n}}&nbsp;</a>
                    <a v-if="isLogin" @click="deleteJob(c)">{{'Delete'|i18n}}&nbsp;</a>
					<a v-if="isLogin && c.status == 1" @click="parseJob(c)">{{'Pause'|i18n}}&nbsp;</a>
					<a v-if="isLogin && c.status == 2" @click="resumeJob(c)">{{'Resume'|i18n}}&nbsp;</a>
					<a v-if="isLogin" @click="executeJob(c)">{{'Execute'|i18n}}&nbsp;</a>
                </td>
            </tr>
        </table>
		
		<div v-if="isLogin  && jobList && jobList.length > 0"  style="position:relative;text-align:center;">
		    <Page ref="pager" :total="totalNum" :page-size="qry.size" :current="qry.curPage"
		          show-elevator show-sizer show-total @on-change="curPageChange"
		          @on-page-size-change="pageSizeChange" :page-size-opts="[10,20,50,100,150,200]"></Page>
		</div>
		
		<div v-if="!isLogin">No permission</div>
		<div v-if="isLogin  && (!jobList || jobList.length == 0)">
		    No data
		</div>

        <Drawer v-if="isLogin && job" v-model="drawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="50" @close="closeDrawer()" :mask-closable="false">
           
		    <div>
				<i-button @click="drawer.drawerStatus=false">{{'Close'|i18n}}</i-button>
				<i-button v-if="drawerModel!=3" @click="onAddOk()">{{'Confirm'|i18n}}</i-button>
			</div>
			
			<div style="color:red">{{errMsg}}</div>
			
			<!-- <Label for="enable">{{"Enable"|i18n}}</Label>
			<Checkbox id="enable"  v-model="job.enable"/><br/> -->
		   <Label for="clientId">{{"ClientID"|i18n}}</Label>
		   <Input :disabled="drawerModel==3 || !$jr.auth.isAdmin()" id="clientId" v-model="job.clientId"/>
		   
			<Label for="cron">Cron</Label>
			<Input :disabled="drawerModel==3" id="cron" v-model="job.cron"/>
			
			<Label for="jobClassName">{{"任务类名"|i18n}}</Label>
			<Input :disabled="drawerModel==3" id="jobClassName" v-model="job.jobClassName"/>
			
			<Label for="mcode">{{"MCode"|i18n}}</Label>
			<Input :disabled="drawerModel==3" id="mcode" v-model="job.mcode"/>

            <Label for="ext">ext</Label>
			<Input :disabled="drawerModel==3" id="ext"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
			       type="textarea" v-model="job.ext"/>

            <Label for="desc">{{'Desc'|i18n}}</Label>
            <Input :disabled="drawerModel==3" id="desc"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                   type="textarea" v-model="job.desc"/>

			<Label for="status">{{"Status"|i18n}}</Label>
			<Input :disabled="true" id="status" v-model="statusMap[job.status]"/>
			
			<Label for="jobId">{{"jobId"|i18n}}</Label>
		   <Input :disabled="true" id="jobId" v-model="job.id"/>
		   
            <Label for="createdTime">{{"CreatedTime"|i18n}}</Label>
            <Input id="createdTime" :disabled="true"  :value="getDateStr(job.createdTime)"/>

            <Label for="updatedTime">{{"UpdatedTime"|i18n}}</Label>
            <Input id="updatedTime" :disabled="true"  :value="getDateStr(job.createdTime)"/>

        </Drawer>

    </div>
</template>

<script>

    import {formatDate} from "../common/JFilters.js";
	import cons from "@/rpc/constants"
    import jmconfig from "@/rpcservice/jm"
    
	const sn = 'cn.jmicro.mng.api.IScheduleJobServiceJMSrv';
	const ns = cons.NS_MNG;
	const v = '0.0.1';
	
    const cid = 'scheduleJobConfig';
    export default {
        name: 'JScheduleJobConfig',
        data () {
            return {
                statusMap:{1:'正常',2:'暂停'},
                jobList:[],
                resMap:{},

                errMsg:'',
                drawerModel:0,//0无效，1:新增，2：更新，3：查看明细
                isLogin:false,

                depIds:[],

                job:{},

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                resStatus: jmconfig.RES_STATUS,

				totalNum:0,
				qry:{
					size:20,
					curPage:1,
					sortName:'updatedTime',
					order:2,
					ps:{},
				}
            }
        },

        watch:{
			
		},

        methods: {
			
			refleshJob(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'jobStatus', [c.id])
				    .then((resp) => {
				        if (resp.code != 0) {
				            this.errMsg = resp.msg;
				        }else {
							c.status = resp.data.status
						}
				    }).catch((err) => {
				    this.errMsg = err;
				});
			},
			
			//暂停任务
			parseJob(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'pause', [c.id])
				    .then((resp) => {
				        if (resp.code != 0) {
				            this.errMsg = resp.msg;
				        }else {
							c.status = 2
						}
				    }).catch((err) => {
				    this.errMsg = err;
				});
			},
			
			//唤醒任务
			resumeJob(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'resume', [c.id])
					.then((resp) => {
						if (resp.code != 0) {
							this.errMsg = resp.msg;
						}else {
							c.status = 1
						}
					}).catch((err) => {
					this.errMsg = err;
				});
			},
			
			//触发任务，运行一次
			executeJob(c){
				this.$jr.rpc.callRpcWithParams(sn, ns, v, 'start', [c.id])
					.then((resp) => {
						if (resp.code != 0) {
							this.errMsg = resp.msg;
						}else {
							  this.$Message.success("任务启动成功");
						}
					}).catch((err) => {
					this.errMsg = err;
				});
			},
			
			pageSizeChange(pageSize){
			    this.qry.size = pageSize;
			    this.qry.curPage = 1;
			    this.refresh();
			},
			
			curPageChange(curPage){
			    this.qry.curPage = curPage;
			    this.refresh();
			},
			
            getDateStr(time) {
                return formatDate(time,2);
            },

			refresh(){
				this.isLogin = this.$jr.auth.isLogin();
				this.act = this.$jr.auth.actInfo;
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'queryJobs', [this.qry])
                    .then((resp) => {
                        if (resp.code != 0) {
                            this.errMsg = resp.msg;
                            return;
                        }
						this.totalNum = resp.total;
                        this.$nextTick(() => {this.jobList = resp.data})
                        this.errMsg = "";
                    }).catch((err) => {
                    this.errMsg = err;
                });
            },
            
            viewDetail(pi) {
                this.drawerModel = 3;
                this.job = pi;
                pi.status = pi.status +'';
                this.drawer.drawerStatus = true;
            },

            closeDrawer(){
                this.drawer.drawerStatus = false;
                this.drawerModel = 0;
                this.resetjob();
            },

            addJob() {
                this.resetjob();
                this.drawer.drawerStatus = true;
                this.drawerModel = 1;
            },

            updateJob(j) {
                this.job = j;
                j.status = j.status +'';
                this.drawer.drawerStatus = true;
                this.drawerModel = 2;
            },

            resetjob() {
                this.errMsg = '';
                this.job = {
                    id : null,
                    status:'1',
                    desc:'',
                    clientId:this.$jr.auth.actInfo.clientId,
                    createdTime:new Date().getTime(),
                    updatedTime:new Date().getTime(),
                }
            },

            onAddOk(){

                this.errMsg = '';
                this.job.cron = this.job.cron.trim();
                if(this.job.cron.length == 0) {
                    this.errMsg = 'cron表达式不能为空';
                    return;
                }

                if(!this.job.jobClassName) {
                    this.errMsg = 'invalid'+ this.job.instanceNum;
                    return;
                }
				
				if(!this.job.mcode) {
				    this.errMsg = 'invalid'+ this.job.mcode;
				    return;
				}

                if(this.drawerModel == 2) {
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'updateJob', [this.job])
					.then((resp)=>{
						console.log(resp)
                        if( resp.code == 0 ) {
							this.closeDrawer();
                            this.errMsg = '';
                            this.$Message.success("更新成功");
                        }else {
                            this.errMsg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }else if(this.drawerModel == 1) {
                    this.$jr.rpc.callRpcWithParams(sn, ns, v, 'saveJob', [this.job])
					.then((resp)=>{
						console.log(resp)
                        if( resp.code == 0 ) {
							this.closeDrawer();
							this.$Message.success("保存成功");
                            this.jobList.push(resp.data);
                            this.errMsg = '';
                        } else {
                            this.$Message.error(resp.msg);
                            this.errMsg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                        this.$Message.error(err);
                    });
                }
            },

            deleteJob(job){
                this.$jr.rpc.callRpcWithParams(sn, ns, v, 'deleteJob', [job.id])
				.then((resp)=>{
                    if(resp.code == 0 ) {
						let idx = this.jobList.findIndex(e=>e.id == job.id)
                        if(idx >= 0) this.jobList.splice(idx,1);
                    }else {
                        this.$Message.error(resp.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    if(err && err.errorCode && err.msg) {
                        this.$Message.error(err.msg);
                    }else {
                        this.$Message.error(err);
                    }
                });
            },

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.refresh();
            this.$jr.auth.addActListener(this.refresh);
            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"ADD",label:"Add",icon:"ios-cog",call:this.addJob},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:this.refresh}]
             });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.$bus.$on('editorClosed',ec);

        },
    }
</script>

<style>
    .JScheduleJobConfig{
        height:auto;
    }

</style>