<template>
    <div class="JRepository">
       <!-- <a @click="addNode()">ADD</a>&nbsp;&nbsp;
        <a @click="testArrayBuffer()">TEST</a>&nbsp;&nbsp;
        <a @click="refresh()">REFRESH</a>-->
        <div>{{errMsg}}</div>
        <div v-if="!resList || resList.length == 0 ">{{"NoData" | i18n("No data")}}</div>

        <table v-if="resList && resList.length > 0" class="configItemTalbe" width="99%">
            <thead><tr><td style="text-align:left;width:280px">{{"Name"|i18n}}</td>
                <td>{{"Group"|i18n}}</td><td>{{"Open"|i18n}}</td>
                <td>{{"Status"|i18n}}</td><td>{{"ID"|i18n}}</td>
                <td>{{"Size"|i18n}}</td><td v-if="actInfo && actInfo.isAdmin">{{"actId"|i18n}}</td>
                <td>{{"Operation"|i18n}}</td></tr></thead>
            <tr v-for="c in resList" :key="c.id">
                <td>{{c.name}}</td><td>{{c.group}}</td>
                <td>{{c.clientId==-1? i18nVal("Public") : i18nVal("Private")}}</td>
                <td>{{status[c.status]|i18n}}</td><td>{{c.id}}</td><td>{{c.size}}</td>
                <td v-if="actInfo && actInfo.isAdmin">{{c.createdBy}}</td>
                <td>
                    <a @click="viewDetail(c)">{{"Detail"|i18n}}</a>&nbsp;&nbsp;&nbsp;
                    <!--<a v-if="(c.createdBy==actInfo.id || actInfo.isAdmin) && !res0.fromMavenCenter"
                       @click="parseRemoteClass(c)">{{"parseRemoteClass"|i18n}}</a>&nbsp;-->&nbsp;&nbsp;
                    <a v-if="c.createdBy==actInfo.id || actInfo.isAdmin" @click="updateDetail(c)">{{"Update"|i18n}}</a>&nbsp;&nbsp;&nbsp;
                    <a v-if="c.createdBy==actInfo.id || actInfo.isAdmin" @click="deleteRes(c)">{{"Delete"|i18n}}</a>&nbsp;&nbsp;&nbsp;
                </td>
            </tr>
        </table>

        <div v-if="isLogin  && resList && resList.length > 0"  style="position:relative;text-align:center;">
            <Page ref="pager" :total="totalNum" :page-size="pageSize" :current="curPage"
                  show-elevator show-sizer show-total @on-change="curPageChange"
                  @on-page-size-change="pageSizeChange" :page-size-opts="[10,20,50,100,150,200]"></Page>
        </div>

        <Drawer  v-if="isLogin && res0"  v-model="drawer.drawerStatus" :closable="false" placement="right" :transfer="true"
                 :draggable="true" :scrollable="true" width="50" @close="closeDrawer()">
            <div v-if="editable()"><i-button @click="onAddOk()">{{'Confirm'|i18n}}</i-button></div>
            <p style="color:red">{{errMsg}}</p>
            <table>

                <tr><td>{{"Group"|i18n}}</td><td><Input :disabled="!editable()" v-model="res0.group"/></td></tr>
                <tr><td>{{"Artifact"|i18n}}</td><td><Input :disabled="true" v-model="res0.artifactId"/></td></tr>
                <tr><td>{{"Version"|i18n}}</td><td><Input   :disabled="true" v-model="res0.version"/></td></tr>
                <tr><td>{{"Status"|i18n}}</td><td>
                    <Select :disabled="!editable()" v-model="res0.status">
                        <Option :value="k" v-for="(v,k) in status" :key="v">{{v | i18n}}</Option>
                    </Select>
                </td>
                </tr>
                <tr><td>{{"Open"|i18n}}</td><td>
                    <Select :disabled="!editable()" :label-in-value="true" v-model="owner">
                        <Option :disabled="!(actInfo && actInfo.isAdmin)" :value="1">{{"Public" | i18n}}</Option>
                        <Option :value="2" >{{"Private" | i18n}}</Option>
                    </Select>
                </td></tr>

                <tr v-if="model != 0"><td>VALUE</td><td>
                    <input type="file" ref="resFile" @change="fileChange()"/>
                </td>
                </tr>

                <tr v-if="uploader.upStatis.onUpload"><td colspan="2">SIZE&FINISH:{{uploader.upStatis.totalSize}}/{{uploader.upStatis.finishSize}}&nbsp;&nbsp;
                    COST:{{uploader.upStatis.costTime}}&nbsp;&nbsp;SPEED:{{uploader.upStatis.uploadSpeed}}</td></tr>
                <tr v-if="uploader.upStatis.onUpload"><td colspan="2"><Progress :percent="uploader.upStatis.progressVal"></Progress></td></tr>

                <tr><td>{{"ModifyTime"|i18n}}</td><td>{{res0.modifiedTime | formatDate}}</td></tr>
                <tr><td>{{"UploadTime"|i18n}}</td><td>{{res0.uploadTime | formatDate}}</td></tr>
                <!--<tr><td>{{"Path"|i18n}}</td><td>{{res0.path}}</td></tr>-->
                <tr><td>{{"Name"|i18n}}</td><td>{{res0.name}}</td></tr>
                <tr><td>{{"downloadNum"|i18n}}</td><td>{{res0.downloadNum}}</td></tr>
                <tr><td>{{"ID"|i18n}}</td><td>{{res0.id}}</td></tr>
                <tr><td>{{"FromMavenCenter"|i18n}}</td><td>{{res0.fromMavenCenter}}</td></tr>

                <tr v-if="res0.depIds && res0.depIds.length > 0">
                    <td colspan="2">{{"Dependencies"|i18n}}
                        <a  @click="dependencyList(res0)">{{"Refresh"|i18n}}</a>&nbsp;
                    </td>
                </tr>
                <tr v-if="res0.depIds && res0.depIds.length > 0">
                    <td colspan="2">
                        <p v-for="d in res0.depIds "  :key="d.name" v-html="showDep(d)">
                        </p>
                    </td>
                </tr>

            </table>
        </Drawer>

        <div v-if="isLogin"  :style="query.drawerBtnStyle" class="queryDrawerStatu" @mouseenter="openQeuryDrawer()"></div>

        <Drawer v-if="isLogin"   v-model="query.drawerStatus" :closable="false" placement="left" :transfer="true"
                :draggable="true" :scrollable="true" width="50">
            <div><i-button @click="doQueryResource()">{{'Confirm'|i18n}}</i-button></div>
            <table id="queryTable">
                <tr>
                    <td>{{"Status"|i18n}}</td>
                    <td>
                        <Select v-model="queryParams.status">
                            <Option value="">{{"None" | i18n}}</Option>
                            <Option :value="k" v-for="(v,k) in status" :key="v">{{v | i18n}}</Option>
                        </Select>
                    </td>
                    <td>{{"Open"|i18n}}</td>
                    <td>
                        <Select  v-model="queryParams.clientId">
                            <Option value="">{{"None" | i18n}}</Option>
                            <Option value="-1" >{{"Public" | i18n}}</Option>
                            <Option v-for="v in dicts.clientIds" :key="v"  :value="v">{{v}}</Option>
                        </Select>
                    </td>
                </tr>
                <tr>
                    <td>{{"Group"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.group"/>
                    </td>
                    <td>{{"artifactId"|i18n}}</td>
                    <td>
                        <Input v-model="queryParams.artifactId"/>
                    </td>
                </tr>
                <tr>
                    <td>{{"version"|i18n}}</td>
                    <td>
                        <Input  v-model="queryParams.version"/>
                    </td>
                    <td>{{"Main"|i18n}}</td>
                    <td>
                        <Select v-model="queryParams.main">
                            <Option value="">{{"none" | i18n}}</Option>
                            <Option value="true">{{"true"|i18n}}</Option>
                            <Option value="false" >{{"false" | i18n}}</Option>
                        </Select>
                    </td>
                </tr>
            </table>
        </Drawer>

    </div>
</template>

<script>

    import {i18n} from "../common/JFilters.js";

    import rep from "@/rpcservice/repository"
	
    import {Constants} from "@/rpc/message"
    import jmconfig from "@/rpcservice/jm"
	
	 import io from "@/rpc/io.js"
    
    const cid = 'repository';

    export default {
        name: 'JRepository',
        data () {
            return {
				cid,
                resList:[],
                dicts:{},

                queryParams:{main:"true"},
                totalNum:0,
                pageSize:100,
                curPage:1,

                errMsg:'',

                uploader : new rep.Uploader(),

                isLogin:false,
                actInfo:null,
                owner:2,

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                query: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },

                status: jmconfig.RES_STATUS,

                res0 : this.resetRes(),
                model:0,
            }
        },

        watch:{
			
            "owner" : function(n){
                if(n == 1) {
                    this.res0.clientId = -1;
                }else {
                    this.res0.clientId = this.actInfo.id;
                }
            },
        },

        methods: {

            i18nVal(key) {
                return i18n(key);
            },

            parseRemoteClass(res) {
                if(res.fromMavenCenter) {
                    return;
                }
                let self = this;
                self.errMsg = '';
                rep.parseRemoteClass(res.id).then((resp)=>{
                    if(resp.code != 0) {
                        self.errMsg = resp.msg;
                    }else {
                        self.$Message.success("Successfully");
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            showDep(d) {
                if(typeof d == 'object') {
                    let msg = d.id + ":" + d.name + ":";
                    let sty = "";
                    if(d.status == 4) {
                        sty = "color:red";
                    }else if(d.status == 5) {
                        sty = "color:yellow";
                    }else if(d.status == 6) {
                        sty = "color:chocolate";
                    }else if(d.status == 3) {
                        sty = "color:green";
                    }

                    msg += "<span style='"+sty+"'>"+this.status[d.status]+"</span>";

                    return msg;
                }else {
                    return d;
                }
            },

            dependencyList(res) {
                let self = this;
                self.errMsg = "";
                rep.dependencyList(res.id).then((resp)=>{
                    if(resp.code == 0) {
                        res.depIds = [];
                        for(let i = 0; i < resp.data.length; i++) {
                            res.depIds.push(resp.data[i]);
                        }
                    }else {
                        self.errMsg = resp.msg;
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            waitingResList(res) {
                let self = this;
                self.errMsg = "";
                rep.waitingResList(res.id).then((resp)=>{
                    if(resp.code == 0) {
                        res.waitingRes = [];
                        for(let i = 0; i < resp.data.length; i++) {
                            res.waitingRes.push(resp.data[i]);
                        }
                    }else {
                        self.errMsg = resp.msg;
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            curPageChange(curPage){
                this.curPage = curPage;
                this.refresh();
            },

            pageSizeChange(pageSize){
                this.pageSize = pageSize;
                this.curPage = 1;
                this.refresh();
            },

            resetRes(){
                this.errMsg = "";

                let cid = this.actInfo ? this.actInfo.id:'-1';

                this.owner = 1;

                return this.res0  = {
                    name:"",
                    status:"1",
                    modifiedTime:0,
                    clientId:cid,
                    createdBy:cid,
                };
            },

            editable() {
                return this.model!=0 && (this.res0.createdBy==this.actInfo.id || this.actInfo.isAdmin);
            },

            fileChange(){
                if(!this.$refs.resFile.files || this.$refs.resFile.files.length ==0) {
                    return;
                }
                let file = this.$refs.resFile.files[0];
                let idx = file.name.indexOf("-");
                if(idx >0 ) {
                    let n = file.name;
                    let atName = n.substring(0,idx)
                    let ver = n.substring(idx+1,n.lastIndexOf("."));

                    if(!atName || !ver) {
                        this.errMsg = "文件名称与资源名称不匹配";
                        return;
                    }

                    if(this.model ==2) {
                        if(atName != this.res0.artifactId) {
                            this.errMsg = "文件名称与资源名称不匹配，只能在同一个资源中做修改";
                            return;
                        }
                        if(ver != this.res0.version) {
                            this.errMsg = "资源版本不匹配，只能在同一个资源中做修改";
                            return;
                        }
                    } else {
                        this.res0.artifactId = atName ;
                        this.res0.version = ver;
                    }
                }else {
                    this.errMsg = "选择文件命名不合法，必须符合Maven打包命名规范";
                    return;
                }

                this.res0.name = file.name;
                this.res0.modifiedTime = file.lastModified;
                if(!this.res0.name.endWith(".jar")) {
                    this.errMsg = "所选文件不是Jar文件";
                    return;
                }
                this.res0.fileChange = true;
                this.res0.resExtType = "jar";

            },

            viewDetail(r) {
                this.errMsg = "";
                this.res0 = r;
                this.res0.status += '';
                this.model = 0;
                this.drawer.drawerStatus = true;
                this.owner = r.clientId==-1?1:2;
            },

            openQeuryDrawer() {
                this.query.drawerStatus = true;
                this.query.drawerBtnStyle.zindex = 10000;
                this.query.drawerBtnStyle.left = '0px';
            },

            updateDetail(r) {
                this.errMsg = "";
                this.viewDetail(r)
                this.model = 2;
                this.owner = r.clientId==-1?1:2;
            },

            getQueryConditions() {
                let ps = {};
				console.log(this)
                if(typeof this.queryParams.status!= "undefined") {
                    ps.status = parseInt(this.queryParams.status);
                }

                if(typeof this.queryParams.clientId!= "undefined") {
                    ps.clientId = parseInt(this.queryParams.clientId);
                }

                if(this.queryParams.version) {
                    ps.version = this.queryParams.version;
                }
                if(this.queryParams.group) {
                    ps.group = this.queryParams.group;
                }
                if(this.queryParams.artifactId) {
                    ps.artifactId = this.queryParams.artifactId;
                }

                if(this.queryParams.main) {
                    ps.main = this.queryParams.main=='true';
                }
                return ps;
            },

            testArrayBuffer() {
                rep.addResourceData('test01',[0,1,2],0,3).then((resp)=>{
                    console.log(resp);
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            closeDrawer() {
                if(!this.res0) {
                    return;
                }
                this.drawer.drawerStatus = false;
                this.drawer.drawerBtnStyle.zindex = 100;
            },

            addNode(){
                this.errMsg = "";
                if(this.$refs.resFile && this.$refs.resFile.files && this.$refs.resFile.files.length > 0) {
                    this.fileChange();
                }
                this.resetRes();
                this.model = 1;
                this.drawer.drawerStatus = true;
            },

            doUpdate(){
                this.errMsg = "";
                let self = this;

                if(!this.res0.fileChange) {
                    rep.updateResource(this.res0,false)
                        .then((resp) =>{
                            if(resp.code==0) {
                                self.$Message.success("Successfully");
                            } else {
                                self.errMsg = resp.msg;
                            }
                        })
                        .catch((err) =>{
                            self.errMsg = "fail"+err;
                        });
                } else {
                    delete this.res0.fileChange;
					this.uploader.uploadFile(this.$refs.resFile.files[0],this.res0,
					(rst)=>{
						console.log(rst)
					})
                }
            },

            onAddOk(){

                if(!this.res0.group || this.res0.group.trim().length == 0) {
                    this.errMsg = "Group cannot be null";
                    return;
                }
				
                this.res0.group = this.res0.group.trim();
                this.errMsg = "";

                if(this.res0.waitingRes) {
                    delete this.res0.waitingRes;
                }
				
                if(this.res0.depIds) {
                    delete this.res0.depIds;
                }

                let self = this;

                if(this.model == 2) {
					//只变数据，没变内容
                    this.doUpdate();
                    return;
                }

                delete this.res0.fileChange;
				
				this.uploader.uploadFile(this.$refs.resFile.files[0], this.res0,
				(rst)=>{
					if(rst.code == io.UP_RES) {
						this.res0 = rst.res;
					}else if(rst.code == io.UP_PROGRESS){
						//更新进度条
					} else if(rst.code == io.UP_FINISH) {
						this.closeDrawer();
					}else {
						console.log(rst)
					}
				})
				
				/*
                this.getFileContent().then((buf) => {
                    self.res0.size =  buf.byteLength;
                    rep.addResource(self.res0)
					.then((resp)=>{
                        if(resp.code != 0) {
                            self.errMsg = resp.msg;
                            return;
                        }
                        self.res0 = resp.data;
                        self.initProgressData(buf);
                        self.uploadCurBlock();
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }).catch(err=>{
                    window.console.log(err);
                    self.$Message.error(err);
                });
				*/
				
            },

            deleteRes(res){
                let self = this;
                rep.deleteResource(res.id).then((rst)=>{
                    if(rst.code == 0 ) {
                        for(let i = 0; i < self.resList.length; i++) {
                            if(self.resList[i].name == res.name) {
                                self.resList.splice(i,1);
                                return;
                            }
                        }
                    }else {
                        self.$Message.error(rst.msg);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err || "Service not found");
                });
            },

            doQueryResource(){
                this.curPage = 1;
                this. refresh();
            },

            refresh(){
                let self = this;
                this.errMsg = "";
                this.isLogin = this.$jr.auth.isLogin();
                this.actInfo = this.$jr.auth.actInfo;

                if(!this.isLogin) {
                    this.resList = [];
                    return;
                }

                let qry = this.getQueryConditions();
                rep.getResourceList(qry,this.pageSize,this.curPage)
                    .then((resp)=>{
                    if(!resp || resp.code != 0 ) {
                        self.$Message.success("No data to show");
                        return;
                    }
                    self.totalNum = resp.total;
                    self.resList = resp.data;
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err || "Service not found");
                });
            },

            getDicts(){
                this.actInfo = this.$jr.auth.actInfo;
                self.errMsg = "";
                if(this.actInfo && this.actInfo.isAdmin) {
                    let self = this;
                    rep.queryDict().then((resp)=>{
                        if(resp.code == 0) {
                            self.dicts = resp.data;
                        }else {
                            self.errMsg = resp.msg;
                        }
                    }).catch((err)=>{
                        window.console.log(err);
                    });
                }

            },

            clearInvalidResourceFile() {
                let self = this;
                this.$jr.rpc.callRpcWithParams(rep.sn, rep.ns,
                    rep.v, 'clearInvalidResourceFile', [])
                    .then((resp)=>{
                        if(resp.code == 0){
                            self.$Message.success("Successfully submit task to clear invalid file");
                        } else {
                            window.console.log(resp.msg);
                        }
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },

            clearInvalidDbFile() {
                let self = this;
                this.$jr.rpc.callRpcWithParams(rep.sn, rep.ns,
                    rep.v, 'clearInvalidDbFile', [])
                    .then((resp)=>{
                        if(resp.code == 0){
                            self.$Message.success("Successfully submit task to clear invalid db resource data");
                        } else {
                            window.console.log(resp.msg);
                        }
                    }).catch((err)=>{
                    window.console.log(err);
                });
            },
        },

        mounted () {
            let self = this;
			
			console.log(self)
			
            this.isLogin = this.$jr.auth.isLogin();
            this.actInfo = this.$jr.auth.actInfo;

            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';

            let menus = [{name:"addNode",label:"Add Node",icon:"ios-cog",call: ()=>{ self.addNode(); }},
                {name:"Refresh",label:"Refresh",icon:"ios-cog",call:self.refresh}
            ];

            let clearFileMenu = {name:"ClearInvalidResourceFile",label:"ClearInvalidResourceFile",icon:"ios-cog",
                call:self.clearInvalidResourceFile};

            let clearDbFileMenu = {name:"clearInvalidDbFile",label:"clearInvalidDbFile",icon:"ios-cog",
                call:self.clearInvalidDbFile};

            if(this.actInfo && this.actInfo.isAdmin) {
                menus.push(clearFileMenu);
                menus.push(clearDbFileMenu);
            }

            this.$jr.auth.addActListener((type,ai)=>{
                self.refresh();
                if(type == Constants.LOGOUT) {
                    if(menus.length == 4){
                        menus.splice(2,1);
                        menus.splice(2,1);
                        this.$bus.$emit("menuChange", {"editorId":cid, "menus":menus});
                    }
                }else if(type == Constants.LOGIN) {
                    if(ai.isAdmin){
                        menus.push(clearFileMenu);
                        menus.push(clearDbFileMenu);
                        this.$bus.$emit("menuChange", {"editorId":cid, "menus":menus});
                    }
                }
            });

            this.$bus.$emit("editorOpen", {"editorId":cid, "menus":menus});

            let ec = ()=> {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }

            this.getDicts();

            this.$bus.$on('editorClosed',ec);

            this.refresh();
        },
    }
</script>

<style>
    .JRepository{
        height:auto;
    }

    .queryDrawerStatu{
        position: fixed;
        left: 0px;
        top: 30%;
        bottom: 30%;
        height: 39%;
        width: 1px;
        border-left: 1px solid lightgray;
        background-color: lightgray;
        border-radius: 3px;
        z-index: 1000000;
    }

</style>