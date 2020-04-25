<template>
    <div class="accountStatuBar">
        <span class="loginBtn"><a href="javascript:void(0);" @click="doLoginOrLogout()">
            {{ actInfo && actInfo.success  ? 'LOGOUT':'LOGIN'}}</a></span>
        <span class="accountBar">{{actInfo.actName}}</span>

        <Modal v-model="loginDialog" :loading="true" width="360" @on-ok="doLogin()" ref="loginDialog">
            <table>
                <tr><td>actName</td><td><input type="input"  v-model="actName"/></td></tr>
                <tr><td>Password</td><td><input type="password"  v-model="pwd"/></td></tr>
                <tr><td>confirm Password</td><td><input type="password"  v-model="cfPwd"/></td></tr>
                <tr><td colspan="2">{{msg}}</td></tr>
            </table>
        </Modal>

    </div>
</template>

<script>

export default {
    name: 'JAccount',

    components: {

    },

    data() {
        return {
            loginDialog : false,
            actName : '',
            pwd : '',
            cfPwd : '',
            actInfo : {actName:'',success:false},
            msg:''
        };
    },

    methods: {
        doLoginOrLogout(){
            if(this.actInfo.success) {
                this.doLogout();
            } else {
                this.loginDialog = true;
            }
        },

        doLogin(){
            let self = this;
            this.$refs.loginDialog.buttonLoading = false;

            if(!this.pwd || !this.cfPwd || this.pwd != this.cfPwd ) {
                self.msg = 'Invalid pwd';
                return;
            }
            self.msg = '';
            window.jm.mng.act.login(this.actName,this.pwd,(actInfo,err)=>{
                if(!err && actInfo.success) {
                    self.actInfo = actInfo;
                    self.msg = '';
                    this.loginDialog = false;
                    window.jm.vue.$emit('userLogin',actInfo);
                }else {
                    self.msg = err.msg | 'Login fail';
                }
            });
        },

        doLogout(){
            let self = this;
            window.jm.mng.act.loginout((sus,err)=>{
                if(!err && sus) {
                    self.actInfo = {actName:'',success:false};
                    self.msg = '';
                    window.jm.vue.$emit('userLogout');
                } else {
                    self.msg = 'Login fail';
                }
            });
        }

    },


}
</script>

<style scoped>
    .accountStatuBar{
        position: absolute;
        right: 0px;
        top: 0px;
        z-index: 1000;
        height: 60px;
        line-height: 60px;
    }

    .loginBtn{
        display: inline-block;
        padding:6px;
        margin-right: 5px;
    }

    .accountBar{
        display: inline-block;
        padding:6px;
        margin-right: 5px;
    }

</style>
