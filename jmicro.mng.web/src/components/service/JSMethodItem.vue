<template>
  <div>

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Method Key
          </p>
          <div>
              <label for="method">Method Name</label>
              <Input id="method" :value="node.val.key.method" disabled/>

              <label for="paramStr">Method Parameters</label>
              <Input id="paramStr" :value="node.val.key.paramsStr" disabled/>

              <label for="topic">Topic</label>
              <Input id="topic" v-model="node.val.topic" disabled/>

              <Checkbox id="asyncable" v-model="node.val.asyncable" disabled>Asyncable</Checkbox>
              <Checkbox v-model="node.val.needResponse">Need Response</Checkbox>
          </div>
      </Card>

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Base Parameters
          </p>
          <a href="#" slot="extra"  @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>
              <label for="degrade">Degrade Status</label>
              <Input id="degrade" v-model="node.val.degrade" placeholder="Degrade"/>

              <label for="maxFailBeforeDegrade">Max Fail Before Degrade</label>
              <Input id="maxFailBeforeDegrade" v-model="node.val.maxFailBeforeDegrade"/>

              <label for="maxSpeed">Max Speed</label>
              <Input id="maxSpeed" v-model="node.val.maxSpeed" placeholder="Max Speed"/>

              <label for="avgResponseTime">Service Average Response Time</label>
              <Input id="avgResponseTime"  v-model="node.val.avgResponseTime" placeholder="Average Response Time"/>

          </div>
      </Card>

      <Card style="width:350px;float:left">
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
                  <Option v-for="v in [-1,0,1]" :value="v" :key="'monitorEnable'+v">{{v}}</Option>
              </Select>

              <Label for="debugMode">Debug Enable</Label>
              <Select id="debugMode" v-model="node.val.debugMode">
                  <Option v-for="v in [-1,0,1]" :value="v" :key="'debugMode'+v">{{v}}</Option>
              </Select>

              <Label for="logLevel">Log Level</Label>
              <Select id="logLevel" v-model="node.val.logLevel">
                  <Option  v-for="(v,index) in ['Trance','Debug','Info','Warn','Error','Final']" :key="'logLevel'+index"
                           :value="index+1">{{v}}
                  </Option>
              </Select>

              <Checkbox v-model="node.val.dumpDownStream">Dump Down Stream Data</Checkbox>
              <Checkbox v-model="node.val.dumpUpStream">Dump Up Stream Data</Checkbox>

          </div>
      </Card>

      <Card style="width:350px;float:left;">
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
              <br/>


          </div>
      </Card>

      <Card style="width:350px;float:left;clear: left">
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

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Break Rule
          </p>
          <a href="#" slot="extra" @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>

              <Checkbox v-model="node.val.breakingRule.enable">Enable</Checkbox>
              <br/>

              <label for="breakTimeInterval">Break Time Interval</label>
              <Input id="breakTimeInterval" v-model="node.val.breakingRule.breakTimeInterval"
                     placeholder=""/>

              <label for="breakPercent">Break Percent</label>
              <Input id="breakPercent" v-model="node.val.breakingRule.percent" placeholder=""/>

              <label for="bcheckInterval">Break Check Interval</label>
              <Input id="bcheckInterval" v-model="node.val.breakingRule.checkInterval" placeholder=""/>

              <label for="failResponse">Fail Response</label>
              <Input id="failResponse" v-model="node.val.failResponse"/>

          </div>
      </Card>

      <Card style="width:350px;float:left;clear: left">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Testing
          </p>
          <a href="#" slot="extra" @click="save()">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <div>

              <label for="testingArgs">Testing Args</label>
              <Input id="testingArgs" v-model="node.val.testingArgs"/>

          </div>
      </Card>

  </div>
</template>

<script>

    import TreeNode from  "./JServiceList.vue"

export default {
    name: 'JMethodItem',
    components: {
    },
    props: {
        meth : TreeNode
    },

    methods: {
        save() {
            let self = this;
            window.jm.mng.srv.updateMethod(this.node.val)
                .then((rst)=>{
                    if(rst) {
                        self.$Message.success("Save successfully");
                    }else {
                        self.$Message.fail("Save fail");
                    }
                })
                .catch(()=>{
                    self.$Message.fail("Save fail");
                });
        }
    },

    data(){
        return {
            node : this.meth,
        }
    }
}
</script>

<style scoped>
  .JContent{
      height:auto;
      position: relative;
  }

</style>
