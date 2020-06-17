<template>
    <div class="JMonitorTypeServiceMethod">
        <a @click="refresh()">REFRESH</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <a  v-if="adminPer"  @click="update()">UPDATE</a>
        <br/>
        <p>{{id}}</p>
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>GROUP</td> <td>LABEL</td><td>FIELD NAME</td><td>TYPE CODE</td> <td>DESC</td><td>OPERATION</td></tr></thead>
            <tr v-for="a in typeList" :key="a.id">
                <td>{{ a.group }}</td><td>{{a.label}}</td><td>{{a.fieldName}}</td>
                <td>{{ a.type }}&nbsp;/&nbsp;0X{{ a.type.toString(16).toUpperCase() }}</td>
                <td>{{ a.desc }}</td>
                <td>
                    <Checkbox v-if="adminPer" v-model="a.check" @change.native="checkChange(a)"></Checkbox>
                </td>
            </tr>
        </table>

    </div>
</template>

<script>

    const cid  = 'JMonitorTypeServiceMethod';
    export default {
        name: cid,
        data () {
            return {
                id: this.getCurKey(),
                types:[],
                adds:[],
                dels:[],
                errMsg:'',
                typeList : [],
                adminPer : false,
                addConfigDialog:false,
            }
        },

        props:{
            item:{
                type:Object,
            },
        },

        methods: {

            refresh(){
                let self = this;
                this.adminPer = window.jm.mng.comm.adminPer;
                window.jm.mng.moType.getAllConfigsByGroup(['deflt']).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }

                    let tl = resp.data;
                    if(!tl || tl.length == 0 ) {
                        self.$Message.success("No data to show");
                        self.typeList =[];
                        return;
                    }

                    window.jm.mng.moType.getConfigByServiceMethodKey(self.id).then((resp)=>{
                        if(resp.code != 0) {
                            self.$Message.success(resp.msg);
                            return;
                        }
                        self.types = resp.data;
                        self.typeList =[];
                        for(let i = 0; i < tl.length; i++) {
                            let e = tl[i];
                            e.check = false;

                            for(let j = 0; j < self.types.length; j++) {
                                if(self.types[j] == e.type) {
                                    e.check = true;
                                    break;
                                }
                            }
                            self.typeList.push(e);
                        }
                    });

                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            checkChange(cfg) {
                if(cfg.check) {
                    for(let i = 0; i < this.dels.length; i++) {
                        if(cfg.type == this.dels[i]) {
                            this.dels.splice(i,1);
                        }
                    }
                    for(let i = 0; i < this.adds.length; i++) {
                        if(cfg.type == this.adds[i]) {
                            return;
                        }
                    }
                    this.adds.push(cfg.type);
                } else {
                    for(let i = 0; i < this.adds.length; i++) {
                        if(cfg.type == this.adds[i]) {
                            this.adds.splice(i,1);
                        }
                    }
                    for(let i = 0; i < this.dels.length; i++) {
                        if(cfg.type == this.dels[i]) {
                            return;
                        }
                    }
                    this.dels.push(cfg.type);
                }
            },

            update() {
                if(this.dels.length == 0 && this.adds.length == 0) {
                    this.$Message.warning("No data change!");
                    return;
                }
                let self = this;
                window.jm.mng.moType.updateServiceMethodMonitorTypes(self.id,this.adds,this.dels).then((resp)=>{
                    if(resp.code == 0) {
                        if(this.adds.length > 0) {
                            this.adds.splice(0,this.adds.length);
                        }
                        if(this.dels.length > 0) {
                            this.dels.splice(0,this.dels.length);
                        }
                        self.$Message.success("Success");
                    } else {
                        self.$Message.error(resp.msg);
                    }
                    self.refresh();
                });
            },

            getCurKey() {

                let key = null;
                if(this.item.type == 'ins') {
                    let node = this.item.val.key;
                    key = node.instanceName
                }else if(this.item.type == 'method') {
                    key = this.item.mkey;
                } else {
                    let node = this.item.val.key;
                    key = node.serviceName + "##" + node.namespace + "##" + node.version;
                }
                return key;
            },

        },

        mounted () {
            //let self = this;
            window.jm.mng.act.addListener(this.item.id,this.refresh);
            this.refresh();
        },

        beforeDestroy() {
            window.jm.mng.act.removeListener(this.item.id);
        },

    }
</script>

<style>
    .JMonitorTypeServiceMethod{

    }



</style>