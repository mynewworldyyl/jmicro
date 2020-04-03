<template>
  <div>
      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Testing
              <Button @click="start()" :disabled="cache.started">start</Button>
              <Button @click="stop()" :disabled="!cache.started">stop</Button>
          </p>

          <div class="ChartContainer" :id="charContainerId">

          </div>

      </Card>


  </div>

</template>

<script>

    import * as echarts from 'echarts';
    //import TreeNode from "../service/JServiceList.vue"

export default {
    name: 'JLineChart',
    components:{

    },

    props:{
        charContainerId:{type:String,default:''},
        node:{type:Object,default:null},
    },

    mounted() {
        let elt = document.getElementById(this.charContainerId);
        this.cache.chart = echarts.init(elt);
        this.cache.chart.setOption(this.option);
    },

    data () {

        let dataKey = "JLinechart_"+this.node.id;

        let cacheData = window.jm.mng.cache[dataKey];
        if(!cacheData) {
            cacheData = window.jm.mng.cache[dataKey] = {
                chart:null,
                data: [],
                now : new Date().getTime(),
                started:false,
            }
        }

        return {
            cache : cacheData,
            option: {
                title: {
                    text: 'testing'
                },
                tooltip: {
                    trigger: 'axis',
                    formatter: function (params) {
                        params = params[0];
                        var date = new Date(params.value[0]);
                        return date.getHours() + '/' + (date.getMinutes()) + '/' + date.getSeconds() + ' : ' + params.value[1];
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
                series: [{
                    name: 'simulator',
                    type: 'line',
                    showSymbol: false,
                    hoverAnimation: false,
                    data: cacheData.data
                }]
            }
        }
    },
    methods:{
        parseData(data) {
            this.cache.now  += 2000;
            //let label = [now.getHours(),now.getMinutes(),now.getSeconds()].join(':')
            return [this.cache.now, data[9].toFixed(2)];
        },

        callback(psData) {
            //console.log(psData);
            let d = this.parseData(psData.data);
            if(this.cache.data.length > 60) {
                this.cache.data.shift();
            }
            this.cache.data.push(d);
            this.cache.chart.setOption({
                series: [{
                    name: 'simulator',
                    data: this.cache.data
                }]
            });
        },

        stop() {
            let self = this;
            window.jm.mng.statis.unsubscribeData(this.node.id,2,this.callback
            ).then(rst =>{
                self.$Message.success(rst);
                self.cache.started = false;
            }).catch(err =>{
                self.$Message.error(err);
                self.cache.started = true;
            });
        },

        start() {
            //let mkey = 'org.jmicro.example.api.rpc.ISimpleRpc##simpleRpc##0.0.1##exampleProdiver0##192.168.56.1##63211##hello##Ljava/lang/String;';
            let self = this;
            window.jm.mng.statis.subscribeData(this.node.id,2,this.callback)
                .then(rst =>{
                self.$Message.success(rst);
                    self.cache.started = true;
            }).catch(err =>{
                self.$Message.error(err);
                self.cache.started = false;
            });

            /*setInterval(()=>{
         let v = Math.random()*100+1;
         self.data.push(self.parseData({9:v}));
         chart.setOption({
             series: [{
                 name: '模拟数据',
                 data: self.data
             }]
         });
     },2000)*/

        }

    }

}
</script>

<style scoped>
   .ChartContainer {
       width:98%; height:300px;
   }

</style>
