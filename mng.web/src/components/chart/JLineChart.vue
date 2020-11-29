<template>
  <div>
      <Card class="StatisCard">
          <!--<p slot="title">
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

          <div class="ChartContainer" :id="charContainerId">

          </div>

      </Card>


  </div>

</template>

<script>

    import * as echarts from 'echarts';
    //import TreeNode from "../service/JServiceList.vue"

    //let type2Labels = null;

export default {
    name: 'JLineChart',
    components:{

    },

    props:{
        charContainerId:{type:String, default:''},
        node:{type:Object, default:null},
        indexTypes:{type:String, required:true},
        titleName:{type:String,default:''},
        type:{type:String,default:'qps'},
    },

    beforeCreate() {

    },

    mounted() {
        let elt = document.getElementById(this.charContainerId);
        this.cache.chart = echarts.init(elt);
        this.cache.chart.setOption(this.option);

        let self = this;
        window.jm.mng.statis.getType2Labels((data,err)=>{
            if(!err) {
                self.type2Labels = data;
                self.allTypes = window.jm.mng.statis.getTypes();
                self.allLabels = window.jm.mng.statis.getLabels();

                self.indexArr = [];
                for(let i = 0; i < this.types.length; i++) {
                    let tc = this.types[i];
                    for(let j = 0; j < this.allTypes.length; j++) {
                        if(tc == this.allTypes[j]) {
                            self.indexArr.push(j);
                        }
                    }
                }
            }else {
                //self.$Message.error(err);
                console.log(err);
            }
        });
    },

    data () {

        let types = this.indexTypes.split(',');
        let ds = [];
        if(types.length > 0) {
            for(let i = 0; i < types.length; i++) {
                types[i] = parseInt(types[i]);
                ds.push([]);
            }
        }

        let dataKey = "JLinechart_"+this.charContainerId;

        let cacheData = window.jm.mng.cache[dataKey];
        if(!cacheData) {
            cacheData = window.jm.mng.cache[dataKey] = {
                chart:null,
                data: ds,
                now : new Date().getTime(),
                started:false,
            }
        }

        let legends = [];
        let series = [];
        if(types.length > 0) {
            for(let i = 0; i < types.length; i++) {
                let s = {
                    name: types[i],
                    type: 'line',
                    showSymbol: false,
                    hoverAnimation: false,
                    data: cacheData.data[i]
                };
                series.push(s);
                legends.push(types[i]+'');
            }
        }else {
            console.log("indexTypes attribute cannot be null");
        }

        return {
            btnTitle:'Start',
            type2Labels:null,
            cache : cacheData,
            types:types,
            series:series,
            allTypes: null,
            allLabels: null,
            indexArr:[],

            option: {
                title: {
                    //text: 'testing'
                },
                legend: {
                    data: legends
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '3%',
                    containLabel: true
                },
                /*toolbox: {
                    feature: {
                        saveAsImage: {}
                    }
                },*/
                tooltip: {
                    trigger: 'axis',
                    formatter: function (params) {
                        let ps = params[0];
                        let date = new Date(ps.value[0]);
                        return ps.seriesName + ":"+date.getHours() + '/' + (date.getMinutes()) + '/' + date.getSeconds() + ' : ' + ps.value[1];
                    },
                    axisPointer: {
                        animation: false
                    }
                },
                xAxis: {
                    type: 'time',
                    splitLine: {
                        show: false
                    }
                },
                yAxis: {
                    type: 'value',
                    boundaryGap: [0, '100%'],
                    splitLine: {
                        show: false
                    }
                },
                series: series
            }
        }
    },
    methods:{

        callback(psData) {
            //console.log(psData);
            let data = psData.data[this.type];
            this.cache.now  += 2000;

            if(!data) {
                console.log(this.type +' data is null');
                return;
            }

            for(let j = 0; j < this.indexArr.length; j++) {
                let arr = this.cache.data[j];
                let s = this.series[j];
                if(arr.length > 60) {
                    arr.shift();
                }
                let v = data[this.indexArr[j]];
                if(!v) {
                    v = 0;
                }
                arr.push([this.cache.now, v.toFixed(2)]);
                s.data = arr;
            }
            this.cache.chart.setOption({
                series: this.series
            });

        },

        menu() {
            //let self = this;
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
                window.jm.mng.statis.unsubscribeData(idKey,2,this.callback).then(rst =>{
                    self.$Message.success(rst);
                    self.cache.started = false;
                    self.btnTitle = 'Start';
                }).catch(err =>{
                    self.$Message.error(err);
                    self.cache.started = true;
                });
            } else {
                window.jm.mng.statis.subscribeData(idKey,2,this.callback)
                    .then(rst =>{
                        self.$Message.success(rst);
                        self.cache.started = true;
                        self.btnTitle = 'Stop';
                    }).catch(err =>{
                    self.$Message.error(err);
                    self.cache.started = false;
                });
            }
        }

    }

}
</script>

<style scoped>
   .ChartContainer {
       width:98%; height:300px;
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
