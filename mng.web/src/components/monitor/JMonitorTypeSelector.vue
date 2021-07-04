<template>
    <table class="configItemTalbe" width="99%">
        <thead><tr><td>GROUP</td> <td>LABEL</td><td>FIELD NAME</td><td>TYPE CODE</td> <td>DESC</td><td>OPERATION</td></tr></thead>
        <tr v-for="a in typeList" :key="a.id">
            <td>{{ a.group }}</td><td>{{a.label}}</td><td>{{a.fieldName}}</td>
            <td>{{ a.type }}&nbsp;/&nbsp;0X{{ a.type.toString(16).toUpperCase() }}</td>
            <td>{{ a.desc }}</td>
            <td>
                <Checkbox v-model="a.check" @change.native="checkChange(a)"></Checkbox>
            </td>
        </tr>
    </table>
</template>

<script>

    import moType from "@/rpcservice/moType"
    import rpc from "@/rpc/rpcbase"

    export default {
        name: 'JMonitorTypeSelector',
        props: ['curSelect'],
        data() {
            return {
                adminPer:true,
                typeList:[],
            }
        },

        watch:{
            'curSelect':function(val){
                this.updateSelect(val);
            },
        },

        mounted() {
            this.refresh();
        },

        methods: {

            checkChange(cfg) {
                if(cfg.check) {
                    this.$emit("select",cfg)
                } else {
                    this.$emit("unselect",cfg)
                }
            },

            updateSelect() {
                let tl =[];
                for(let i = 0; i < this.typeList.length; i++) {
                    let e = this.typeList[i];
                    e.check = false;

                    for(let j = 0; j < this.curSelect.length; j++) {
                        if(this.curSelect[j] == e.type) {
                            e.check = true;
                            break;
                        }
                    }
                    tl.push(e);
                }
                this.typeList = tl;
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

            refresh(){
                let self = this;
                moType.getAllConfigs().then((resp)=>{
                    if(resp.code != 0) {
                        self.$Message.success(resp.msg);
                        return;
                    }

                    self.typeList = resp.data;
                    self.updateSelect();

                }).catch((err)=>{
                    window.console.log(err);
                });
            },

        },

    }
</script>

<style>
    .JMonitorTypeSelector{

    }
</style>