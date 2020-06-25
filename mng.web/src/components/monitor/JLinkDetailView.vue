<template>
    <div class="JLinkDetailView">
        <div class="treePanel">
            <Tree v-if="curModel" :data="[curModel]" ref="servicesTree"  @on-select-change="nodeSelect($event)"></Tree>
        </div>
        <div class="detailPanel">
            <div class="overViewCls" v-if="curMi && curMi.item" >
                <h3 :class="{'failCls' : !curMi.item.resp.success}">OVERVIEW</h3>
                <Row type="flex">
                    <i-col span="12" order="1">SERVICE | {{curMi.item.req.serviceName}}</i-col>
                    <i-col span="12" order="2">  IMPL | {{curMi.item.implCls}}</i-col>
                </Row>
                <Row type="flex">
                    <i-col span="6" order="1">NAMESPACE | {{curMi.item.req.namespace}}</i-col>
                    <i-col span="6" order="2">VERSION | {{curMi.item.req.version}}</i-col>
                    <i-col span="6" order="3">METHOD | {{curMi.item.req.method}}</i-col>
                </Row>
                <Row type="flex">
                    <i-col span="12" order="1">PARAMS TYPE | {{curMi.item.sm.key.paramsStr}}</i-col>
                    <i-col span="12" order="2">  ARGS | {{ curMi.item.req.args }}</i-col>
                </Row>
                <Row type="flex">
                    <i-col span="6" order="1">LINK ID | {{curMi.item.linkId}}</i-col>
                    <i-col span="6" order="2">REQ ID | {{curMi.item.reqId}}</i-col>
                    <i-col span="6" order="3">PARENT ID | {{curMi.item.reqParentId}}</i-col>
                    <i-col span="6" order="4">ACCOUNT | {{curMi.item.act}}</i-col>
                    <i-col span="6" order="5">LO HOST | {{curMi.item.localHost}}</i-col>
                    <i-col span="6" order="6">RO HOST | {{curMi.item.remoteHost}}</i-col>
                    <i-col span="6" order="7">RO PORT | {{curMi.item.remotePort}}</i-col>
                    <i-col span="6" order="8">INSTANCE | {{curMi.item.instanceName}}</i-col>
                    <i-col span="6" order="9">PROVIDER | {{curMi.item.provider}}</i-col>
                </Row>

                <Row>
                    <i-col span="12" order="1">COST TIME | {{curMi.item.costTime+'MS'}}</i-col>
                    <i-col span="12" order="1">CREATE TIME | {{curMi.item.createTime | formatDate}}</i-col>
                    <i-col span="12" order="2">INPUT TIME | {{curMi.item.inputTime | formatDate}}</i-col>
                </Row>
                <Row>
                    <i-col span="6" order="1">STATUS | {{curMi.item.resp.success?"success":"fail"}}</i-col>
                    <i-col span="20" order="2">RESULT | {{curMi.item.resp.result}}</i-col>
                </Row>

            </div>

            <div>
                <div v-if="curMi && curMi.item" class="tableCls">
                    <table class="configItemTalbe" width="99%">
                        <caption>COMSUMER</caption>
                        <thead><tr><td class="tagCls">TAG</td><td>TYPE</td><td class="levelCls">LEVEL</td><td  class="timeCls">TIME</td><td>DESC</td><td>VAL</td><td>NUN</td></tr></thead>
                        <tr v-for="c in curMi.item.items" :key="c.type +'_'+ c.time">
                            <td>{{c.tag}}</td><td>{{c.typeLabel}}</td><td>{{c.levelLabel}}</td><td>{{c.time | formatDate}}</td>
                            <td>{{c.desc}}</td><td>{{c.val}}</td><td>{{c.num}}</td>
                        </tr>
                    </table>
                </div>

                <div v-if="curMi && curMi.providerItems" style="margin-top:10px">
                    <h3>PROVIDERS</h3>
                    <div v-for="mi in curMi.providerItems" :key="mi.reqId" class="tableCls">
                        <div class="overViewCls">
                            <h4 :class="{'failCls': !mi.resp.success}">OVERVIEW</h4>
                            <Row type="flex">
                                <i-col span="12" order="1">ARGS | {{ mi.req.args }}</i-col>
                                <i-col span="6" order="2">INSTANCE | {{mi.instanceName}}</i-col>
                                <i-col span="6" order="3">PROVIDER | {{mi.provider}}</i-col>
                            </Row>
                            <Row type="flex">
                                <i-col span="6" order="1">ACCOUNT | {{mi.act}}</i-col>
                                <i-col span="6" order="2">LO HOST | {{mi.localHost}}</i-col>
                                <i-col span="6" order="3">RO HOST | {{mi.remoteHost}}</i-col>
                                <i-col span="6" order="4">RO PORT | {{mi.remotePort}}</i-col>
                                <i-col span="12" order="5">COST TIME | {{mi.costTime+'MS'}}</i-col>
                                <i-col span="12" order="6">CREATE TIME | {{mi.createTime | formatDate}}</i-col>
                            </Row>
                            <Row>
                                <i-col span="6" order="1">STATUS | {{mi.resp.success?"success":"fail"}}</i-col>
                                <i-col span="20" order="2">RESULT | {{mi.resp.result}}</i-col>
                            </Row>

                        </div>
                        <table class="configItemTalbe" width="99%">
                            <thead><tr><td class="tagCls">TAG</td><td>TYPE</td><td class="levelCls">LEVEL</td><td class="timeCls">TIME</td><td>DESC</td><td>VAL</td><td>NUN</td></tr></thead>
                            <tr v-for="c in mi.items" :key="c.type +'_'+ c.time">
                                <td>{{c.tag}}</td><td>{{c.typeLabel}}</td><td>{{c.levelLabel}}</td><td>{{c.time | formatDate}}</td>
                                <td>{{c.desc}}</td><td>{{c.val}}</td><td>{{c.num}}</td>
                            </tr>
                        </table>
                    </div>
                </div>


            </div>
        </div>
    </div>
