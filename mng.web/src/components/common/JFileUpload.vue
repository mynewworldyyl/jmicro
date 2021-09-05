<template>
    <div class="JFileUpload">
        <!--<div class="btnBox">
            <Button @click="doUpdate()">确定</Button>
        </div>-->
        <div class="error">{{errMsg}}</div>
        <div class="fileBox">
            <Label  for="file">{{'File'|i18n}}</Label>
            <input  id ="file"  type="file" ref="fileUpload" @change="fileChange()"/><br/>

           <!-- <Label v-if="extParams"  for="extParams">{{extParams |i18n}}</Label>
            <Input  v-if="extParams"  id="extParams" v-model="res.extParams"/>-->

<!--            <input type="file" ref="fileUpload" @change="fileChange()"/>
            <input type="text" ref="fileUpload" @change="fileChange()"/>-->
        </div>
        <div class="statisBox" v-if="upStatis.onUpload">
            <div>SIZE&FINISH:{{upStatis.totalSize}}/{{upStatis.finishSize}}&nbsp;&nbsp;
                COST:{{upStatis.costTime}}&nbsp;&nbsp;SPEED:{{upStatis.uploadSpeed}}</div>
            <div><Progress :percent="upStatis.progressVal"></Progress></div>
        </div>

    </div>
</template>

<script>

    import rpc from "@/rpc/rpcbase"
    import JDataOutput from "@/rpc/dataoutput";
    import {Constants} from "@/rpc/message"

export default {
    name: 'JFileUpload',

    props:{
        //上传文件方法
        mcode:{
            default:'',
            type:String,
        },

        serviceName:{
            default:'',
            type:String,
        },

        namespace:{
            default:'',
            type:String,
        },

        version:{
            default:'',
            type:String,
        },

        method:{
            default:'',
            type:String,
        },

        fileType:{
            default:'*',
            type:String,
        },
    },

    components: {

    },

    data() {
        return {
            upStatis:{},
            fc:false,
            errMsg:'',

            res:{
                name:'',
                modifiedTime:0,
                resExtType:'',
            }
        };
    },

    updated() {

    },

    mounted(){

    },

    methods: {
        fileChange() {
            if(!this.$refs.fileUpload.files || this.$refs.fileUpload.files.length ==0) {
                return;
            }
            let file = this.$refs.fileUpload.files[0];
            this.res.name = file.name;
            this.res.modifiedTime = file.lastModified;
            this.res.fc = true;
            this.res.resExtType = "";
        },

        doUpdate(extParams){
            this.errMsg = "";
            let self = this;
            if(!this.res.fc) {
                return;
            }
            if(!this.res.name) {
                throw 'Invalid file name: ' + this.res.name;
            }
            this.res.name = this.res.name.trim()
            if(this.res.name.length == 0) {
                throw 'Invalid file name length ' + this.res.name;
            }

    /*        if(this.extNotNull && !this.res.extParams) {
                this.errMsg = this.extParams + '不能为空';
                return
            }*/

            delete this.res.fc;

            self.getFileContent().then((buf) => {
                self.res.size =  buf.byteLength;
                let jo = new JDataOutput(128);
                jo.writeInt(0)
                jo.writeUnsignedLong(self.res.size)
                jo.writeUtf8String(extParams)
                jo.writeUtf8String(this.res.name)

                self.callRemote(self.mcode,[jo.getBuf()])
                    .then((resp)=>{
                    if(resp.code != 0) {
                        //self.$Message.error(resp.msg);
                        self.errMsg = resp.msg;
                        return;
                    }
                    self.res = resp.data;
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
            }
        },

        initProgressData(buf) {
            let totalLen = buf.byteLength
            this.upStatis.totalLen = totalLen
            this.upStatis.totalSize =  this.getSizeVal(totalLen)
            this.upStatis.onUpload = true
            this.upStatis.blockNum = parseInt(totalLen/this.res.blockSize)
            //this.upStatis.dv = new DataView(buf,0,totalLen)
            this.upStatis.buf = buf
            this.upStatis.curBlock = 0
            this.upStatis.startTime = new Date().getTime()
        },

        uploadCurBlock() {
            let self = this;
            self.upStatis.finishSize =  self.getFinishSize(self.res.blockSize,self.upStatis.curBlock);
            self.upStatis.costTime = self.getCostTime(self.upStatis.startTime);
            self.upStatis.progressVal = parseInt(self.getProgressVal(self.res.blockSize,self.upStatis.curBlock,self.upStatis.totalLen));
            self.upStatis.uploadSpeed = self.getSpeedVal(self.res.blockSize,self.upStatis.curBlock,self.upStatis.startTime);

            let jo = new JDataOutput(self.res.blockSize + 8);
            jo.writeInt(self.res.id)//文件ID
            jo.writeInt(self.upStatis.curBlock)//块编号

            if( self.upStatis.curBlock < self.upStatis.blockNum ) {
                let stPos = self.res.blockSize * self.upStatis.curBlock
                jo.writeArrayBuffer(this.upStatis.buf.slice(stPos, stPos + self.res.blockSize))
                self.callRemote(self.mcode,[jo.getBuf()])
                    .then((resp)=>{
                        if(resp.code == 0) {
                            self.upStatis.curBlock += 1;
                            self.uploadCurBlock();
                        } else {
                            self.errMsg = "Fail upload: " + resp.msg;
                            self.resetUploadStatis();
                        }
                    });

            }else if(self.upStatis.curBlock == self.upStatis.blockNum) {
                //最后一块
                let lastBlockSize = self.upStatis.totalLen % self.res.blockSize;
                if( lastBlockSize > 0) {
                    let stPos = self.res.blockSize * self.upStatis.curBlock
                    jo.writeArrayBuffer(this.upStatis.buf.slice(stPos, self.upStatis.totalLen))
                    self.callRemote(self.mcode,[jo.getBuf()])
                        .then(resp=>{
                            if(resp.code == 0) {
                                self.$Message.success("Success upload "+self.res.name);
                                self.$emit('finish')
                            } else {
                                //self.$Message.error("Fail upload "+self.res.name);
                                self.errMsg = "Fail upload: "+resp.msg;
                            }
                        });
                }
                self.resetUploadStatis();
            }
        },

        getFileContent(){
            let self = this;
            return new Promise(function(reso,reje){
                let file = self.$refs.fileUpload.files[0];
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

        callRemote(mcode,args) {
            let req = rpc.cmreq(mcode,args)
            return rpc.callRpc(req,Constants.PROTOCOL_BIN,Constants.PROTOCOL_JSON)
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

    },
}

</script>

<style scoped>
    .JFileUpload{
        width:100%;
    }

    .fileBox{
        height:50px;
    }

    .statisBox {
        height:80px;
    }

</style>
