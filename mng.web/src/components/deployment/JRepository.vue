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
                <td>{{c.name}}</td><td>{{c.group}}</td><td>{{c.clientId==-1}}</td>
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
                    <Select :disabled="!editable()" :label-in-value="true" v-model="res0.clientId">
                        <Option :disabled="!(actInfo && actInfo.isAdmin)" :value="-1">{{"Public" | i18n}}</Option>
                        <Option :value="owner" >{{"Private" | i18n}}</Option>
                    </Select>
                </td></tr>

                <tr v-if="model != 0"><td>VALUE</td><td><input type="file" ref="resFile" @change="fileChange()"/></td></tr>

                <tr v-if="upStatis.onUpload"><td colspan="2">SIZE&FINISH:{{upStatis.totalSize}}/{{upStatis.finishSize}}&nbsp;&nbsp;
                    COST:{{upStatis.costTime}}&nbsp;&nbsp;SPEED:{{upStatis.uploadSpeed}}</td></tr>
                <tr v-if="upStatis.onUpload"><td colspan="2"><Progress :percent="upStatis.progressVal"></Progress></td></tr>

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

    const cid = 'repository';

    export default {
        name: 'JRepository',
        data () {
            return {
                resList:[],
                dicts:{},

                queryParams:{},
                totalNum:0,
                pageSize:100,
                curPage:1,

                errMsg:'',

                upStatis:{
                    finishSize:'',
                    costTime:'',
                    uploadSpeed:'',
                    progressVal:0,
                    onUpload:false,
                    totalLen:0,
                    blockNum:0,
                    dv:null,
                    curBlock:0,
                },

                isLogin:false,
                actInfo:null,

                drawer: {
                    drawerStatus:false,
                    drawerBtnStyle:{right:'0px',zindex:1005},
                },

                query: {
                    drawerStatus:false,
                    drawerBtnStyle:{left:'0px',zindex:1000},
                },

                status: window.jm.mng.RES_STATUS,

                res0 : this.resetRes(),
                model:0,
            }
        },

        methods: {

            parseRemoteClass(res) {
                if(res.fromMavenCenter) {
                    return;
                }
                let self = this;
                self.errMsg = '';
                window.jm.mng.repository.parseRemoteClass(res.id).then((resp)=>{
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
                window.jm.mng.repository.dependencyList(res.id).then((resp)=>{
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
                window.jm.mng.repository.waitingResList(res.id).then((resp)=>{
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
                let cid = this.owner = this.actInfo ? this.actInfo.id:'-3';
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
                    }else {
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
                this.owner = r.clientId;
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
                this.owner = r.clientId;
            },

            getQueryConditions() {
                let ps = {};
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
                window.jm.mng.repository.addResourceData('test01',[0,1,2],0,3).then((resp)=>{
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

            uploadData(data,blockNum,cb) {
                let self = this;
                window.jm.mng.repository.addResourceData(this.res0.id,data,blockNum)
                    .then((resp) =>{
                        if(resp.code==0) {
                            cb(true);
                        } else {
                            self.errMsg = resp.msg;
                            cb(false);
                        }
                    })
                    .catch((err) =>{
                        if(cb) {
                            cb(false);
                        }
                        self.errMsg ='Upload data error: ' + name + err;
                    });
            },

            getSizeVal(size) {
                let v = '';
                if(size < 1024) {
                    v = size + 'B';
                }else if(size < 1024*1024 ) {
                    v = this.toFixed(size/1024,2) + 'KB';
                }else if(size < 1024*1024*1024 ) {
                    v = this.toFixed(size/(1024*1024),2) + 'MB';
                }else {
                    v = this.toFixed(size/(1024*1024*1024),2) + 'GB';
                }
                return v;
            },

            getFinishSize(blockSize,curBlock) {
                let s = blockSize * (curBlock+1);
                return this.getSizeVal(s);
            },

            getProgressVal(blockSize,curBlock,totalSize) {
                let s = blockSize * (curBlock+1);
                return this.toFixed((s/totalSize)*100,0);
            },

            toFixed(val, num) {
                if(val) {
                    return parseFloat(val).toFixed(num);
                }
            },

            getCostTime(startTime) {
                let v = '';
                let c = new Date().getTime() - startTime;
                if(c < 1000) {
                    v = c + 'MS';
                }else if(c < 1000 * 60) {
                    v = this.toFixed(c/1000,2) + 'S';
                }else if(c < 1000 * 60 * 60) {
                    c = c / 1000;
                    v = this.toFixed(c/60,2)+'M '+(c%60)+'S'
                }else {
                    c = c / 1000;
                    let h = c/(60*60);

                    c = c % (60*60);
                    let m = c/60;

                    let s = c %(60);

                    v = this.toFixed(h,0)+'H '+this.toFixed(m,0)+'M '+this.toFixed(s,0)+'S'
                }
                return v;
            },

            getSpeedVal(blockSize,curBlock,startTime) {
                let s = blockSize * (curBlock+1);
                let c = (new Date().getTime() - startTime)/1000;

                if(c <= 0) {
                    return '*';
                }else {
                    let sp = s/c;
                    return this.getSizeVal(sp)+'/M';
                }
            },

            doUpdate(){
                this.errMsg = "";
                let self = this;

                if(!this.res0.fileChange) {
                    window.jm.mng.repository.updateResource(this.res0,false)
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
                    self.getFileContent().then((buf) => {
                        self.res0.size =  buf.byteLength;
                        window.jm.mng.repository.updateResource(self.res0,true).then((resp)=>{
                            if(resp.code != 0) {
                                //self.$Message.error(resp.msg);
                                self.errMsg = resp.msg;
                                return;
                            }
                            //resp.data blockSize
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
                }

            },

            resetUploadStatis() {
                this.upStatis = {
                        finishSize:'',
                        costTime:'',
                        uploadSpeed:'',
                        progressVal:0,

                        onUpload:false,
                        totalLen:0,
                        blockNum:0,
                        dv:null,
                        curBlock:0,
                        startTime:0,
                },
                this.refresh();
            },

            initProgressData(buf) {
                let self = this;
                let totalLen = buf.byteLength;

                self.upStatis.totalLen = totalLen;
                self.upStatis.totalSize =  self.getSizeVal(totalLen);
                self.upStatis.onUpload = true;
                self.upStatis.blockNum = parseInt(totalLen/self.res0.blockSize);
                self.upStatis.dv = new DataView(buf,0,totalLen);
                self.upStatis.curBlock = 0;
                self.upStatis.startTime = new Date().getTime();
            },

            uploadCurBlock() {
                let self = this;
                self.upStatis.finishSize =  self.getFinishSize(self.res0.blockSize,self.upStatis.curBlock);
                self.upStatis.costTime = self.getCostTime(self.upStatis.startTime);
                self.upStatis.progressVal = parseInt(self.getProgressVal(self.res0.blockSize,self.upStatis.curBlock,self.upStatis.totalLen));
                self.upStatis.uploadSpeed = self.getSpeedVal(self.res0.blockSize,self.upStatis.curBlock,self.upStatis.startTime);

                if(self.upStatis.curBlock < self.upStatis.blockNum) {
                    let bl = [];
                    for(let j = 0; j < self.res0.blockSize; j++) {
                        bl.push(self.upStatis.dv.getUint8(self.res0.blockSize*self.upStatis.curBlock+j));
                    }
                    self.uploadData(bl,self.upStatis.curBlock,(success)=>{
                        if(success) {
                            self.upStatis.curBlock += 1;
                            self.uploadCurBlock();
                        } else {
                            self.errMsg = "Fail upload: " + self.res0.name;
                            self.resetUploadStatis();
                        }
                    });

                }else if(self.upStatis.curBlock == self.upStatis.blockNum) {
                    //最后一块
                    let lastBlockSize = self.upStatis.totalLen % self.res0.blockSize;
                    if( lastBlockSize > 0) {
                        let bl = [];
                        for (let j = 0; j < lastBlockSize; j++) {
                            bl.push(self.upStatis.dv.getUint8(self.upStatis.blockNum * self.res0.blockSize + j));
                        }
                        self.uploadData(bl,self.upStatis.curBlock,function(suc){
                            if(suc) {
                                self.closeDrawer();
                                self.$Message.success("Success upload "+self.res0.name);
                            } else {
                                //self.$Message.error("Fail upload "+self.res0.name);
                                self.errMsg = "Fail upload "+self.res0.name;
                            }
                        });
                    }
                    self.resetUploadStatis();
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
                    this.doUpdate();
                    return;
                }

                delete this.res0.fileChange;

                this.getFileContent().then((buf) => {
                    self.res0.size =  buf.byteLength;
                    window.jm.mng.repository.addResource(self.res0).then((resp)=>{
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
            },

            getFileContent(){
                let self = this;
                return new Promise(function(reso,reje){
                    let file = self.$refs.resFile.files[0];
                    if(file){
                        let reader = new FileReader();
                        reader.readAsArrayBuffer(file);

                        reader.onabort	=()=> {};

                        //当读取操作发生错误时调用
                        reader.onerror	= ()=> {
                            reje('read file error');
                        };

                        //当读取操作成功完成时调用
                        reader.onload =	 () => {
                            reso(reader.result);
                        };

                        //当读取操作完成时调用,不管是成功还是失败
                        reader.onloadend =	()=> {

                        };

                        //当读取操作将要开始之前调用
                        reader.onloadstart	= ()=> {

                        };

                        //在读取数据过程中周期性调用
                        reader.onprogress	= ()=> {

                        };
                    }
                })
            },

            deleteRes(res){
                let self = this;
                window.jm.mng.repository.deleteResource(res.id).then((rst)=>{
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
                this.isLogin = window.jm.rpc.isLogin();
                this.actInfo = window.jm.rpc.actInfo;

                if(!this.isLogin) {
                    this.resList = [];
                    return;
                }

                let qry = this.getQueryConditions();
                window.jm.mng.repository.getResourceList(qry,this.pageSize,this.curPage)
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
                this.actInfo = window.jm.rpc.actInfo;
                self.errMsg = "";
                if(this.actInfo && this.actInfo.isAdmin) {
                    let self = this;
                    window.jm.mng.repository.queryDict().then((resp)=>{
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
                window.jm.rpc.callRpcWithParams(window.jm.mng.repository.sn, window.jm.mng.repository.ns,
                    window.jm.mng.repository.v, 'clearInvalidResourceFile', [])
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
                window.jm.rpc.callRpcWithParams(window.jm.mng.repository.sn, window.jm.mng.repository.ns,
                    window.jm.mng.repository.v, 'clearInvalidDbFile', [])
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
            this.isLogin = window.jm.rpc.isLogin();
            this.actInfo = window.jm.rpc.actInfo;

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

            window.jm.rpc.addActListener(cid,function(type,ai){
                self.refresh();
                if(type == window.jm.rpc.Constants.LOGOUT) {
                    if(menus.length == 4){
                        menus.splice(2,1);
                        menus.splice(2,1);
                        window.jm.vue.$emit("menuChange", {"editorId":cid, "menus":menus});
                    }
                }else if(type == window.jm.rpc.Constants.LOGIN) {
                    if(ai.isAdmin){
                        menus.push(clearFileMenu);
                        menus.push(clearDbFileMenu);
                        window.jm.vue.$emit("menuChange", {"editorId":cid, "menus":menus});
                    }
                }
            });

            window.jm.vue.$emit("editorOpen", {"editorId":cid, "menus":menus});

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            this.getDicts();

            window.jm.vue.$on('editorClosed',ec);

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