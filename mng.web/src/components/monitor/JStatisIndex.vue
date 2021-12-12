<template>
    <tr>
        <td>
            <Select :disabled="status.edit" id="siTypes" v-model="si.type">
                <Option v-for="(k,v) in statisIndex" :key="k" :label="k" :value="v" ></Option>
            </Select>
        </td>
        <td><Input  :disabled="status.edit"  v-model="si.desc"/></td>
        <td><Input  :disabled="status.edit"  v-model="si.vk"/></td>
        <td>
            <Input :readonly="true" v-model="nums">
                <Button :disabled="status.edit" slot="append" icon="ios-search" @click="selectTypes(si.nums)"></Button>
        </Input>
        </td>
        <td>
            <Input  :readonly="true"   v-model="dens">
                <Button :disabled="!(!status.edit && si.type==2 || !status.edit && si.type==5)"  slot="append"
                        icon="ios-search" @click="selectTypes(si.dens)"/>
            </Input>
        </td>
        <td><a v-if="status.edit && !readonly" href="javascript:void(0)"  @click="editStatisIndex()">{{'Edit' | i18n }}</a>&nbsp;&nbsp;&nbsp;
            <a v-if="status.save" href="javascript:void(0)"  @click="okStatisIndex()">{{'Ok' | i18n }}</a>&nbsp;&nbsp;&nbsp;
            <a v-if="status.cancel" href="javascript:void(0)"  @click="cancelStatisIndex()">{{'Cancel' | i18n }}</a>&nbsp;&nbsp;&nbsp;
            <a v-if="!readonly" href="javascript:void(0)" @click="delStatisIndex()">{{'Delete' | i18n }}</a>&nbsp;&nbsp;&nbsp;
        </td>

        <Modal v-model="typesSelectDialog" :loading="true" ref="typesSelectDialog" width="90%"
               @on-ok="typeSelectOk()" @on-cancel="typeSelectCancel()">
            <JMonitorTypeSelector :curSelect.sync="editSelectTypes" @select="select($event)"
                                  @unselect="unselect($event)"></JMonitorTypeSelector>
        </Modal>
    </tr>

</template>

<script>

    import JMonitorTypeSelector from './JMonitorTypeSelector.vue'


    export default {
        name: 'JStatisIndex',
        props: ['si','readonly'],

        components: {
            JMonitorTypeSelector
        },

        data() {
            return {
                status : {edit:true,save:false,cancel:false},
                statisIndex:{1:'Total',2:'TotalPercent',3:'Qps',4:'Cur',5:'CurPercent'},

                editSelectTypes:[],
                typesSelectDialog:false,
                nums : '',
                dens : '',
            }
        },

        mounted() {
            if(this.si.nums && this.si.nums.length > 0) {
                this.nums = this.si.nums.join(',');
            }
            if(this.si.dens && this.si.dens.length >0) {
                this.dens = this.si.dens.join(',');
            }
            //this.si.name0 = this.si.name;
            this.si.type += '';
        },

        methods: {

            selectTypes(arrTypes) {
                this.editSelectTypes = arrTypes;
                this.typesSelectDialog = true;
            },

            typeSelectOk(){
                if(this.si.nums && this.si.nums.length > 0) {
                    this.nums = this.si.nums.join(',');
                }
                if(this.si.dens && this.si.dens.length >0) {
                    this.dens = this.si.dens.join(',');
                }

                this.editSelectTypes = [];
                this.typesSelectDialog = false;
            },

            typeSelectCancel(){
                this.editSelectTypes = [];
                this.typesSelectDialog = false;
            },

            select(evt) {
                for(let i = 0; i < this.editSelectTypes.length ; i++) {
                    if(this.editSelectTypes[i] == evt.type) {
                        return;
                    }
                }
                this.editSelectTypes.push(evt.type);
                //window.console.log(evt);
            },

            unselect(evt) {
                let idx = -1;
                for(let i = 0; i < this.editSelectTypes.length ; i++) {
                    if(this.editSelectTypes[i] == evt.type) {
                       idx = i;
                       break;
                    }
                }
                if(idx >=0) {
                    this.editSelectTypes.splice(idx,1);
                }
                //window.console.log(evt);
            },

            editStatisIndex(/*si*/) {
                this.status.edit=false;
                this.status.save=true;
                this.status.cancel=true;
            },

            cancelStatisIndex(){
                this.status.edit=true;
                this.status.save=false;
                this.status.cancel=false;
            },

            delStatisIndex() {
                this.$bus.$emit("delete");
            },

            okStatisIndex() {
                //this.si.name = this.si.name0;
                this.cancelStatisIndex();
                //this.$bus.$emit("save");
            },

        },

    }
</script>

<style>
    .JStatisIndex{

    }
</style>