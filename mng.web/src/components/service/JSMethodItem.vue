<template>
  <div>
      <p stype="word-break: break-all;padding: 0px 0px;font-size: medium;">{{node.id}}</p>

      <div class="itemRow">
          <div class="ItemCard">
              <div class="ItemCardHeader"><h3 class="ItemCardTitle">Method Key</h3></div>
              <table class="SMethodTable">
                  <!--<thead><tr><td>{{"Name"|i18n}}</td><td>{{'Value'|i18n}}</td></tr></thead>-->
                  <!--<tr><td>Client ID</td> <td><Input :value="node.val.key.method" disabled/></td></tr>-->
                  <tr><td>Method Name</td> <td><Input :value="node.val.key.method" disabled/></td></tr>
                  <tr><td>Method Parameters</td> <td><Input :value="node.val.key.paramsStr" disabled/></td></tr>
                  <tr><td>Return Parameter</td> <td><Input id="paramStr" :value="node.val.key.paramsStr" disabled/></td></tr>
                  <tr><td>Method Code</td> <td> <Input id="snvHash" :value="node.val.key.snvHash" disabled/></td></tr>
                  <tr><td>Topic</td>
                      <td>
                      <Input class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}"
                           v-model="node.val.topic" type="textarea" disabled/>
                      </td>
                  </tr>
              </table>
          </div>

          <div  class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">Base Parameters</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                  </div>
              </div>
              <table class="SMethodTable">
                  <tr><td>Asyncable</td>
                      <td><Checkbox id="asyncable" v-model="node.val.asyncable" disabled></Checkbox></td>
                  </tr>
                  <tr><td>Need Response</td> <td><Checkbox v-model="node.val.needResponse"></Checkbox></td></tr>
                  <tr><td>Degrade Status</td>
                      <td><Input id="degrade" v-model="node.val.degrade" placeholder="Degrade"/></td></tr>
                  <tr><td>Max Fail Before Degrade</td>
                      <td><Input id="maxFailBeforeDegrade" v-model="node.val.maxFailBeforeDegrade"/></td></tr>
                  <tr><td>Max Speed</td>
                      <td><Input id="maxSpeed" v-model="node.val.maxSpeed" placeholder="Max Speed"/></td></tr>
                  <tr><td>{{"LimitSpeedType"|i18n}}</td>
                      <td><Select id="LimitSpeedType" v-model="node.val.limitType">
                          <Option value="1">{{"BySelf"|i18n}}</Option>
                          <Option value="2">{{"ByStatisServer"|i18n}}</Option>
                      </Select></td></tr>
                  <tr><td>Service Average Response Time</td>
                      <td><Input id="avgResponseTime"  v-model="node.val.avgResponseTime" placeholder="Average Response Time"/>
                      </td></tr>

              </table>
          </div>

          <div class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">Monitor And Debug</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                  </div>
              </div>

              <table class="SMethodTable">

                  <tr><td>Dump Down</td> <td><Checkbox v-model="node.val.dumpDownStream"></Checkbox></td></tr>
                  <tr><td>Dump Up</td> <td> <Checkbox v-model="node.val.dumpUpStream"></Checkbox></td></tr>
                  <tr><td>Monitor Enable</td>
                      <td><Select id="monitorEnable" v-model="node.val.monitorEnable">
                          <Option v-for="v in [-1,0,1]" :value="v" :key="'monitorEnable'+v">{{v}}</Option>
                      </Select></td>
                  </tr>
                  <tr><td>Debug Enable</td>
                      <td>  <Select id="debugMode" v-model="node.val.debugMode">
                          <Option v-for="v in [-1,0,1]" :value="v" :key="'debugMode'+v">{{v}}</Option>
                      </Select></td></tr>

                  <tr><td>Log Level</td>
                      <td>  <Select id="logLevel" v-model="node.val.logLevel">
                          <Option  v-for="(v,index) in ['Trance','Debug','Info','Warn','Error','Final']" :key="'logLevel'+index"
                                   :value="index+1">{{v}}
                          </Option>
                      </Select></td></tr>
              </table>
          </div>

      </div>

      <div class="itemRow">
          <div  class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">Timeout</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                  </div>
              </div>

              <table class="SMethodTable">
                  <tr><td>Timeout</td>
                      <td><Input id="timeout" v-model="node.val.timeout" placeholder="Timeout"/></td></tr>
                  <tr><td>Retry Count</td>
                      <td><Input id="retryCnt" v-model="node.val.retryCnt" placeholder="Retry Count"/></td></tr>
                  <tr><td>Retry Interval</td>
                      <td><Input id="retryInterval" v-model="node.val.retryInterval" placeholder="retryInterval"/></td></tr>
              </table>
          </div>

          <div class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">Statis Timer</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                  </div>
              </div>
              <table class="SMethodTable">
                  <tr><td>Time Unit</td>
                      <td><Select id="baseTimeUnit" v-model="node.val.baseTimeUnit">
                          <Option value="D">天</Option>
                          <Option value="H">时</Option>
                          <Option value="M">分</Option>
                          <Option value="S">秒</Option>
                          <Option value="MS">毫秒</Option>
                          <Option value="MC">微秒</Option>
                          <Option value="N">纳秒</Option>
                      </Select></td></tr>
                  <tr><td>Time Window</td>
                      <td><Input id="timeWindow" v-model="node.val.timeWindow" placeholder="Time"/></td></tr>
                  <tr><td>Slot Interval</td>
                      <td><Input id="slotSize" v-model="node.val.slotIndterval" placeholder="slot Size"/></td></tr>
                  <tr><td>Check Interval</td>
                      <td><Input id="checkInterval" v-model="node.val.checkInterval" placeholder="checkInterval"/></td></tr>
              </table>
          </div>

          <div  class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">Break Rule</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                  </div>
              </div>
              <table class="SMethodTable">
                  <tr><td>Is Breaking</td>
                      <td><Checkbox v-model="node.val.breaking"></Checkbox></td></tr>
                  <tr><td>Enable</td>
                      <td><Checkbox v-model="node.val.breakingRule.enable"></Checkbox></td></tr>
                  <tr><td>Break Time Interval</td>
                      <td><Input id="breakTimeInterval" v-model="node.val.breakingRule.breakTimeInterval"
                                 placeholder=""/></td></tr>
                  <tr><td>Break Percent</td>
                      <td><Input id="breakPercent" v-model="node.val.breakingRule.percent" placeholder=""/></td></tr>
                  <tr><td>Break Check Interval</td>
                      <td><Input id="bcheckInterval" v-model="node.val.breakingRule.checkInterval" placeholder=""/></td></tr>
                  <tr><td>Fail Response</td> <td> <Input id="failResponse" v-model="node.val.failResponse"/></td></tr>
              </table>
          </div>

      </div>

      <div class="itemRow">
          <div  class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">{{'Security'|i18n}}</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                  </div>
              </div>
              <table class="SMethodTable">
                  <tr><td>{{"NeedLogin"|i18n}}</td>
                      <td><Checkbox v-model="node.val.needLogin"></Checkbox></td></tr>
                  <tr><td>{{"InvokePer2Account"|i18n}}</td>
                      <td><Checkbox v-model="node.val.perType"></Checkbox></td></tr>
                  <tr><td>{{"DownSsl"|i18n}}</td>
                      <td><Checkbox v-model="node.val.isDownSsl"></Checkbox></td></tr>
                  <tr><td>{{"UpSsl"|i18n}}</td>
                      <td><Checkbox v-model="node.val.isUpSsl"></Checkbox></td></tr>

                  <tr><td>{{"EncryptType"|i18n}}</td>
                      <td><Select id="encType" v-model="node.val.encType">
                          <Option value="0">AES</Option>
                          <Option value="1">RSA</Option>
                      </Select></td></tr>

                  <tr><td>{{"MaxPacketSize"|i18n}}</td>
                      <td><Input id="maxPacketSize" v-model="node.val.maxPacketSize" placeholder=""/></td></tr>

                  <tr><td>{{"FeeType"|i18n}}</td>
                      <td><Select id="FeeType" v-model="node.val.feeType">
                      <Option value="0">{{"Free"|i18n}}</Option>
                      <Option value="1">{{"Client"|i18n}}</Option>
                      <Option value="2">{{"Private"|i18n}}</Option>
                  </Select></td></tr>

                  <tr><td>{{"authClients"|i18n}}</td>
                      <td><span v-for="val in node.val.authClients" :key="val">{{val}}</span></td></tr>

              </table>
          </div>

          <div class="ItemCard">
              <div class="ItemCardHeader">
                  <h3 class="ItemCardTitle">Testing</h3>
                  <div class="ItemCardBtnBar">
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="save()" href="javascript:void(0);">
                          Save
                      </a>
                      <a class="ItemCardBtn" v-show="isLogin" slot="extra" @click="doTesting(node.val)" href="javascript:void(0);">
                          {{timerId == -1? 'Start':'Stop'}}
                      </a>
                      <a  class="ItemCardBtn"  v-show="isLogin" slot="extra" @click="testingResult=''" href="javascript:void(0);">
                          <Icon type="ios-loop-strong"></Icon>
                          Clear
                      </a>
                  </div>
              </div>
              <table class="SMethodTable">
                  <tr><td>Testing Args</td>
                      <td><Input id="testingArgs"  class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}"
                                                       type="textarea" v-model="node.val.testingArgs"/></td></tr>
                  <tr><td>Testing Result</td>
                      <td><Input id="testingResult"  class='textarea' :rows="5" :autosize="{maxRows:5,minRows: 5}"
                                                         type="textarea" v-model="testingResult"/></td></tr>
                  <tr><td>Testing Num</td>
                      <td><Input id="InvokeNum" v-model="invokeNum"/></td></tr>
                  <tr><td>Interval with Milliseconds</td>
                      <td><Input id="InvokeInterval"  v-model="invokeInterval"/></td></tr>
              </table>
          </div>
      </div>

  </div>
