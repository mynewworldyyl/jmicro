<template>
  <div>
      <Card style="width:350px;float:left;">
         <!-- <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              {{titleName}}
              <Button @click="start()" :disabled="cache.started">start</Button>
              <Button @click="stop()" :disabled="!cache.started">stop</Button>
          </p>-->
          <div class="chartHeaderBar">
              <span class="titleBar">{{titleName}}</span>
              <span class="startBtn"><a @click="start()">{{btnTitle}}</a></span>
              <span class="menuBtn"><a @click="menu()">Menu</a></span>
          </div>

          <div class="ChartContainer" :id="charContainerId" >
              <div v-for="(val, idx) in cache.indexData" :key="idx" class="itemContainer">
                  <p>Value: <span>{{ val}}</span> </p>
                  <div>
                      <span>CODE:{{allTypes[idx]}}</span>
                      <span>{{allLabels[idx]}} </span>
                  </div>
              </div>
             <!-- <Table border :columns="indexColumn" :data="cache.indexData"  height="310" width="330"></Table>-->
              <!--<table>
                  <tr v-for="(type, idx) in cache.indexData.types" :key="type">
                      <td>{{type}}</td>  <td>{{cache.indexData.labels[idx]}}</td><td>{{cache.indexData.datas[idx]}}</td>
                  </tr>
              </table>-->
          </div>

      </Card>


  </div>

</template>

<script>

export default {
    name: 'JIndexPercent',
    components:{

    },

    props:{
        charContainerId:{type:String,default:''},
        titleName:{type:String,default:''},
        node:{type:Object,default:null},
        type:{type:String,default:'qps'},
    },

    mounted() {
        let self = this;
        window.jm.mng.statis.getType2Labels((data,err)=>{
            if(!err) {
                self.type2Labels = data;
                self.allTypes = window.jm.mng.statis.getTypes();
                self.allLabels = window.jm.mng.statis.getLabels();
            }else {
                //self.$Message.error(err);
                console.log(err);
            }
        });
    },

    data () {

        let dataKey = "JIndexPercent_" + this.charContainerId;
        let cache = window.jm.mng.cache[dataKey];
        if(!cache) {
            cache = window.jm.mng.cache[dataKey] = {
                started:false,
                indexData:[]
            }
        }
        return {
            btnTitle:'Start',
            cache:cache,
            type2Labels:null,
            allTypes: null,
            allLabels: null,
            indexArr:[],

            indexColumn:[
                {title: 'Code', key: 'code'},
                {title: 'Name', key: 'name'},
                {title: 'Value', key: 'value'}
            ]
        }
    },
    methods:{
        callback(psData) {
            if(psData.data) {
                this.cache.indexData = psData.data[this.type];

               /* let arr = [];
                for(let i = 0; i < psData.data.types.length; i++) {
                    let d = psData.data;
                    arr.push({name:d.labels[i],value:d.datas[i],code:d.types[i]});
                }
                this.cache.indexData = arr;*/
            }
        },

        /*rowClassName(colIndex) {
            console.log(colIndex);
            if(colIndex == 0) {
                return "typeClass";
            }else if(colIndex == 1) {
                return "nameClass";
            }else if(colIndex == 2) {
                return "valClass";
            }
        },*/

        menu() {

        },

        start() {
            //let mkey = 'org.jmicro.example.api.rpc.ISimpleRpc##simpleRpc##0.0.1##exampleProdiver0##192.168.56.1##63211##hello##Ljava/lang/String;';
            let self = this;

            let idKey = this.node.id;
            if(idKey.startWith('statis:')) {
                idKey = idKey.substring(7);
            }else if(idKey.startWith('service:')) {
                idKey = idKey.substring(8);
            }

            if(self.cache.started) {
                window.jm.mng.statis.unsubscribeData(idKey,2,this.callback
                ).then(rst =>{
                    self.$Message.success(rst);
                    self.cache.started = false;
                    self.btnTitle = 'Start';
                }).catch(err =>{
                    self.$Message.error(err);
                });
            }else {
                window.jm.mng.statis.subscribeData(idKey,2,this.callback)
                    .then(rst =>{
                        self.$Message.success(rst);
                        self.cache.started = true;
                        self.btnTitle = 'Stop';
                    }).catch(err =>{
                    self.$Message.error(err);
                });
            }

        }

    }

}
</script>

<style scoped>
   .ChartContainer {
       width:98%; height:500px;
       overflow-y: auto;
   }

   .ivu-card-body {
       padding:0px;
   }

   .itemContainer{
       border-bottom: 1px dotted lightgray;
   }

   .itemContainer span{
       display: inline-block;
       margin-right: 20px;
       font-size: 10px;
   }

   .itemContainer p span{
       font-weight: bold;
       font-size: 12px;
   }

   .chartHeaderBar {
       height: 25px;
       text-align: center;
       border-bottom: 1px solid lightgray;
       position: relative;
       height:25px;
   }

   .chartHeaderBar a {
       color:black;
   }

   .titleBar{
       font-weight: bold;
   }

   .menuBtn{
       display:inline-block;
       position:absolute;
       left:2px;
   }

   .startBtn{
       display:inline-block;
       position:absolute;
       right:2px;
   }
</style>
