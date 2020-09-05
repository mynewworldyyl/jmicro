<template>
  <div>
      <p stype="word-break: break-all;padding: 0px 10px;font-size: medium;">{{node.id}}</p>
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
              <Input id="topic"  class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}"
                     v-model="node.val.topic" type="textarea" disabled/>
              <!--<textarea id="topic" v-model="node.val.topic" disabled/>-->
          </div>
      </Card>

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Base Parameters
          </p>
          <a  href="javascript:void(0);" slot="extra"  @click="save()">
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
              <Checkbox id="asyncable" v-model="node.val.asyncable" disabled>Asyncable</Checkbox>
              <Checkbox v-model="node.val.needResponse">Need Response</Checkbox>
          </div>
      </Card>

      <Card style="width:350px;float:left">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Monitor And Debug
          </p>
          <a v-if="isLogin"  href="javascript:void(0);" slot="extra"  @click="save()">
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

      <Card style="width:350px; float:left;clear: left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Timeout
          </p>
          <a v-if="isLogin" href="javascript:void(0);" slot="extra"  @click="save()">
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

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Statis Timer
          </p>
          <a v-if="isLogin"  href="javascript:void(0);" slot="extra" @click="save()">
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

              <label for="slotSize">Slot Interval</label>
              <Input id="slotSize" v-model="node.val.slotIndterval" placeholder="slot Size"/>

              <label for="checkInterval">Check Interval</label>
              <Input id="checkInterval" v-model="node.val.checkInterval" placeholder="checkInterval"/>

          </div>
      </Card>

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Break Rule
          </p>
          <a v-if="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
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

      <Card style="width:350px;float:left;">
          <p slot="title">
              <Icon type="ios-film-outline"></Icon>
              Testing
          </p>
          <a v-if="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
              <Icon type="ios-loop-strong"></Icon>
              Save
          </a>
          <a slot="extra" @click="doTesting(node.val)" href="javascript:void(0);">
              <Icon type="ios-loop-strong"></Icon>
              {{timerId == -1? 'Start':'Stop'}}
          </a>
          <a slot="extra" @click="testingResult=''" href="javascript:void(0);">
              <Icon type="ios-loop-strong"></Icon>
              Clear
          </a>
          <div>

              <label for="testingArgs">Testing Args</label>
              <!-- <Input id="testingArgs" v-model="node.val.testingArgs"/> -->
              <Input id="testingArgs"  class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}"
                      type="textarea" v-model="node.val.testingArgs"/>

              <label for="testingResult">Testing Result</label>
              <Input id="testingResult"  class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}"
                     type="textarea" v-model="testingResult"/>

              <label for="InvokeNum">Testing Num</label>
              <Input id="InvokeNum" v-model="invokeNum"/>

              <label for="InvokeInterval">Interval with Milliseconds</label>
              <Input id="InvokeInterval"  v-model="invokeInterval"/>

          </div>
      </Card>

  </div>
</template>

<script>

    import TreeNode from  "./JServiceList.vue"

    const cid = 'JMethodItem';
export default {
    name: cid,
    components: {
    },
    props: {
        meth : TreeNode
    },

    mounted() {
        let self = this;
        window.jm.rpc.addActListener(cid,()=>{
            self.isLogin = window.jm.rpc.isLogin();
        });

        let ec = function() {
            window.jm.rpc.removeActListener(cid);
            window.jm.vue.$off('editorClosed',ec);
        }

        window.jm.vue.$on('editorClosed',ec);

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
        },

        doTesting() {
            if(this.timerId != -1) {
                clearInterval(this.timerId);
                this.timerId = -1;
            }else {
                let method = this.node.val;
                let self = this;
                let args = JSON.parse(method.testingArgs);

                if(self.invokeNum <= 0) {
                    self.$Message.fail("Invalid invoke Num:" + self.invokeNum);
                }else if(self.invokeNum == 1) {
                    self.callRpc(method,args);
                } else {
                    let i = 0;
                    self.timerId = setInterval(()=>{
                        if(i < self.invokeNum) {
                            self.callRpc(method,args);
                            i++;
                        }else {
                            clearInterval(self.timerId);
                            self.timerId = -1;
                        }
                    },self.invokeInterval);
                }
            }
            return false;
        },

        callRpc(method,args) {
            let self = this;
            window.jm.mng.callRpcWithParams(method.key.usk.serviceName, method.key.usk.namespace,
                method.key.usk.version, method.key.method, args)
                .then(rst=>{
                    //rst = window.jm.utils.parseJson(rst);
                    if(self.testingResult) {
                        self.testingResult  = self.testingResult + '\n' + rst;
                    } else {
                        self.testingResult  = rst;
                    }
                }).catch(err=>{
                    self.testingResult  = err;
            });
        }
    },

    data(){
        return {
            node : this.meth,
            isLogin : false,
            testingResult:'',
            invokeNum:1,
            invokeInterval:5000,
            timerId:-1,
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
