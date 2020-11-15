<template>
    <div class="JRepository">
       <!-- <a @click="addNode()">ADD</a>&nbsp;&nbsp;
        <a @click="testArrayBuffer()">TEST</a>&nbsp;&nbsp;
        <a @click="refresh()">REFRESH</a>-->
        <table class="configItemTalbe" width="99%">
            <thead><tr><td style="width:280px">NAME</td><td>SIZE</td><td>FINISH</td><td>OPERATION</td></tr></thead>
            <tr v-for="c in resList" :key="c.id">
                <td>{{c.name}}</td><td>{{c.size}}</td><td>{{c.finish}}</td>
                <td>
                    <a v-if="c.finishBlockNum != c.totalBlockNum && c.totalBlockNum > 0" @click="continueUpload(c)">CONTINUE</a>&nbsp;&nbsp;
                    <a @click="deleteRes(c)">DELETE</a>
                </td>
            </tr>
        </table>

        <Modal v-model="addResourceDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="onAddOk()">
            <table>
               <tr><td>NAME</td><td><input type="input" id="fileName" v-model="fileName"/></td></tr>
                <tr><td>VALUE</td><td><input type="file" id="nodeValue" ref="resFile"/></td></tr>
                <tr><td colspan="2" style="color:red">{{errMsg}}</td></tr>
                <tr v-if="onUpload"><td colspan="2">SIZE&FINISH:{{totalSize}}/{{finishSize}}&nbsp;&nbsp; COST:{{costTime}}&nbsp;&nbsp;SPEED:{{uploadSpeed}}</td></tr>
                <tr v-if="onUpload"><td colspan="2"><Progress :percent="progressVal"></Progress></td></tr>
            </table>
        </Modal>
    </div>
</template>

<script>

    const cid = 'repository';

    export default {
        name: 'JRepository',
        data () {
            return {
                resList:[],
                addResourceDialog:false,
                fileName:'',
                errMsg:'',

                totalSize:'',
                finishSize:'',
                costTime:'',
                uploadSpeed:'',
                progressVal:0,
                onUpload:false,
                isLogin:false,
            }
        },
        methods: {

            testArrayBuffer() {
                window.jm.mng.repository.addResourceData('test01',[0,1,2],0,3).then((resp)=>{
                    console.log(resp);
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            addNode(){
                this.addResourceDialog = true;
            },

            uploadData(name,data,blockNum,cb) {
                let self = this;
                window.jm.mng.repository.addResourceData(name,data,blockNum)
                    .then((resp) =>{
                        if(resp.code==0) {
                            cb(true);
                        }else {
                            self.$Message.error(resp.msg);
                            cb(false);
                        }
                    })
                    .catch((err) =>{
                        if(cb) {
                            cb(false);
                        }
                        throw 'Upload data error: ' + name + err;
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

            onAddOk(){
                let self = this;
                let startTime = new Date().getTime();

                this.getFileContent().then((buf) => {
                    let totalLen = buf.byteLength;
                    self.totalSize =  self.getSizeVal(totalLen);
                    self.onUpload = true;
                    let file = self.$refs.resFile.files[0];

                    if(!self.fileName || self.fileName == 0 || this.fileName == '') {
                        self.fileName = file.name;
                    }

                    window.jm.mng.repository.addResource(self.fileName, totalLen).then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        //resp.data blockSize
                        let blockSize = resp.data;
                        let blockNum = parseInt(totalLen/blockSize);
                        let dv = new DataView(buf,0,totalLen);
                        let curBlock = 0;

                        let ud = function(success){
                            if(success) {
                                self.finishSize =  self.getFinishSize(blockSize,curBlock);
                                self.costTime = self.getCostTime(startTime);
                                self.progressVal = parseInt(self.getProgressVal(blockSize,curBlock,totalLen));
                                self.uploadSpeed = self.getSpeedVal(blockSize,curBlock,startTime);

                                if(curBlock < blockNum) {
                                    let bl = [];
                                    for(let j = 0; j < blockSize; j++) {
                                        bl.push(dv.getUint8(blockSize*curBlock+j));
                                    }
                                    self.uploadData(self.fileName,bl,curBlock,ud);
                                    curBlock++;
                                }else if(curBlock == blockNum) {
                                    //最后一块
                                    let lastBlockSize = totalLen % blockSize;
                                    if( lastBlockSize > 0) {
                                        let bl = [];
                                        for (let j = 0; j < lastBlockSize; j++) {
                                            bl.push(dv.getUint8(blockNum * blockSize + j));
                                        }
                                        self.uploadData(self.fileName,bl,curBlock,function(suc){
                                            if(suc) {
                                                self.addResourceDialog = false;
                                                self.$Message.success("Success upload "+this.fileName);
                                            } else {
                                                self.$Message.success("Fail upload "+this.fileName);
                                            }
                                        });
                                    }

                                        self.totalSize = '',
                                        self.finishSize = '',
                                        self.costTime = '',
                                        self.uploadSpeed = '',
                                        self.progressVal = 0,
                                        self.onUpload = false;
                                        this.fileName = '';
                                        self.refresh();
                                }
                            }
                        }

                        ud(true);

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

                        reader.onabort	=()=> {

                        };

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

            continueUpload(res){
                console.log(res);
            },

            deleteRes(res){
                let self = this;
                window.jm.mng.repository.deleteResource(res.name).then((rst)=>{
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

            refresh(){
                let self = this;
                this.isLogin = window.jm.rpc.isLogin();
                if(!this.isLogin) {
                    this.resList = [];
                    return;
                }
                window.jm.mng.repository.getResourceList(false).then((resList)=>{
                    if(!resList || resList.length == 0 ) {
                        self.$Message.success("No data to show");
                        return;
                    }
                    this.resList = resList;
                }).catch((err)=>{
                    window.console.log(err);
                    self.$Message.error(err || "Service not found");
                });
            }
        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            window.jm.rpc.addActListener(cid,this.refresh);
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"addNode",label:"Add Node",icon:"ios-cog",call: ()=>{self.addNode(); }},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

            let ec = function() {
                window.jm.rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }

            window.jm.vue.$on('editorClosed',ec);

            this.refresh();
        },
    }
</script>

<style>
    .JRepository{
        height:auto;
    }

</style>