</template>

<script>

    export default {
        name: 'JLinkDetailView',
        components: {

        },

        data () {

            return {
                cacheModels:{},
                curModel:null,
                curMi:null,
                ds:{},

            };
        },

        props:{
            linkId: {
                type: Number
            },
        },

        watch:{
            'linkId': {
                handler() {
                    this.loadLinkData()
                },
            },
        },

        mounted : function() {
            console.log(this.linkId);
            let self = this;
            window.jm.mng.comm.getDicts(['logKey2Val','mtKey2Val']).then((dicts)=>{
                if(dicts) {
                    for(let k in dicts) {
                        let k2v = dicts[k];
                        let v2k = {};
                        self.ds[k] = v2k;
                        for(let kk in k2v) {
                            v2k[k2v[kk]] = kk;
                        }
                    }
                }
            }).catch((err)=>{
                throw err;
            });
        },

        filters: {
            formatDate: function(time) {
                // 后期自己格式化
                return new Date(time).format("yyyy/MM/dd hh:mm:ss S") //Utility.formatDate(date, 'yyyy/MM/dd')
            }
        },

        methods: {

            nodeSelect(evt) {
                if(evt && evt.length > 0) {
                    this.curMi = evt[0];
                }
            },

            loadLinkData(){
                if(this.curModel != null && this.linkId == this.curModel.item.linkId) {
                    return;
                }
                if(this.cacheModels[this.linkId]) {
                    this.curModel = this.cacheModels[this.linkId];
                    this.curMi = this.curModel;
                    return;
                }
                let self = this;
                window.jm.mng.logSrv.getByLinkId(this.linkId).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    self.cacheModels[self.linkId] = resp.data;
                    self.curModel = resp.data;
                    self.parseTree(self.curModel);
                    this.curMi = self.curModel;
                }).catch((err)=>{
                    window.console.log(err);
                });

            },

            parseTree(model) {
                if(!model) {
                    return;
                }

                model.title = model.item.reqId;
                model.expand=true;
                let its = model.item.items;
                if(its && its.length > 0) {
                    its.map(e => {
                        e.typeLabel = this.ds['mtKey2Val'][e.type];
                        e.levelLabel = this.ds['logKey2Val'][e.level];
                    });
                }

                its = model.providerItems;
                if(its && its.length > 0) {
                    its.map(mi => {
                        mi.items.map(e => {
                            e.typeLabel = this.ds['mtKey2Val'][e.type];
                            e.levelLabel = this.ds['logKey2Val'][e.level];
                        });
                    });
                }

                if(model.children && model.children.length > 0) {
                    model.children.map(e  => {
                        e.parent = model;
                        this.parseTree(e)
                    });
                }
            }
        }
    }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
    .JLinkDetailView{
        height:100%;
        overflow:hidden;
    }

    .treePanel {
        float: left;
        border:1px solid lightgray;
        width:16%;
        height: 100%;
    }

    .detailPanel {
        float: right;
        width:83%;
        height: 100%;
        overflow-y: auto;
    }

    .detailPanel td {
        text-align: center;
    }

    .levelCls {
        width:90px;
    }

    .tagCls {
        width:220px;
    }

    .timeCls {
        width:160px;
    }

    .tableCls {
        margin-top:15px;
    }

    .failCls {
        background-color: red;
    }

</style>
