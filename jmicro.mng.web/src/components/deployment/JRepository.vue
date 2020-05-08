<template>
    <div class="JRepository">
        <a @click="addNode()">ADD</a>&nbsp;&nbsp;
        <a @click="testArrayBuffer()">TEST</a>&nbsp;&nbsp;
        <a @click="refresh()">REFRESH</a>
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>NAME</td><td>SIZE</td><td>FINISH</td><td>OPERATION</td></tr></thead>
            <tr v-for="c in resList" :key="c.id">
                <td>{{c.name}}</td><td>{{c.size}}</td><td>{{c.finish}}</td>
                <td>
                    <a v-if="!c.finish" @click="continueUpload(c)">CONTINUE</a>&nbsp;&nbsp;
                    <a @click="deleteRes(c)">DELETE</a>
                </td>
            </tr>
        </table>

        <Modal v-model="addResourceDialog" :loading="true" ref="addNodeDialog" width="360" @on-ok="onAddOk()">
            <table>
                <tr><td>NAME</td><td><input type="input" id="nodeName" v-model="name"/></td></tr>
                <tr><td>VALUE</td><td><input type="file" id="nodeValue" @change="fileSelect()" ref="resFile"/></td></tr>
                <tr><td colspan="2" style="color:red">{{errMsg}}</td></tr>
            </table>
        </Modal>
    </div>
</template>

<script>

    export default {
        name: 'JRepository',
        data () {
            return {
                resList:[],
                addResourceDialog:false,
                file:null,
                name:'',
                errMsg:'',
                fileContent:null,
            }
        },
        methods: {

            testArrayBuffer() {
                window.jm.mng.repository.addResourceData('test01',[0,1,2],0,3).then((rst)=>{
                    console.log(rst);
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            addNode(){
                this.addResourceDialog = true;

            },

            onAddOk(){

            },

            fileSelect(){
                let self = this;
               let file = this.$refs.resFile.files[0];
                if(file){
                    //读取本地文件，以gbk编码方式输出
                    let reader = new FileReader();
                    reader.readAsArrayBuffer(file);

                    reader.onabort	=()=> {

                    };

                    //当读取操作发生错误时调用
                    reader.onerror	= ()=> {

                    };

                    //当读取操作成功完成时调用
                    reader.onload =	 () => {
                        self.fileContent = this.result;
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
            },

            continueUpload(res){
                console.log(res);
            },

            deleteRes(res){
                let self = this;
                window.jm.mng.repository.deleteResource(res.name).then((rst)=>{
                    if(rst ) {
                        for(let i = 0; i < self.resList.length; i++) {
                            if(self.resList[i].name == res.name) {
                                self.resList.splice(i,1);
                                return;
                            }
                        }
                    }else {
                        self.$Message.fail("Fail to delete resource "+res.name);
                    }
                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            refresh(){
                window.jm.mng.repository.getResourceList().then((resList)=>{
                    if(!resList || resList.length == 0 ) {
                        return;
                    }
                    this.resList = resList;
                }).catch((err)=>{
                    window.console.log(err);
                });
            }
        },

        activated () {
            this.refresh();
        },
    }
</script>

<style>
    .JRepository{

    }
    .configItemTalbe {
        border-collapse: collapse;
        margin: 0 auto;
        text-align: left;
    }

    .configItemTalbe th {
        font-size: medium;
        font-family: "Microsoft Yahei", "微软雅黑", Tahoma, Arial, Helvetica, STHeiti;

    }



    .configItemTalbe td, table th {
        border: 1px solid #cad9ea;
        color: #666;
        height: 30px;
        max-width: 95px;
        max-height: 50px;
        overflow: hidden; /*超过区域就隐藏*/
        /*display: -webkit-box;*/ /*-webkit- 是浏览器前缀，兼容旧版浏览器的 即为display: box;*/
        -webkit-line-clamp: 2; /*限制在一个块元素显示的文本的行数*/
        -webkit-box-orient: vertical; /*box-orient 属性规定框的子元素应该被水平或垂直排列。horizontal：水平，vertical：垂直*/
        word-break: break-all; /*word-break 属性规定自动换行的处理方法 ，break-all：允许在单词内换行。*/
    }

    .configItemTalbe thead th {
        background-color: #CCE8EB;
        width: 100px;
    }

    .configItemTalbe tr:nth-child(odd) {
        background: #fff;
    }

    .configItemTalbe tr:nth-child(even) {
        background: #F5FAFA;
    }

</style>