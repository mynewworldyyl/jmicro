<template>
    <div class="JUserProfileEditor">
        <table v-if="item.val" class="configItemTalbe" width="99%">
            <thead><tr><td>{{'Key'|i18n}}</td><td>{{'Value'|i18n}}</td><td>{{'Type'|i18n}}</td>
                <td>{{'Operation'|i18n}}</td></tr></thead>
            <tr v-for="c in item.val" :key="c.id">
                <td>{{c.key}}</td><td>{{c.val}}</td><td>{{c.type}}</td>
                <td>
                    <a v-if="isLogin" @click="update(c)">{{'Modify'|i18n}}</a>
                </td>
            </tr>
        </table>
        <div v-if="!item.val">{{'NoData'|i18n}}</div>
        <div v-if="!isLogin">{{'NotLogin'|i18n}}</div>

       <Modal v-model="modifyDialog" :loading="true" width="360" @on-ok="doUpdate()" ref="modifyDialog">
            <table>
                <tr><td>{{'Key'|i18n}}</td><td><input v-if="modifyProfile" type="input" readonly="true"  v-model="modifyProfile.key"/></td></tr>
                <tr><td>{{'Value'|i18n}}</td><td><input v-if="modifyProfile" type="input"  v-model="modifyProfile.val"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

    </div>
</template>

<script>
    import rpc from "@/rpc/rpcbase"
    import profile from "@/rpcservice/profile"
    
    const cid = 'userProfile';

    export default {
        name: 'JUserProfileEditor',
        data () {
            return {
                isLogin:false,
                modifyDialog:false,
                modifyProfile:null,
                msg:'',
            }
        },

        props:{
            item : {type: Object,required: true},
        },

        watch:{
            item() {
                this.refresh();
            }
        },

        methods: {

            refresh(){
                let self = this;
                this.isLogin = rpc.isLogin();
                if(!self.isLogin) {
                    self.$Message.info("Not login");
                    self.item.val = [];
                    return;
                }

                profile.getModuleKvs(this.item.id)
                    .then((resp) => {
                        if(resp.code == 0) {
                            self.item.val = resp.data;
                            //this.$set(self.item, 'kvs', resp.data)
                        }else {
                            self.$Message.info(resp.msg);
                        }
                    }).catch((err) => {
                        self.$Message.info(err || "error");
                });
            },

            save() {

            },

            update(p) {
                this.modifyProfile = p;
                this.modifyDialog = true;
            },

            doUpdate() {
                this.$refs.modifyDialog.buttonLoading = false;

                let self = this;
                profile.updateKv(this.item.id,this.modifyProfile)
                    .then((resp,errmsg)=>{
                        if(resp.data) {
                            self.$Message.success("Update successfully")
                            self.modifyDialog = false;
                        }else {
                            self.msg = errmsg;
                        }
                    });
            }

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            rpc.addActListener(cid,this.refresh);
            let self = this;
            window.jm.vue.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"Save",label:"Save",icon:"ios-cog",call: ()=>{ self.save();}},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

            let ec = function() {
                rpc.removeActListener(cid);
                window.jm.vue.$off('editorClosed',ec);
            }
            window.jm.vue.$on('editorClosed',ec);
            this.refresh();
        },
    }

</script>

<style>
    .JUserProfileEditor{
    }

</style>