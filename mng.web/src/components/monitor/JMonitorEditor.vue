<template>
  <div class="JMonitorEditor">
    <p stype="word-break: break-all;padding: 0px 10px;font-size: medium;">{{group.id}}</p>
    <div v-for="(s,idx ) in cache.serverList" :key="s.srvKey+idx">
        <p class="header">
               <span v-if="idx!=0">
                  <a @click="btnClick(s)">{{ s.status ?  'Stop' : 'Start' }}</a>
              </span>
            <span v-if="idx != 0 && s.info">{{s.info.instanceName}}</span>
            <br/>
            <span>{{s.srvKey}}</span>
        </p>
        <div class="content">
            <!-- <div v-if="s.data">
                 subsriberSize: <span class="valueCls">{{s.data.subsriberSize}}</span>
                 sendCacheSize: <span class="valueCls">{{s.data.sendCacheSize}}</span>
             </div>-->
            <table >
                <thead>
                <tr v-if="s.info">
                    <td></td>
                    <td style="padding:0px 5px;" v-for="(t,idx) in s.info.types" :key="'type_'+idx">
                        {{t}} : {{ s.info.typeLabels[idx] }}</td>
                </tr>
                </thead>
                <tbody>
                <tr v-if="s.data">
                    <td>QPS</td> <td v-for="(q,idx) in s.data.qps" :key="'qps_'+idx">{{q.toFixed(2)}}</td>
                </tr>
                <tr v-if="s.data">
                    <td>CUR</td> <td v-for="(q,idx) in s.data.cur" :key="'cur_'+idx">{{q}}</td>
                </tr>
                <tr v-if="s.data">
                    <td>TOTAL</td> <td v-for="(q,idx) in s.data.total" :key="'total_'+idx">{{q}}</td>
                </tr>
                </tbody>
            </table>
        </div>
        <br/>
    </div>

</div>
</template>

<script>

    import TreeNode from '../common/JTreeNode.js'

    import config from "@/rpc/config"
    import i18n from "@/rpc/i18n"
    import monitor from "@/rpcservice/monitor"


export default {

    name: 'JMonitorEditor',

    components: {

    },

    props: {
        title: String,
        group: {type: TreeNode,required:true},
   },

    mounted() {
        let self = this;
        self.cache.serverList = [];


        if(this.group && this.group.val.length > 0) {
            self.cache.serverList.push({ info:this.group.val[0], status:false, srvKey:'Overview', data:null });
            for(let i = 0; i < this.group.val.length; i++) {
                self.cache.serverList.push({ info:this.group.val[i], status:false, srvKey:this.group.val[i].srvKey,
                    data:null });
            }
        }
    },


    data() {

        let dataKey = "JMonitor_cacheData_" + this.group.id ;

        let cacheData = config.cache[dataKey];
        if(!cacheData) {
            cacheData = config.cache[dataKey] = {
                serverList:[],
                monitorServers:[],
                timerId:-1,
                rmSrvKeys:[],
                addSrvKeys:[],
            }
        }

        return {
            cache : cacheData,
        };

    },

    methods:{

        i18nVal(t) {
            return i18n.get(t);
        },

        btnClick(s) {
            if(s.status) {
                this.stopServer(s);
                s.status = false;
            } else {
                this.startServer(s);
                s.status = true;
            }
        },

        startServer(s) {
            let self = this;
            if(self.cache.timerId == -1) {
                self.cache.monitorServers.push(s.srvKey);
            } else {
                self.cache.addSrvKeys.push(s.srvKey);
            }

            if(self.cache.timerId == -1) {
                self.cache.timerId =  setInterval(()=>{
                    if(self.cache.monitorServers.length == 0) {
                        clearInterval(self.cache.timerId);
                        self.cache.timerId = -1;
                        return;
                    }
                    monitor.status(self.cache.monitorServers)
                        .then((status)=>{
                            self.cache.serverList[0].data = status[0];
                            for(let i = 0; i < self.cache.monitorServers.length; i++) {
                                let ser = null;
                                for(let j = 1; j < self.cache.serverList.length; j++) {
                                    if(self.cache.serverList[j].srvKey == self.cache.monitorServers[i]) {
                                        ser = self.cache.serverList[j];
                                        break;
                                    }
                                }

                                if( ser ) {
                                    ser.data = status[i+1];
                                }
                            }

                            if(self.cache.rmSrvKeys.length > 0) {
                                self.rmKey();
                            }
                            if(self.cache.addSrvKeys.length > 0) {
                                self.addKey();
                            }
                        }).catch(err=>{
                        console.log(err);
                    });
                },1000);
            }
        },

        addKey() {
            for(let j = 0; j < this.cache.addSrvKeys.length; j++) {
                let srvKey = this.cache.addSrvKeys[j];
                let find = false;
                for(let i = 0; i < this.cache.monitorServers.length; i++) {
                    if(srvKey == this.cache.monitorServers[i]) {
                        find = true;
                        break;
                    }
                }
                this.cache.addSrvKeys = [];
                if(!find) {
                    this.cache.monitorServers.push(srvKey);
                }
            }
        },

        rmKey() {
            for(let j = 0; j < this.cache.rmSrvKeys.length; j++ ) {
                let idx = -1;
                let srvKey = this.cache.rmSrvKeys[j];
                for(let i = 0; i < this.cache.monitorServers.length; i++) {
                    if(srvKey == this.cache.monitorServers[i]) {
                        idx = i;
                        break;
                    }
                }
                if(idx >= 0) {
                    this.cache.monitorServers.splice(idx,1);
                }

                monitor.enable(srvKey,false)
                    .then(()=>{

                    }).catch(err=>{
                    console.log(err);
                    console.log(srvKey);
                });

            }

            this.cache.rmSrvKeys = [];
            if(this.cache.monitorServers.length == 0) {
                clearInterval(this.cache.timerId);
                this.cache.timerId = -1;
            }

        },

        stopServer(s) {
            if(this.cache.timerId != -1) {
                this.cache.rmSrvKeys.push(s.srvKey);
            }
        }

    }
}
</script>

<style scoped>
  .JMonitorEditor{
      height:auto;
      position: relative;
      margin:10px;
      margin-bottom:50px;
      overflow-x: auto;
  }

    .header{

    }

    .content{

    }

  .header span, .content span{
      display:inline-block;
      padding-right:10px;
  }

  .valueCls{
      font-weight: bold;
  }

  table
  {
      border-collapse: collapse;
      text-align: center;
  }
  table td, table th
  {
      border: 1px solid #cad9ea;
      color: #666;
      height: 30px;
  }
  table thead th
  {
      background-color: #CCE8EB;
      width: 100px;
  }
  table tr:nth-child(odd)
  {
      background: #fff;
  }
  table tr:nth-child(even)
  {
      background: #F5FAFA;
  }

</style>
