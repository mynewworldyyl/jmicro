<template>
    <div class="JMonitorTypeKeyList">
       <!-- <a @click="refresh()">REFRESH</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <a  v-if="adminPer"   @click="selectAll(true)">SELECTALL</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <a  v-if="adminPer"   @click="selectAll(false)">UNSELECTALL</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        <a  v-if="adminPer"  @click="update()">UPDATE</a>
        <br/>-->
        <p>{{item.id}}</p>
        <table class="configItemTalbe" width="99%">
            <thead><tr><td>GROUP</td> <td>LABEL</td><td>FIELD NAME</td><td>TYPE CODE</td> <td>DESC</td><td>OPERATION</td></tr></thead>
            <tr v-for="a in typeList" :key="a.id">
                <td>{{ a.group }}</td><td>{{a.label}}</td><td>{{a.fieldName}}</td>
                <td>{{ a.type }}&nbsp;/&nbsp;0X{{ a.type.toString(16).toUpperCase() }}</td>
                <td>{{ a.desc }}</td>
                <td>
                    <Checkbox :disabled="!adminPer" v-model="a.check" @change.native="checkChange(a)"></Checkbox>
                </td>
            </tr>
        </table>

    </div>
</template>

<script>

    const cid  = 'JMonitorTypeKeyList';
    export default {
        name: cid,
        data () {
            return {
                types:[],
                adds:[],
                dels:[],
                errMsg:'',
                typeList : [],
                adminPer : false,
                addConfigDialog:false,
                cfg:{group:'',fieldName:'',label:'',desc:''}
            }
        },

        props:{
            item : {
                type:Object,
            },
        },

        methods: {

            refresh(){
                let self = this;
                this.adminPer = window.jm.rpc.isAdmin();
                window.jm.mng.moType.getAllConfigs().then((resp)=>{
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

                    window.jm.mng.moType.getTypesByNamed(self.item.id).then((resp)=>{
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

                window.jm.mng.moType.updateNamedTypes(this.item.id,this.adds,this.dels).then((resp)=>{
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

            selectAll(st) {
                if(this.typeList == null || this.typeList.length == 0) {
                    return;
                }

                for(let i = 0; i < this.typeList.length; i++) {
                    let v = this.typeList[i];
                    if(v.check != st) {
                        v.check = st;
                        this.checkChange(v)
                    }
                }
            },

        },

        mounted () {
            let self = this;

            window.jm.rpc.addActListener(this.item.id,()=>{
                self.refresh();
            });
            this.refresh();

            let menus = [{name:"Refresh",label:"refresh",icon:"ios-cog",call:self.refresh},
                { name:"SelectAll", label:"Select All", icon:"ios-cog",call : ()=>{self.selectAll(true);}, needAdmin:true },
                { name:"UnselectAll", label: "Unselect All", icon : "ios-cog",call : ()=>{self.selectAll(false);}, needAdmin:true },
                { name:"Update", label:"Update", icon:"ios-cog",call : ()=>{self.update();}, needAdmin:true }];
            window.jm.vue.$emit("editorOpen", {"editorId":this.item.id, "menus":menus});
        },

        beforeDestroy() {
            window.jm.rpc.removeActListener(this.item.id);
        },

    }
</script>

<style>
    .JMonitorTypeKeyList{

    }



</style>