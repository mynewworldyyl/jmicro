<template>
    <div class="JTypeConfig">
       <!-- <a @click="refresh()">REFRESH</a>
        <a v-if="adminPer" @click="add()">ADD</a>-->
        <table v-if="typeList && typeList.length > 0" class="configItemTalbe" width="99%">
        <thead><tr><td>GROUP</td> <td>LABEL</td><td>FIELD NAME</td><td>TYPE CODE</td> <td>DESC</td>
            <td v-if="isAdmin">OPERATION</td></tr></thead>
        <tr v-for="a in typeList" :key="a.id">
            <td>{{ a.group }}</td><td>{{a.label}}</td><td>{{a.fieldName}}</td>
            <td>{{ a.type }}&nbsp;/&nbsp;0X{{ a.type.toString(16).toUpperCase() }}</td>
            <td>{{ a.desc }}</td>
            <td v-if="isAdmin">&nbsp;
                <a v-if=" a.type > 0X0FFF" @click="deleteCfg(a.type)">DELETE</a>&nbsp;&nbsp;&nbsp;&nbsp;
                <a  @click="update(a)">UPDATE</a>
            </td>
        </tr>
    </table>

        <Modal v-model="addConfigDialog" :loading="true" ref="addConfigDialog" width="360" @on-ok="onAddOk()">
            <div>

                <div style="color:red">{{errMsg}}</div>

                <Label for="group">GROUP</Label>
                <Select :filterable="true"
                        :allowCreate="true" ref="setSelect"
                        @on-create="createGroup" id="group" :label-in-value="true" v-model="cfg.group">
                    <Option :value="g" v-for="g in groups" v-bind:key="g">{{g}}</Option>
                </Select>

               <!-- <Label for="group">GROUP</Label>
                <Input id="group"  v-model="cfg.group"/>-->
                <Label for="fieldName">FIELD NAME</Label>
                <Input id="fieldName" v-model="cfg.fieldName"/>

                <Label for="label">LABEL</Label>
                <Input id="label" v-model="cfg.label"/>

                <Label for="desc">DESC</Label>
                <Input id="desc"  class='textarea' :rows="5" :autosize="{maxRows:3,minRows: 3}"
                       type="textarea" v-model="cfg.desc"/>

                <Label for="type">TYPE</Label>
                <Input id="type" v-model="cfg.type" readonly/>

            </div>
        </Modal>

    </div>
</template>

<script>

    const cid = 'typeConfig';
    import moType from "@/rpcservice/moType"
    
    export default {
        name: 'JTypeConfig',
        data () {
            return {
                isAdmin:false,
                groups:[],
                errMsg:'',
                typeList : [],
                adminPer : false,
                addConfigDialog:false,
                cfg:{group:'',fieldName:'',label:'',desc:''}
            }
        },
        methods: {

            onAddOk() {
                if(!this.cfg.fieldName) {
                    this.errMsg = 'field name cannot be null';
                    return;
                }
                if(!this.isAdmin) {
                    this.errMsg = 'No permission';
                    return;
                }
                this.errMsg ='';
                let self = this;
                moType.add(this.cfg).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    self.addConfigDialog = false;
                    self.refresh();
                }).catch((err)=>{
                    window.$Message.error(err);
                });

            },

            refresh(){
                let self = this;
                this.isAdmin = this.$jr.rpcisAdmin();

                moType.getAllConfigs().then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }
                    let typeList = resp.data;
                    if(!typeList || typeList.length == 0 ) {
                        self.$Message.success("No data to show");
                        this.typeList =[];
                        return;
                    }

                    let gs = {};
                    this.typeList =[];
                    for(let i = 0; i < typeList.length; i++) {
                        let e = typeList[i];
                        gs[e.group]='';
                        this.typeList.push(e);
                    }

                    self.groups = [];
                    for(let g in gs) {
                        self.groups.push(g);
                    }

                }).catch((err)=>{
                    window.console.log(err);
                });
            },

            add() {
                if(!this.isAdmin) {
                    this.errMsg = 'No permission';
                    return;
                }
                this.addConfigDialog = true;
            },

            deleteCfg(cfg) {
                if(!this.isAdmin) {
                    this.errMsg = 'No permission';
                    return;
                }
                let self = this;
                moType.delete(cfg).then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }else {
                        self.refresh();
                    }
                }).catch((err)=>{
                    self.$Message.error(err);
                });
            },

            update(mc) {
                if(!this.isAdmin) {
                    this.errMsg = 'No permission';
                    return;
                }
                console.log(mc);
            },

            createGroup(val) {
                if(!this.isAdmin) {
                    this.errMsg = 'No permission';
                    return;
                }
                if(val && val.trim() != '') {
                    val = val.trim();
                    for(let i =0; i < this.groups.length; i++) {
                        if(val == this.groups[i]) {
                            return;
                        }
                    }
                    this.groups.push(val);
                }
                /*let query = this.$refs['setSelect'].$data.query;
                if (query) {
                    this.$refs['setSelect'].$data.query = ''
                }*/
            }

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            let self = this;
            this.$jr.auth.addActListener(cid,self.refresh);
            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }
            this.$bus.$on('editorClosed',ec);
            self.refresh();

            let menus = [{name:"REFRESH",label:"REFRESH",icon:"ios-cog",call:self.refresh},
                { name:"ADD", label:"ADD", icon:"ios-cog",call : self.addDeploy, needAdmin:true }];
            this.$bus.$emit("editorOpen", {"editorId":cid, "menus":menus});
        },

        beforeDestroy() {
            this.$jr.auth.removeActListener(cid);
        },
    }
</script>

<style>
    .JTypeConfig{

    }



</style>