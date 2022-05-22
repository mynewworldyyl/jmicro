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
		
		<Modal v-model="showAddModule" :loading="true" width="360" 
			@on-cancel="showAddModule=false" @on-ok="doAddModule()" ref="addMobule">
		    <table>
				<tr><td>{{'Module'|i18n}}</td><td><input readonly="true" type="input" v-model="mod.module"/></td></tr>
				<tr v-if="isAdmin"><td>{{'ClientId'|i18n}}</td><td><input  type="input"   v-model="mod.clientId"/></td></tr>
				<tr><td>{{'Key'|i18n}}</td><td><input  type="input"   v-model="mod.key"/></td></tr>
		        <tr><td>{{'Value'|i18n}}</td><td><input  type="input"  v-model="mod.val"/></td></tr>
				<tr><td>{{'Type'|i18n}}</td><td>
					<Select v-model="mod.type">
						<Option v-for="(val,key) in type2Desc" :value="key+''" :key="key">{{val | i18n}}</Option>
					</Select>
				</td></tr>
		    </table>
		</Modal>

    </div>
</template>

<script>
    import profile from "@/rpcservice/profile"
    import {Constants} from "@/rpc/message"
	
    const cid = 'userProfile';

    export default {
        name: 'JUserProfileEditor',
        data () {
			let au = this.$jr.auth
			console.log(this.item)
            return {
				isAdmin:au.isAdmin(),
				showAddModule:false,
				type2Desc:Constants.PREFIX_TYPE_2DESC,
                isLogin:false,
                modifyDialog:false,
                modifyProfile:null,
                msg:'',
				mod:{clientId:au.actInfo.clientId,module:this.item.id, type: Constants.PREFIX_TYPE_STRINGG+''}
            }
        },

        props:{
            item : {type: Object, required: true},
        },

        watch:{
            item() {
                this.refresh();
            }
        },

        methods: {

            refresh(){
                let self = this;
                this.isLogin = this.$jr.auth.isLogin();
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
			
			async doAddModule(){
				
				if(!this.mod.key) {
					this.$Message.error("名称不能为空")
					return
				}
				
				if(!this.mod.val) {
					this.$Message.error("值不能为空")
					return
				}
				
				if(!this.mod.type) {
					this.$Message.error("值类型不能为空")
					return
				}
				
				if(!this.$jr.auth.isAdmin() || !this.mod.clientId) {
					this.mod.clientId = this.$jr.auth.actInfo.clientId
				}
				
				this.mod.module = this.item.id
				
				let rst = await profile.addKv(this.mod.clientId, this.mod.module,
					this.mod.key, this.mod.val, this.mod.type)
				if(rst.code != 0) {
					 this.$Message.error(rst.msg || "失败")
				}else{
					 this.refresh()
					 this.showAddModule=false
					 this.$Message.info("成功")
				}
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
						this.refresh()
						self.$Message.success("Update successfully")
						self.modifyDialog = false
					}else {
						self.msg = errmsg
					}
				});
            }

        },

        mounted () {
            this.$el.style.minHeight=(document.body.clientHeight-67)+'px';
            this.$jr.auth.addActListener(this.refresh);
            let self = this;
            this.$bus.$emit("editorOpen",
                {"editorId":cid,
                    "menus":[{name:"Add",label:"Add",icon:"ios-cog",call: ()=>{ self.showAddModule = true}},
                        {name:"REFRESH",label:"Refresh",icon:"ios-cog",call:self.refresh}]
                });

            let ec = function() {
                this.$jr.auth.removeActListener(cid);
                this.$off('editorClosed',ec);
            }
            this.$bus.$on('editorClosed',ec);
            this.refresh();
        },
    }

</script>

<style>
    .JUserProfileEditor{
    }

</style>