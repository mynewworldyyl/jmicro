<template>
  <div class="JServiceItem">
      <p stype="word-break: break-all;padding: 0px 10px;font-size: medium;">{{node.id}}</p>

      <Card class="ItemCard">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Service Coordinate
          </p>
          <div>
              <label for="serviceName">Service Name</label>
              <Input id="serviceName" :value="node.val.key.serviceName" placeholder="Name" disabled/>
              <label for="namespace">Namespace</label>
              <Input id="namespace" :value="node.val.key.namespace" placeholder="Namespace" disabled/>
              <label for="version">Version</label>
              <Input id="version" :value="node.val.key.version" placeholder="Version" disabled/>
              <label for="instanceName">Instance Name</label>
              <Input id="instanceName" :value="node.val.key.instanceName" placeholder="Instance Name" disabled/>
              <label for="host">Host</label>
              <Input id="host" :value="node.val.key.host" placeholder="Host" disabled/>
              <label for="port">Socket Port</label>
              <Input id="port" :value="node.val.key.port" placeholder="Port" disabled/>
              <label for="code">Code</label>
              <Input id="code" :value="node.val.code" placeholder="" disabled/>
          </div>
      </Card>

      <Card  class="ItemCard">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Server
          </p>
          <a href="#" slot="extra"  @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>
              <label for="impl">Implement class</label>
              <Input id="impl" :value="node.val.impl" disabled/>

              <label for="handler">Handler</label>
              <Input id="handler" :value="node.val.handler" disabled/>

              <label for="servers">Servers</label>
              <div id="servers">
                  <p v-for="(s,index) in node.val.servers" :key="s.host+index">
                      {{s.protocol}}://{{s.host}}:{{s.port}}
                  </p>
              </div>

              <label for="degrade">Degrade Status</label>
              <Input id="degrade" v-model="node.val.degrade" placeholder="Degrade"/>

              <label for="maxSpeed">Max Speed</label>
              <Input id="maxSpeed" v-model="node.val.maxSpeed" placeholder="Max Speed"/>

              <label for="avgResponseTime">Service Average Response Time</label>
              <Input id="avgResponseTime"  v-model="node.val.avgResponseTime" placeholder="Average Response Time"/>

          </div>
      </Card>

      <Card  class="ItemCard">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Monitor And Debug
          </p>
          <a href="#" slot="extra"  @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>
              <Label for="monitorEnable">Monitor Enable</Label>
              <Select id="monitorEnable" v-model="node.val.monitorEnable">
                  <Option v-for="v in [-1,0,1]" :value="v" :key="'srvMonitorEnable'+v">{{v}}</Option>
              </Select>

              <Label for="debugMode">Debug Enable</Label>
              <Select id="debugMode" v-model="node.val.debugMode">
                  <Option v-for="v in [-1,0,1]" :value="v" :key="'srvDebugMode'+v">{{v}}</Option>
              </Select>

              <Label for="logLevel">Log Level</Label>
              <Select id="logLevel" v-model="node.val.logLevel">
                  <Option  v-for="(v,index) in ['Trance','Debug','Info','Warn','Error','Final']"
                           :value="index+1" :key="'srvlogLevel'+index">{{v}}
                  </Option>
              </Select>
          </div>
      </Card>

      <Card  class="ItemCard">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Timeout
          </p>
          <a href="#" slot="extra"  @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>
              <label for="timeout">Timeout</label>
              <Input id="timeout" v-model="node.val.timeout" placeholder="Timeout"/>

              <label for="retryCnt">Retry Count</label>
              <Input id="retryCnt" v-model="node.val.retryCnt" placeholder="Retry Count"/>

              <label for="retryInterval">Retry Interval</label>
              <Input id="retryInterval" v-model="node.val.retryInterval" placeholder="retryInterval"/>
          </div>
      </Card>

      <Card  class="ItemCard">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Statis Timer
          </p>
          <a href="#" slot="extra" @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>

              <Label for="baseTimeUnit">Time Unit</Label>
              <Select id="baseTimeUnit" v-model="node.val.baseTimeUnit">
                  <Option value="D">天</Option>
                  <Option value="H">时</Option>
                  <Option value="M">分</Option>
                  <Option value="S">秒</Option>
                  <Option value="MS">毫秒</Option>
                  <Option value="MC">微秒</Option>
                  <Option value="N">纳秒</Option>
              </Select>

              <label for="timeWindow">Time Window</label>
              <Input id="timeWindow" v-model="node.val.timeWindow" placeholder="Time"/>

              <label for="slotSize">Slot Size</label>
              <Input id="slotSize" v-model="node.val.slotSize" placeholder="slot Size"/>

              <label for="checkInterval">Check Interval</label>
              <Input id="checkInterval" v-model="node.val.checkInterval" placeholder="checkInterval"/>

          </div>
      </Card>


  </div>
</template>

<script>

import TreeNode from  "./JServiceList.vue"
import srv from "@/rpcservice/srv"

export default {
    name: 'JServiceItem',

    components: {
    },

    props: {
        item: TreeNode
    },

    methods: {
       save() {
            let self = this;
            let ms = this.node.val.methods;
           srv.updateItem(this.node.val)
               .then((rst)=>{
                   this.node.val.methods = ms;
                    if(rst) {
                        self.$Message.success("Save successfully");
                    }else {
                        self.$Message.fail("Save fail");
                    }
               })
               .catch(()=>{
                   this.node.val.methods = ms;
                   self.$Message.fail("Save fail");
               });
       }
    },

    data() {
        return {
            node:this.item,
        }
    }
}
</script>

<style scoped>
  .JServiceItem{
      height:auto;
      position: relative;
  }

  .JServiceItem > p{
      word-break: break-all;
      padding: 0px 10px;
      font-size: medium;
  }

  .ItemCard {
      width:350px;
      display:inline-block
  }

</style>