</template>

<script>

    import TreeNode from  "./JServiceList.vue"

    import srv from "@/rpcservice/srv"
    
    const cid = 'JMethodItem';
export default {
    name: cid,
    components: {
    },
    props: {
        meth : TreeNode
    },

    mounted() {
        this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
        let self = this;

        this.isLogin = this.$jr.auth.isLogin();

        this.$jr.auth.addActListener(cid,()=>{
            self.isLogin = this.$jr.auth.isLogin();
        });

        let ec = function() {
            this.$jr.auth.removeActListener(cid);
            this.$off('editorClosed',ec);
        }

        this.$bus.$on('editorClosed',ec);

    },

    methods: {
        save() {
            let self = this;
            srv.updateMethod(this.node.val)
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
            this.$jr.rpc.callRpcWithParams(method.key.usk.serviceName, method.key.usk.namespace,
                method.key.usk.version, method.key.method, args)
                .then(rst=>{
                    //rst = window.jm.utils.parseJson(rst);
                    let msg = '';
                    if(typeof rst != 'undefined') {
                        if(typeof rst == 'object') {
                            if(rst.code != 0 ) {
                                msg = "code="+rst.code+",msg= "+rst.msg;
                            }else {
                                msg  =  JSON.stringify(rst.data);
                            }
                        } else {
                            msg  = rst;
                        }
                    }else {
                        msg  = 'NULL';
                    }
                    self.testingResult  +=  msg+'\n';
                }).catch(err=>{
                    if(err) {
                        self.testingResult  += (err+"\n");
                    }else {
                        self.testingResult  += "error\n";
                    }
            });
        }
    },

    data(){
        this.meth.val.encType=this.meth.val.encType+"";
        this.meth.val.feeType=this.meth.val.feeType+"";
        this.meth.val.limitType = this.meth.val.limitType+"";

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

    .ItemCard {
        display:inline-block;width:30%;text-align: center;
        margin: 0px 8px;
    }

    .itemRow{
        position:relative ;margin: 10px 0px;
    }

    .ItemCardHeader{
        background-color: lightgray; border-radius: 3px;position:relative;
    }

    .ItemCardTitle{
        display:inline-block;
    }

    .ItemCardBtnBar{
        display:inline-block; position: absolute; right: 0px;top: 0px;
    }

    .ItemCardBtn{
        padding-right: 5px;
    }

    .SMethodTable {
        width: 100%;
    }
</style>
