<template>
    <div class="JLinkDetailView">
        <div class="treePanel">
            <Tree v-if="curModel" :data="[curModel]" ref="servicesTree"  @on-select-change="nodeSelect($event)"></Tree>
        </div>
        <div class="detailPanel">
            <div>
                <div v-if="curMi && curMi.item" >
                    <h5>COMSUMER</h5>
                    <table class="configItemTalbe" width="99%">
                        <thead><tr><td stype="width:300px;">TAG</td><td>TYPE</td><td>LEVEL</td><td>TIME</td><td>DESC</td><td>VAL</td><td>NUN</td></tr></thead>
                        <tr v-for="c in curMi.item.items" :key="c.type +'_'+ c.time">
                            <td>{{c.tag}}</td><td>{{c.typeLabel}}</td><td>{{c.levelLabel}}</td><td>{{c.time | formatDate}}</td>
                            <td>{{c.desc}}</td><td>{{c.val}}</td><td>{{c.num}}</td>
                        </tr>
                    </table>
                </div>

                <div v-if="curMi && curMi.providerItems">
                    <div v-for="mi in curMi.providerItems" :key="mi.reqId">
                        <h5>PROVIDER</h5>
                        <table class="configItemTalbe" width="99%">
                            <thead><tr><td stype="width:300px;">TAG</td><td>TYPE</td><td>LEVEL</td><td>TIME</td><td>DESC</td><td>VAL</td><td>NUN</td></tr></thead>
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
        width:20%;
        height: 100%;
    }

    .detailPanel {
        float: right;
        width:79%;
        height: auto;
    }

</style>
